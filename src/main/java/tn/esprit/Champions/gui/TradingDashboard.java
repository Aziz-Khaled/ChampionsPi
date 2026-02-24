package tn.esprit.Champions.gui;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
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

    @FXML private Label lblBalance, lblBalanceTND, lblSelected, lblPnL, lblAdvice, lblTotal;
    @FXML private TextField txtQty, txtTargetPrice;
    @FXML private ComboBox<String> comboOrderMode;
    @FXML private LineChart<String, Number> priceChart;
    @FXML private VBox paneMarket, paneHistory;

    private XYChart.Series<String, Number> series = new XYChart.Series<>();

    // Services
    private final TradeService tradeService = new TradeService();
    private final MarketApiService marketApi = new MarketApiService();
    private final TechnicalAnalysisService techService = new TechnicalAnalysisService();
    private final wallet_currencyService wcService = new wallet_currencyService();
    private final AssetService assetService = new AssetService();
    private final TransactionService transService = new TransactionService();

    private Asset selectedAsset;
    private double initialEntryPrice = 0.0;
    private final double USDT_TND_RATE = 3.12;

    private final int MY_WALLET_ID = 3;
    private final int MARKET_WALLET_ID = 4;
    private final int USDT_ID = 1;
    private final int CURRENT_USER_ID = 1;

    @FXML
    public void initialize() {
        setupTables();
        setupChart();

        if (comboOrderMode != null) comboOrderMode.setValue("MARKET");

        // Écouteur pour le calcul du total en temps réel
        txtQty.textProperty().addListener((obs, old, newVal) -> refreshTotalLabel());

        // Listener sélection d'actif
        tableAssets.getSelectionModel().selectedItemProperty().addListener((obs, old, newVal) -> {
            if (newVal != null) {
                selectedAsset = newVal;
                initialEntryPrice = newVal.getCurrentPrice();
                lblSelected.setText(newVal.getSymbol().toUpperCase() + " / USDT");
                series.getData().clear();
                refreshTotalLabel();
                updateTechnicalAnalysis(newVal.getSymbol());
            }
        });

        startTradingEngine();
    }

    private void setupTables() {
        colSymbol.setCellValueFactory(new PropertyValueFactory<>("symbol"));
        colPrice.setCellValueFactory(new PropertyValueFactory<>("currentPrice"));

        colHistSymbol.setCellValueFactory(new PropertyValueFactory<>("asset_id"));
        colHistQty.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        colHistPrice.setCellValueFactory(new PropertyValueFactory<>("price"));
        colHistType.setCellValueFactory(new PropertyValueFactory<>("tradeType"));
        colHistStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        refreshAssetList();
    }

    private void setupChart() {
        priceChart.getData().add(series);
        priceChart.setCreateSymbols(false);
        priceChart.setAnimated(false);
    }

    private void startTradingEngine() {
        Timeline engine = new Timeline(new KeyFrame(Duration.seconds(2), e -> {
            // 1. Mise à jour des prix live
            tableAssets.getItems().forEach(a -> a.setCurrentPrice(marketApi.fetchPrice(a.getSymbol())));
            tableAssets.refresh();

            // 2. Mise à jour de l'UI si un actif est sélectionné
            if (selectedAsset != null) {
                double livePrice = selectedAsset.getCurrentPrice();
                updateChart(livePrice);
                refreshTotalLabel(); // Le total change car le prix bouge
                updatePnL(livePrice);
            }

            // 3. Sync automatique (Actions du Bot en arrière-plan)
            Platform.runLater(() -> {
                loadBalance();
                loadTradeHistory();
            });
        }));
        engine.setCycleCount(Animation.INDEFINITE);
        engine.play();
    }

    private void refreshTotalLabel() {
        if (selectedAsset != null && !txtQty.getText().isEmpty()) {
            try {
                double qty = Double.parseDouble(txtQty.getText());
                double total = qty * selectedAsset.getCurrentPrice();
                lblTotal.setText(String.format("%.2f USDT", total));
            } catch (NumberFormatException e) {
                lblTotal.setText("0.00 USDT");
            }
        } else {
            lblTotal.setText("0.00 USDT");
        }
    }

    private void updatePnL(double currentPrice) {
        double pnl = RiskManager.calculatePnL(currentPrice, initialEntryPrice);
        lblPnL.setText(String.format("%.2f %%", pnl));
        lblPnL.setStyle("-fx-text-fill: " + RiskManager.getPnLColor(pnl) + "; -fx-font-size: 24; -fx-font-weight: bold;");
    }

    private void updateTechnicalAnalysis(String symbol) {
        double rsi = techService.fetchRSI(symbol);
        lblAdvice.setText("RSI: " + String.format("%.2f", rsi) + " (" + techService.getAdvice(rsi) + ")");
    }

    @FXML
    private void handleAction(typeTransaction type) {
        if (selectedAsset == null || txtQty.getText().isEmpty()) {
            showAlert("Action requise", "Sélectionnez un actif et une quantité.");
            return;
        }

        try {
            double qty = Double.parseDouble(txtQty.getText());
            if (comboOrderMode.getValue().equals("LIMIT")) {
                double target = Double.parseDouble(txtTargetPrice.getText());
                placeLimitOrder(type, qty, target);
            } else {
                executeMarketTrade(type, qty);
            }
        } catch (Exception e) {
            showAlert("Erreur", "Vérifiez vos saisies : " + e.getMessage());
        }
    }

    private void executeMarketTrade(typeTransaction type, double qty) throws SQLException {
        double price = selectedAsset.getCurrentPrice();
        double total = qty * price;
        TradeType tType = (type == typeTransaction.ACHAT) ? TradeType.BUY : TradeType.SELL;

        // Débit/Crédit réel
        wcService.updateBalanceAfterTrade(MY_WALLET_ID, USDT_ID, total, tType);

        // Enregistrement du Trade
        Trade trade = new Trade(CURRENT_USER_ID, selectedAsset.getId(), tType, price, qty, Status.COMPLETED);
        trade.setOrderMode(OrderMode.MARKET);
        trade.setCreatedAt(LocalDateTime.now());
        tradeService.insertOne(trade);

        saveTransactionRecord(type, total);
        showAlert("Succès", "Ordre au marché exécuté !");
    }

    private void placeLimitOrder(typeTransaction type, double qty, double target) throws SQLException {
        Trade limit = new Trade(CURRENT_USER_ID, selectedAsset.getId(),
                (type == typeTransaction.ACHAT ? TradeType.BUY : TradeType.SELL), target, qty, Status.PENDING);
        limit.setOrderMode(OrderMode.LIMIT);
        limit.setCreatedAt(LocalDateTime.now());
        tradeService.insertOne(limit);
        showAlert("IA Activée", "Ordre placé. Le Bot surveille le prix cible.");
    }

    private void saveTransactionRecord(typeTransaction type, double amount) throws SQLException {
        transaction t = new transaction();
        t.setIdWalletSource(type == typeTransaction.ACHAT ? MY_WALLET_ID : MARKET_WALLET_ID);
        t.setIdWalletDestination(type == typeTransaction.ACHAT ? MARKET_WALLET_ID : MY_WALLET_ID);
        t.setMontant(amount);
        t.setType(type);
        t.setCurrencyId(USDT_ID);
        t.setStatut(StatutTransaction.Completed);
        t.setDateTransaction(LocalDateTime.now());
        transService.insertOne(t);
    }

    private void loadBalance() {
        try {
            double usdt = wcService.getBalance(MY_WALLET_ID, USDT_ID);
            lblBalance.setText(String.format("%.2f USDT", usdt));
            lblBalanceTND.setText(String.format("%.2f TND", usdt * USDT_TND_RATE));
        } catch (SQLException e) { /* Log error */ }
    }

    public void loadTradeHistory() {
        try {
            tableHistory.getItems().setAll(tradeService.SelectAll());
        } catch (SQLException e) { /* Log error */ }
    }

    private void refreshAssetList() {
        try {
            tableAssets.getItems().setAll(assetService.SelectAll());
        } catch (SQLException e) { /* Log error */ }
    }

    private void updateChart(double price) {
        String time = LocalDateTime.now().toString().substring(11, 19);
        series.getData().add(new XYChart.Data<>(time, price));
        if (series.getData().size() > 15) series.getData().remove(0);
    }

    @FXML private void openBotWindow() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/BotView.fxml"));
            Stage stage = new Stage();
            stage.setScene(new Scene(loader.load()));
            stage.setTitle("🤖 Champions Bot Engine");
            stage.show();
        } catch (IOException e) { e.printStackTrace(); }
    }

    @FXML private void onBuy() { handleAction(typeTransaction.ACHAT); }
    @FXML private void onSell() { handleAction(typeTransaction.VENTE); }
    @FXML private void showMarket() { paneMarket.setVisible(true); paneHistory.setVisible(false); }
    @FXML private void showHistory() { paneMarket.setVisible(false); paneHistory.setVisible(true); loadTradeHistory(); }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.show();
    }
}