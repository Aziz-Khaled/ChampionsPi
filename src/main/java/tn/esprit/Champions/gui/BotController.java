package tn.esprit.Champions.gui;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.util.Duration;
import tn.esprit.Champions.models.*;
import tn.esprit.Champions.services.*;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

public class BotController {

    @FXML private ListView<String> listLogs;
    @FXML private Label lblBotStatus;

    // Services
    private TradeService tradeService = new TradeService();
    private MarketApiService marketApi = new MarketApiService();
    private TransactionService transService = new TransactionService();
    private wallet_currencyService wcService = new wallet_currencyService();

    private Timeline botTimeline;

    // Constantes de configuration
    private final int USER_WALLET_ID = 3;
    private final int MARKET_WALLET_ID = 4;
    private final int USDT_ID = 1;

    @FXML
    public void initialize() {
        startAutomationEngine();
    }

    private void startAutomationEngine() {
        botTimeline = new Timeline(new KeyFrame(Duration.seconds(5), event -> {
            try {
                // Récupération des ordres en attente
                List<Trade> pendingTrades = tradeService.SelectAll().stream()
                        .filter(t -> t.getStatus() == Status.PENDING && t.getOrderMode() == OrderMode.LIMIT)
                        .toList();

                if (pendingTrades.isEmpty()) {
                    lblBotStatus.setText("Statut : En veille (Aucun ordre)");
                    return;
                }

                lblBotStatus.setText("Statut : Surveillance de " + pendingTrades.size() + " ordres...");

                for (Trade trade : pendingTrades) {
                    // Récupération du prix actuel (BTC par défaut ici)
                    double currentPrice = marketApi.fetchPrice("BTC");
                    boolean shouldExecute = false;

                    if (trade.getTradeType() == TradeType.BUY && currentPrice <= trade.getPrice()) {
                        shouldExecute = true;
                    } else if (trade.getTradeType() == TradeType.SELL && currentPrice >= trade.getPrice()) {
                        shouldExecute = true;
                    }

                    if (shouldExecute) {
                        executeBotOrder(trade, currentPrice);
                    }
                }
            } catch (SQLException e) {
                updateLogs("❌ Erreur BDD : " + e.getMessage());
            }
        }));

        botTimeline.setCycleCount(Animation.INDEFINITE);
        botTimeline.play();
    }

    private void executeBotOrder(Trade trade, double executionPrice) {
        try {
            double totalAmount = trade.getQuantity() * executionPrice;

            // 1. MISE À JOUR DU WALLET (via ta nouvelle méthode sécurisée)
            // Cette méthode lève une exception si le solde est insuffisant pour un achat
            wcService.updateBalanceAfterTrade(USER_WALLET_ID, USDT_ID, totalAmount, trade.getTradeType());

            // 2. Mise à jour du statut de l'ordre en BDD
            trade.setStatus(Status.COMPLETED);
            trade.setExecutedAt(LocalDateTime.now());
            tradeService.updateOne(trade);

            // 3. Création de la trace financière (Transaction)
            transaction t = new transaction();
            t.setIdWalletSource(trade.getTradeType() == TradeType.BUY ? USER_WALLET_ID : MARKET_WALLET_ID);
            t.setIdWalletDestination(trade.getTradeType() == TradeType.BUY ? MARKET_WALLET_ID : USER_WALLET_ID);
            t.setMontant(totalAmount);
            t.setType(trade.getTradeType() == TradeType.BUY ? typeTransaction.ACHAT : typeTransaction.VENTE);
            t.setCurrencyId(USDT_ID);
            t.setStatut(StatutTransaction.Completed);
            t.setDateTransaction(LocalDateTime.now());
            transService.insertOne(t);

            // 4. Feedback utilisateur
            updateLogs("✅ EXÉCUTÉ : " + trade.getTradeType() + " | Montant: " + String.format("%.2f", totalAmount) + " USDT");
            System.out.println("🤖 Bot : Ordre " + trade.getId() + " traité avec succès.");

        } catch (SQLException e) {
            updateLogs("⚠️ Échec exécution : " + e.getMessage());
            System.err.println("🤖 Bot Error : " + e.getMessage());
        }
    }

    /**
     * Petite méthode utilitaire pour mettre à jour la liste des logs
     * en s'assurant de rester sur le thread JavaFX principal.
     */
    private void updateLogs(String message) {
        Platform.runLater(() -> listLogs.getItems().add(0, message));
    }

    @FXML
    private void handleStop() {
        if (botTimeline != null) {
            botTimeline.stop();
            lblBotStatus.setText("Statut : Arrêté");
            updateLogs("🛑 Bot arrêté manuellement.");
        }
    }
}