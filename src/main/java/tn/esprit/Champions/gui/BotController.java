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
    private final WalletService walletService = new WalletService();
    private final CurrencyService currencyService = new CurrencyService();

    private Timeline botTimeline;

    // Paramètres dynamiques (Identiques à la logique du Dashboard)
    private final int CURRENT_USER_ID = 1;
    private int dynamicUsdtId = -1;
    private int marketWalletId = 4; // Gardé pour la contrepartie technique

    @FXML
    public void initialize() {
        try {
            // Récupération dynamique de l'ID de l'USDT au démarrage
            currency c = currencyService.getByName("USDT");
            if (c != null) {
                this.dynamicUsdtId = c.getId_currency();
            }
        } catch (SQLException e) {
            updateLogs("❌ Erreur init Currency: " + e.getMessage());
        }

        updateLogs("🤖 Moteur d'automation prêt.");
        startAutomationEngine();
    }

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
                    Asset asset = allAssets.stream()
                            .filter(a -> a.getId() == trade.getAsset_id())
                            .findFirst().orElse(null);

                    if (asset != null) {
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

    private void checkConditions(Trade trade, double marketPrice, String symbol) throws SQLException {
        boolean trigger = false;
        if (trade.getTradeType() == TradeType.BUY && marketPrice <= trade.getPrice()) {
            trigger = true;
        }
        else if (trade.getTradeType() == TradeType.SELL && marketPrice >= trade.getPrice()) {
            trigger = true;
        }

        if (trigger) {
            executeBotOrder(trade, marketPrice, symbol);
        }
    }

    private void executeBotOrder(Trade trade, double executionPrice, String symbol) {
        try {
            // --- LOGIQUE DYNAMIQUE DU WALLET ---
            // On récupère le wallet de trading de l'utilisateur qui a passé l'ordre
            wallet userWallet = walletService.SelectAll().stream()
                    .filter(w -> w.getIdUser() == trade.getId_user()) // Dynamique selon l'user du trade
                    .filter(w -> w.getTypeWallet() == typeWallet.trading)
                    .findFirst()
                    .orElse(null);

            if (userWallet == null || dynamicUsdtId == -1) {
                updateLogs("⚠️ Échec : Configuration Wallet/Currency dynamique introuvable.");
                return;
            }

            int walletId = userWallet.getIdWallet();
            double totalAmountInUSDT = trade.getQuantity() * executionPrice;

            // Récupération de l'objet WalletCurrency (USDT) dynamique
            wallet_currency currentWc = wcService.getWalletCurrencyByWalletAndId(walletId, dynamicUsdtId);

            if (currentWc == null) {
                updateLogs("⚠️ Échec : Portefeuille USDT introuvable pour le wallet " + walletId);
                return;
            }

            double oldBalance = currentWc.getSolde();
            double newBalance;

            if (trade.getTradeType() == TradeType.BUY) {
                if (oldBalance < totalAmountInUSDT) {
                    updateLogs(String.format("❌ SOLDE INSUFFISANT pour %s (Besoin: %.2f | Dispo: %.2f)",
                            symbol, totalAmountInUSDT, oldBalance));
                    return;
                }
                newBalance = oldBalance - totalAmountInUSDT;
            } else {
                newBalance = oldBalance + totalAmountInUSDT;
            }

            // Mise à jour du solde
            currentWc.setSolde(newBalance);
            wcService.updateOne(currentWc);

            // Mise à jour de l'ordre
            trade.setPrice(executionPrice);
            trade.setStatus(Status.COMPLETED);
            trade.setExecutedAt(LocalDateTime.now());
            tradeService.updateOne(trade);

            // Sauvegarde transaction avec Wallet ID dynamique
            saveTransaction(trade, totalAmountInUSDT, walletId);

            updateLogs(String.format("✅ EXÉCUTÉ : %s %s | Cible: %.2f -> Réel: %.2f",
                    trade.getTradeType(), symbol, trade.getPrice(), executionPrice));

        } catch (Exception e) {
            updateLogs("⚠️ Échec technique (" + symbol + ") : " + e.getMessage());
        }
    }

    private void saveTransaction(Trade trade, double amount, int walletId) throws SQLException {
        transaction t = new transaction();
        t.setIdWalletSource(trade.getTradeType() == TradeType.BUY ? walletId : marketWalletId);
        t.setIdWalletDestination(trade.getTradeType() == TradeType.BUY ? marketWalletId : walletId);
        t.setMontant(amount);
        t.setType(trade.getTradeType() == TradeType.BUY ? typeTransaction.ACHAT : typeTransaction.VENTE);
        t.setCurrencyId(dynamicUsdtId); // Dynamique
        t.setStatut(StatutTransaction.Completed);
        t.setDateTransaction(LocalDateTime.now());
        transService.insertOne(t);
    }

    private void updateLogs(String message) {
        String time = LocalDateTime.now().toString().substring(11, 19);
        Platform.runLater(() -> {
            if (listLogs != null) listLogs.getItems().add(0, "[" + time + "] " + message);
        });
    }

    @FXML
    private void handleStop() {
        if (botTimeline != null) {
            botTimeline.stop();
            lblBotStatus.setText("Statut : Arrêté");
            updateLogs("🛑 Bot arrêté par l'utilisateur.");
        }
    }
}