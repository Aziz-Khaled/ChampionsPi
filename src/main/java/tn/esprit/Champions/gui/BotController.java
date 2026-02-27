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

    // IDs de configuration (Assure-toi qu'ils correspondent à ta BDD)
    private final int USER_WALLET_ID = 3;
    private final int MARKET_WALLET_ID = 4;
    private final int USDT_ID = 1;

    @FXML
    public void initialize() {
        updateLogs("🤖 Moteur d'automation prêt.");
        startAutomationEngine();
    }

    /**
     * Boucle principale du Bot : Scan la base de données toutes les 5 secondes
     */
    private void startAutomationEngine() {
        botTimeline = new Timeline(new KeyFrame(Duration.seconds(5), event -> {
            try {
                List<Asset> allAssets = assetService.SelectAll();

                // On récupère uniquement les ordres LIMIT en attente (PENDING)
                List<Trade> pendingTrades = tradeService.SelectAll().stream()
                        .filter(t -> t.getStatus() == Status.PENDING && t.getOrderMode() == OrderMode.LIMIT)
                        .toList();

                if (pendingTrades.isEmpty()) {
                    lblBotStatus.setText("Statut : En veille (0 ordres)");
                    return;
                }

                lblBotStatus.setText("Statut : Surveillance de " + pendingTrades.size() + " ordres...");

                for (Trade trade : pendingTrades) {
                    // Trouver l'actif correspondant au trade
                    Asset asset = allAssets.stream()
                            .filter(a -> a.getId() == trade.getAsset_id())
                            .findFirst().orElse(null);

                    if (asset != null) {
                        // Récupérer le prix actuel du marché via l'API
                        double currentPrice = marketApi.fetchPrice(asset.getSymbol());
                        if (currentPrice > 0) {
                            checkConditions(trade, currentPrice, asset.getSymbol());
                        }
                    }
                }
            } catch (SQLException e) {
                updateLogs("❌ Erreur SQL : " + e.getMessage());
            }
        }));
        botTimeline.setCycleCount(Animation.INDEFINITE);
        botTimeline.play();
    }

    /**
     * Vérifie si les conditions d'exécution sont remplies
     */
    private void checkConditions(Trade trade, double marketPrice, String symbol) throws SQLException {
        boolean trigger = false;

        // Condition ACHAT : Prix du marché <= Mon prix cible
        if (trade.getTradeType() == TradeType.BUY && marketPrice <= trade.getPrice()) {
            trigger = true;
        }
        // Condition VENTE : Prix du marché >= Mon prix cible
        else if (trade.getTradeType() == TradeType.SELL && marketPrice >= trade.getPrice()) {
            trigger = true;
        }

        if (trigger) {
            executeBotOrder(trade, marketPrice, symbol);
        }
    }

    /**
     * Exécute l'ordre, met à jour le portefeuille et enregistre la transaction
     */
    private void executeBotOrder(Trade trade, double executionPrice, String symbol) {
        try {
            double totalAmountInUSDT = trade.getQuantity() * executionPrice;

            // 1. Récupération de l'objet WalletCurrency (USDT)
            wallet_currency currentWc = wcService.getWalletCurrencyByWalletAndId(USER_WALLET_ID, USDT_ID);

            if (currentWc == null) {
                updateLogs("⚠️ Échec : Portefeuille USDT introuvable.");
                return;
            }

            double oldBalance = currentWc.getSolde();
            double newBalance;

            // --- SÉCURITÉ : VÉRIFICATION DU SOLDE AVANT ACHAT ---
            if (trade.getTradeType() == TradeType.BUY) {
                if (oldBalance < totalAmountInUSDT) {
                    updateLogs(String.format("❌ SOLDE INSUFFISANT pour %s (Besoin: %.2f | Dispo: %.2f)",
                            symbol, totalAmountInUSDT, oldBalance));
                    return; // On arrête l'exécution
                }
                newBalance = oldBalance - totalAmountInUSDT;
            } else {
                // Pour une vente, on ajoute le montant au solde USDT
                newBalance = oldBalance + totalAmountInUSDT;
            }

            // 2. Mise à jour du solde en BDD via updateOne
            currentWc.setSolde(newBalance);
            wcService.updateOne(currentWc);

            // 3. Mise à jour de l'ordre de Trade
            double targetPrice = trade.getPrice();
            trade.setPrice(executionPrice); // On enregistre le prix réel d'exécution
            trade.setStatus(Status.COMPLETED);
            trade.setExecutedAt(LocalDateTime.now());
            tradeService.updateOne(trade);

            // 4. Enregistrement de la transaction historique
            saveTransaction(trade, totalAmountInUSDT);

            updateLogs(String.format("✅ EXÉCUTÉ : %s %s | Cible: %.2f -> Réel: %.2f",
                    trade.getTradeType(), symbol, targetPrice, executionPrice));

        } catch (Exception e) {
            updateLogs("⚠️ Échec technique (" + symbol + ") : " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Sauvegarde la transaction financière en BDD
     */
    private void saveTransaction(Trade trade, double amount) throws SQLException {
        transaction t = new transaction();
        // Si achat : l'argent sort du user vers le marché (ou inversement selon ta logique)
        t.setIdWalletSource(trade.getTradeType() == TradeType.BUY ? USER_WALLET_ID : MARKET_WALLET_ID);
        t.setIdWalletDestination(trade.getTradeType() == TradeType.BUY ? MARKET_WALLET_ID : USER_WALLET_ID);
        t.setMontant(amount);
        t.setType(trade.getTradeType() == TradeType.BUY ? typeTransaction.ACHAT : typeTransaction.VENTE);
        t.setCurrencyId(USDT_ID);
        t.setStatut(StatutTransaction.Completed);
        t.setDateTransaction(LocalDateTime.now());
        transService.insertOne(t);
    }

    /**
     * Ajoute un message dans la liste des logs de l'interface
     */
    private void updateLogs(String message) {
        String time = LocalDateTime.now().toString().substring(11, 19);
        Platform.runLater(() -> {
            listLogs.getItems().add(0, "[" + time + "] " + message);
        });
    }

    /**
     * Arrête manuellement le moteur du bot
     */
    @FXML
    private void handleStop() {
        if (botTimeline != null) {
            botTimeline.stop();
            lblBotStatus.setText("Statut : Arrêté");
            updateLogs("🛑 Bot arrêté par l'utilisateur.");
        }
    }
}