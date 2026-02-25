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
    private final TradeService tradeService = new TradeService();
    private final MarketApiService marketApi = new MarketApiService();
    private final TransactionService transService = new TransactionService();
    private final wallet_currencyService wcService = new wallet_currencyService();
    private final AssetService assetService = new AssetService();

    private Timeline botTimeline;

    // Configuration des IDs (A vérifier selon ta DB)
    private final int USER_WALLET_ID = 3;
    private final int MARKET_WALLET_ID = 4;
    private final int USDT_ID = 1;

    @FXML
    public void initialize() {
        updateLogs("🤖 Moteur d'automation prêt.");
        startAutomationEngine();
    }

    /**
     * Boucle principale de surveillance (Polling toutes les 5 secondes)
     */
    private void startAutomationEngine() {
        botTimeline = new Timeline(new KeyFrame(Duration.seconds(5), event -> {
            try {
                // 1. On récupère les ordres LIMIT en attente (PENDING)
                List<Trade> pendingTrades = tradeService.SelectAll().stream()
                        .filter(t -> t.getStatus() == Status.PENDING && t.getOrderMode() == OrderMode.LIMIT)
                        .toList();

                if (pendingTrades.isEmpty()) {
                    lblBotStatus.setText("Statut : En veille (0 ordres)");
                    return;
                }

                lblBotStatus.setText("Statut : Surveillance de " + pendingTrades.size() + " ordres...");

                // 2. Pour chaque ordre, on vérifie le prix réel sur Binance
                for (Trade trade : pendingTrades) {
                    processTradeAnalysis(trade);
                }

            } catch (SQLException e) {
                updateLogs("❌ Erreur BDD : " + e.getMessage());
            }
        }));

        botTimeline.setCycleCount(Animation.INDEFINITE);
        botTimeline.play();
    }

    /**
     * Analyse un trade spécifique en fonction de son actif réel
     */
    private void processTradeAnalysis(Trade trade) throws SQLException {
        // Solution pour l'absence de findById : On cherche dans la liste complète
        Asset asset = assetService.SelectAll().stream()
                .filter(a -> a.getId() == trade.getAsset_id())
                .findFirst()
                .orElse(null);

        if (asset == null) {
            updateLogs("⚠️ Actif #" + trade.getAsset_id() + " non trouvé.");
            return;
        }

        String symbol = asset.getSymbol(); // Ex: "ETH", "BTC", "SOL"
        double currentPrice = marketApi.fetchPrice(symbol); // Récupère le prix sur Binance pour ce symbole

        if (currentPrice <= 0) return;

        boolean shouldExecute = false;

        // Logique de déclenchement
        if (trade.getTradeType() == TradeType.BUY && currentPrice <= trade.getPrice()) {
            shouldExecute = true;
        } else if (trade.getTradeType() == TradeType.SELL && currentPrice >= trade.getPrice()) {
            shouldExecute = true;
        }

        if (shouldExecute) {
            executeBotOrder(trade, currentPrice, symbol);
        }
    }

    /**
     * Exécute l'ordre financièrement et met à jour les données
     */
    private void executeBotOrder(Trade trade, double executionPrice, String symbol) {
        try {
            double totalAmount = trade.getQuantity() * executionPrice;

            // 1. Mise à jour du Wallet (Logique métier de ton service)
            wcService.updateBalanceAfterTrade(USER_WALLET_ID, USDT_ID, totalAmount, trade.getTradeType());

            // 2. Mise à jour du statut de l'ordre
            trade.setStatus(Status.COMPLETED);
            trade.setExecutedAt(LocalDateTime.now());
            tradeService.updateOne(trade);

            // 3. Enregistrement de la transaction financière
            saveTransaction(trade, totalAmount);

            updateLogs("✅ EXÉCUTÉ : " + trade.getTradeType() + " " + symbol + " à " + String.format("%.2f", executionPrice));

        } catch (Exception e) {
            updateLogs("⚠️ Échec (" + symbol + ") : " + e.getMessage());
        }
    }

    private void saveTransaction(Trade trade, double amount) throws SQLException {
        transaction t = new transaction();
        t.setIdWalletSource(trade.getTradeType() == TradeType.BUY ? USER_WALLET_ID : MARKET_WALLET_ID);
        t.setIdWalletDestination(trade.getTradeType() == TradeType.BUY ? MARKET_WALLET_ID : USER_WALLET_ID);
        t.setMontant(amount);
        t.setType(trade.getTradeType() == TradeType.BUY ? typeTransaction.ACHAT : typeTransaction.VENTE);
        t.setCurrencyId(USDT_ID);
        t.setStatut(StatutTransaction.Completed);
        t.setDateTransaction(LocalDateTime.now());
        transService.insertOne(t);
    }

    private void updateLogs(String message) {
        String time = LocalDateTime.now().toString().substring(11, 19);
        Platform.runLater(() -> listLogs.getItems().add(0, "[" + time + "] " + message));
    }

    @FXML
    private void handleStop() {
        if (botTimeline != null) {
            botTimeline.stop();
            lblBotStatus.setText("Statut : Arrêté");
            updateLogs("🛑 Bot arrêté.");
        }
    }
}