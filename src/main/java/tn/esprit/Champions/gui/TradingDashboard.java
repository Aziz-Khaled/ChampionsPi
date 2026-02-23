package tn.esprit.Champions.gui;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;
import tn.esprit.Champions.models.*;
import tn.esprit.Champions.services.*;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

public class TradingDashboard {

    @FXML private TableView<Asset> tableAssets;
    @FXML private TableColumn<Asset, String> colSymbol;
    @FXML private TableColumn<Asset, Double> colPrice;

    @FXML private TableView<Trade> tableHistory;
    @FXML private TableColumn<Trade, String> colHistSymbol, colHistType;
    @FXML private TableColumn<Trade, Double> colHistQty, colHistPrice;
    @FXML private TableColumn<Trade, Status> colHistStatus;

    @FXML private Label lblBalance, lblBalanceTND, lblSelected, lblTotal, lblPnL, lblAdvice;
    @FXML private TextField txtQty, txtTargetPrice;
    @FXML private ComboBox<String> comboOrderMode;
    @FXML private LineChart<String, Number> priceChart;
    @FXML private VBox paneMarket, paneHistory;

    private XYChart.Series<String, Number> series = new XYChart.Series<>();

    // Services
    private TransactionService transService = new TransactionService();
    private TradeService tradeService = new TradeService();
    private MarketApiService marketApi = new MarketApiService();
    private TechnicalAnalysisService techService = new TechnicalAnalysisService();
    private wallet_currencyService wcService = new wallet_currencyService();
    private AssetService assetService = new AssetService();

    private Asset selectedAsset;
    private double initialEntryPrice = 0.0;
    private final int MY_WALLET_ID = 3;
    private final int MARKET_WALLET_ID = 4;
    private final int USDT_ID = 1;
    private final int CURRENT_USER_ID = 1;

    @FXML
    public void initialize() {
        setupTables();
        if (priceChart != null) priceChart.getData().add(series);
        if (comboOrderMode != null) comboOrderMode.setValue("MARKET");

        loadBalance();
        loadTradeHistory();
        startTradingEngine();

        // Listener pour la sélection d'actifs
        tableAssets.getSelectionModel().selectedItemProperty().addListener((obs, old, newVal) -> {
            if (newVal != null) {
                selectedAsset = newVal;
                initialEntryPrice = newVal.getCurrentPrice();
                if (lblSelected != null) lblSelected.setText(newVal.getSymbol().toUpperCase() + " / USDT");
                series.getData().clear();

                // Analyse technique auto au clic
                double rsi = techService.fetchRSI(newVal.getSymbol());
                if (lblAdvice != null) lblAdvice.setText("RSI: " + techService.getAdvice(rsi));
            }
        });
    }

    private void setupTables() {
        // Table des actifs (Prix live)
        colSymbol.setCellValueFactory(new PropertyValueFactory<>("symbol"));
        colPrice.setCellValueFactory(new PropertyValueFactory<>("currentPrice"));

        // Table de l'historique (Trace des ordres)
        colHistSymbol.setCellValueFactory(new PropertyValueFactory<>("asset_id"));
        colHistQty.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        colHistPrice.setCellValueFactory(new PropertyValueFactory<>("price"));
        colHistType.setCellValueFactory(new PropertyValueFactory<>("tradeType"));
        colHistStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        try {
            List<Asset> assets = assetService.SelectAll();
            tableAssets.getItems().setAll(assets);
        } catch (Exception e) { System.err.println("Load Error: " + e.getMessage()); }
    }

    /**
     * MOTEUR DE MISE À JOUR (Trading Engine)
     * Rafraîchit les prix, le graphique et synchronise la trace BDD
     */
    private void startTradingEngine() {
        Timeline engine = new Timeline(new KeyFrame(Duration.seconds(2), e -> {
            // 1. Mise à jour des prix des actifs
            tableAssets.getItems().forEach(a -> a.setCurrentPrice(marketApi.fetchPrice(a.getSymbol())));
            tableAssets.refresh();

            // 2. Si un actif est sélectionné, on met à jour le graph et le PnL
            if (selectedAsset != null) {
                double livePrice = selectedAsset.getCurrentPrice();
                updateChart(livePrice);

                double pnl = RiskManager.calculatePnL(livePrice, initialEntryPrice);
                lblPnL.setText(String.format("%.2f %%", pnl));
                lblPnL.setStyle("-fx-text-fill: " + RiskManager.getPnLColor(pnl) + ";");
            }

            // 3. MISE À JOUR DE LA TRACE : On recharge l'historique pour voir les ordres exécutés par le bot
            Platform.runLater(this::loadTradeHistory);

        }));
        engine.setCycleCount(Animation.INDEFINITE);
        engine.play();
    }

    @FXML
    private void handleAction(typeTransaction type) {
        if (selectedAsset == null || txtQty.getText().isEmpty()) return;

        try {
            double qty = Double.parseDouble(txtQty.getText());
            OrderMode mode = comboOrderMode.getValue().equals("LIMIT") ? OrderMode.LIMIT : OrderMode.MARKET;

            if (mode == OrderMode.LIMIT) {
                // LOGIQUE MÉTIER AVANCÉ : Ordre conditionnel (Trace PENDING)
                double target = Double.parseDouble(txtTargetPrice.getText());
                Trade limitOrder = new Trade();
                limitOrder.setId_user(CURRENT_USER_ID);
                limitOrder.setAsset_id(selectedAsset.getId());
                limitOrder.setQuantity(qty);
                limitOrder.setPrice(target);
                limitOrder.setTradeType(type == typeTransaction.ACHAT ? TradeType.BUY : TradeType.SELL);
                limitOrder.setOrderMode(OrderMode.LIMIT);
                limitOrder.setStatus(Status.PENDING);
                limitOrder.setCreatedAt(LocalDateTime.now());

                tradeService.insertOne(limitOrder);
                loadTradeHistory(); // Affiche immédiatement la trace "PENDING"
                showNotification("Ordre Automatique", "Cible fixée à " + target + ". L'IA surveille le prix.");
            } else {
                // Ordre au marché immédiat
                executeCoreTrade(type, qty, selectedAsset.getCurrentPrice());
            }
        } catch (Exception e) {
            showNotification("Erreur", "Saisie invalide.");
        }
    }

    private void executeCoreTrade(typeTransaction type, double qty, double price) {
        try {
            double amount = qty * price;

            // 1. Enregistrement Transaction
            transaction t = new transaction();
            t.setIdWalletSource(type == typeTransaction.ACHAT ? MY_WALLET_ID : MARKET_WALLET_ID);
            t.setIdWalletDestination(type == typeTransaction.ACHAT ? MARKET_WALLET_ID : MY_WALLET_ID);
            t.setMontant(amount);
            t.setType(type);
            t.setCurrencyId(USDT_ID);
            t.setStatut(StatutTransaction.Completed);
            t.setDateTransaction(LocalDateTime.now());
            transService.insertOne(t);

            // 2. Enregistrement Trace Trade (Status COMPLETED)
            Trade trade = new Trade();
            trade.setId_user(CURRENT_USER_ID);
            trade.setAsset_id(selectedAsset.getId());
            trade.setTradeType(type == typeTransaction.ACHAT ? TradeType.BUY : TradeType.SELL);
            trade.setOrderMode(OrderMode.MARKET);
            trade.setPrice(price);
            trade.setQuantity(qty);
            trade.setStatus(Status.COMPLETED);
            trade.setCreatedAt(LocalDateTime.now());
            trade.setExecutedAt(LocalDateTime.now());

            tradeService.insertOne(trade);

            loadBalance();
            loadTradeHistory();
            showNotification("Exécution", "Achat/Vente effectué au prix du marché.");
        } catch (Exception e) { e.printStackTrace(); }
    }

    public void loadTradeHistory() {
        try {
            // On récupère tous les trades (Limit et Market) pour voir la progression
            List<Trade> history = tradeService.SelectAll();
            tableHistory.getItems().setAll(history);
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void loadBalance() {
        try {
            wallet_currency wc = wcService.getWalletCurrencyByWalletAndId(MY_WALLET_ID, USDT_ID);
            if (wc != null) lblBalance.setText(String.format("%.2f USDT", wc.getSolde()));
        } catch (Exception e) {}
    }

    @FXML
    private void openBotWindow() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/BotView.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle("🤖 FinTech Autonomous Bot");
            stage.setScene(new Scene(root));
            stage.setAlwaysOnTop(true);
            stage.show();
        } catch (IOException e) { e.printStackTrace(); }
    }

    private void updateChart(double price) {
        series.getData().add(new XYChart.Data<>(LocalDateTime.now().toString().substring(11, 19), price));
        if (series.getData().size() > 15) series.getData().remove(0);
    }

    private void showNotification(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.show();
    }

    @FXML private void onBuy() { handleAction(typeTransaction.ACHAT); }
    @FXML private void onSell() { handleAction(typeTransaction.VENTE); }
    @FXML private void showMarket() { paneMarket.setVisible(true); paneHistory.setVisible(false); }
    @FXML private void showHistory() { paneMarket.setVisible(false); paneHistory.setVisible(true); loadTradeHistory(); }
}