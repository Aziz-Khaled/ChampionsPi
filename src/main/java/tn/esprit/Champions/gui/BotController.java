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
    private final TransactionService transactionService = new TransactionService();
    private final AssetService assetService = new AssetService();
    private final WalletService walletService = new WalletService();
    private final CurrencyService currencyService = new CurrencyService();
    private final BlockchainService blockchainService = new BlockchainService(); // ✅ AJOUTÉ

    private Timeline botTimeline;

    @FXML
    public void initialize() {
        updateLogs("🤖 Automation engine ready.");        startAutomationEngine();
    }

    private void startAutomationEngine() {
        botTimeline = new Timeline(new KeyFrame(Duration.seconds(5), event -> {
            try {
                // 1. Recharger les actifs et les trades directement de la DB
                List<Asset> allAssets = assetService.SelectAll();
                List<Trade> allTrades = tradeService.SelectAll();

                // 2. Filtrage robuste (on compare les noms en majuscules pour éviter les erreurs d'Enum)
                List<Trade> pendingTrades = allTrades.stream()
                        .filter(t -> t.getStatus() != null &&
                                t.getStatus().name().equalsIgnoreCase("PENDING"))
                        .filter(t -> t.getOrderMode() != null &&
                                t.getOrderMode().name().equalsIgnoreCase("LIMIT"))
                        .toList();

                // 3. Mise à jour de l'UI
                if (pendingTrades.isEmpty()) {
                    Platform.runLater(() -> lblBotStatus.setText("Status: Idle (0 PENDING orders)"));                    return;
                }

                Platform.runLater(() -> lblBotStatus.setText("Status: Monitoring " + pendingTrades.size() + " orders..."));
                // 4. Boucle de vérification
                for (Trade trade : pendingTrades) {
                    // Trouver l'asset correspondant par ID
                    Asset asset = allAssets.stream()
                            .filter(a -> a.getId() == trade.getAsset_id())
                            .findFirst()
                            .orElse(null);

                    if (asset != null) {
                        // Récupérer le prix réel actuel via l'API
                        double currentPrice = marketApi.fetchPrice(asset.getSymbol());

                        if (currentPrice > 0) {
                            checkConditions(trade, currentPrice, asset.getSymbol());
                        }
                    } else {
                        System.out.println("⚠️ Asset ID " + trade.getAsset_id() + " not found for trade " + trade.getId());                    }
                }
            } catch (Exception e) {
                // Utilise Exception pour attraper aussi les NullPointer potentiels
                updateLogs("❌ Engine error: " + e.getMessage());                e.printStackTrace();
            }
        }));
        botTimeline.setCycleCount(Animation.INDEFINITE);
        botTimeline.play();
    }

    private void checkConditions(Trade trade, double marketPrice, String symbol) throws SQLException {
        boolean trigger = false;
        if (trade.getTradeType() == TradeType.BUY && marketPrice <= trade.getPrice()) trigger = true;
        else if (trade.getTradeType() == TradeType.SELL && marketPrice >= trade.getPrice()) trigger = true;

        if (trigger) executeBotOrder(trade, marketPrice, symbol);
    }

    private void executeBotOrder(Trade trade, double executionPrice, String symbol) {
        try {
            // 1. Récupération du Wallet Trading
            wallet userWallet = walletService.SelectAll().stream()
                    .filter(w -> w.getIdUser() == trade.getId_user())
                    .filter(w -> w.getTypeWallet() == typeWallet.trading)
                    .findFirst()
                    .orElse(null);

            if (userWallet == null) return;

            // 2. Préparation des devises (Important: Nettoyage du symbole)
            String cleanSymbol = symbol.toUpperCase().replace("USDT", "").trim();
            currency usdt = currencyService.getByName("USDT");
            currency assetCurr = currencyService.getByName(cleanSymbol);

            if (usdt == null || assetCurr == null) {
                updateLogs("❌ Erreur : Devise " + cleanSymbol + " introuvable.");
                return;
            }

            // 3. Logique de transaction (Conversion)
            double totalUSDT = trade.getQuantity() * executionPrice;
            Conversion conv = new Conversion();
            conv.setExchangeRate(executionPrice);

            transaction t = new transaction();
            t.setIdWalletSource(userWallet.getIdWallet());
            t.setIdWalletDestination(userWallet.getIdWallet());
            t.setStatut(StatutTransaction.Completed);
            t.setDateTransaction(LocalDateTime.now());

            if (trade.getTradeType() == TradeType.BUY) {
                conv.setAmountFrom(totalUSDT); conv.setCurrencyFrom(usdt.getId_currency());
                conv.setAmountTo(trade.getQuantity()); conv.setCurrencyTo(assetCurr.getId_currency());
                t.setType(typeTransaction.ACHAT);
                t.setMontant(totalUSDT);
                t.setCurrencyId(usdt.getId_currency());
            } else {
                conv.setAmountFrom(trade.getQuantity()); conv.setCurrencyFrom(assetCurr.getId_currency());
                conv.setAmountTo(totalUSDT); conv.setCurrencyTo(usdt.getId_currency());
                t.setType(typeTransaction.VENTE);
                t.setMontant(trade.getQuantity());
                t.setCurrencyId(assetCurr.getId_currency());
            }

            // --- EXÉCUTION RÉELLE (Mise à jour base + Blockchain) ---
            // Cette ligne appelle ta méthode insertExchange qui gère les soldes et la blockchain
            transactionService.insertExchange(t, conv);

            // 4. Mise à jour du statut du Trade
            trade.setStatus(Status.COMPLETED);
            trade.setPrice(executionPrice);
            trade.setExecutedAt(LocalDateTime.now());
            tradeService.updateOne(trade); // ✅ Change PENDING en COMPLETED en base

            updateLogs(String.format("✅ BOT EXÉCUTÉ : %s %s à %.2f", trade.getTradeType(), cleanSymbol, executionPrice));

        } catch (Exception e) {
            updateLogs("❌ Erreur : " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void updateLogs(String message) {
        String time = LocalDateTime.now().toString().substring(11, 19);
        Platform.runLater(() -> {
            if (listLogs != null) listLogs.getItems().add(0, "[" + time + "] " + message);
        });
    }

    @FXML private void handleStop() {
        if (botTimeline != null) {
            botTimeline.stop();
            lblBotStatus.setText("Statut : Arrêté");
            updateLogs("🛑 Bot arrêté.");
        }
    }
}