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

    private final TradeService tradeService = new TradeService();
    private final MarketApiService marketApi = new MarketApiService();
    private final TransactionService transService = new TransactionService();
    private final wallet_currencyService wcService = new wallet_currencyService();
    private final AssetService assetService = new AssetService();

    private Timeline botTimeline;

    private final int USER_WALLET_ID = 3;
    private final int MARKET_WALLET_ID = 4;
    private final int USDT_ID = 1;

    @FXML
    public void initialize() {
        updateLogs("🤖 Moteur d'automation prêt.");
        startAutomationEngine();
    }

    private void startAutomationEngine() {
        botTimeline = new Timeline(new KeyFrame(Duration.seconds(5), event -> {
            try {
                List<Asset> allAssets = assetService.SelectAll();
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
                        if (currentPrice > 0) checkConditions(trade, currentPrice, asset.getSymbol());
                    }
                }
            } catch (SQLException e) { updateLogs("❌ Erreur : " + e.getMessage()); }
        }));
        botTimeline.setCycleCount(Animation.INDEFINITE);
        botTimeline.play();
    }

    private void checkConditions(Trade trade, double marketPrice, String symbol) throws SQLException {
        boolean trigger = false;
        // Condition Classique (Achat si prix baisse, Vente si prix monte)
        if (trade.getTradeType() == TradeType.BUY && marketPrice <= trade.getPrice()) trigger = true;
        else if (trade.getTradeType() == TradeType.SELL && marketPrice >= trade.getPrice()) trigger = true;

        if (trigger) executeBotOrder(trade, marketPrice, symbol);
    }

    private void executeBotOrder(Trade trade, double executionPrice, String symbol) {
        try {
            double totalAmount = trade.getQuantity() * executionPrice;

            // 1. Mise à jour Wallet avec prix RÉEL du marché
            wcService.updateBalanceAfterTrade(USER_WALLET_ID, USDT_ID, totalAmount, trade.getTradeType());

            // 2. MISE À JOUR PROFESSIONNELLE : On remplace le prix cible par le prix RÉEL d'exécution
            double oldLimitPrice = trade.getPrice();
            trade.setPrice(executionPrice);
            trade.setStatus(Status.COMPLETED);
            trade.setExecutedAt(LocalDateTime.now());

            // Sauvegarde en BDD (Écrase l'ancien prix par le prix réel)
            tradeService.updateOne(trade);

            // 3. Enregistrement transaction
            saveTransaction(trade, totalAmount);

            updateLogs(String.format("✅ EXÉCUTÉ : %s %s | Cible: %.2f -> Réel: %.2f",
                    trade.getTradeType(), symbol, oldLimitPrice, executionPrice));

        } catch (Exception e) { updateLogs("⚠️ Échec (" + symbol + ") : " + e.getMessage()); }
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

    @FXML private void handleStop() {
        if (botTimeline != null) {
            botTimeline.stop();
            lblBotStatus.setText("Statut : Arrêté");
            updateLogs("🛑 Bot arrêté.");
        }
    }
}