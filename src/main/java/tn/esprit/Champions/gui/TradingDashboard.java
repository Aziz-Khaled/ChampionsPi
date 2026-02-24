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

public class TradingDashboard {

    // FXML IDs
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
    private final wallet_currencyService wcService = new wallet_currencyService();
    private final AssetService assetService = new AssetService();
    private final TransactionService transService = new TransactionService();

    // Settings
    private Asset selectedAsset;
    private double initialEntryPrice = 0.0;
    private final double TND_RATE = 3.12;
    private final int MY_WALLET_ID = 3;
    private final int USDT_ID = 1;
    private final int CURRENT_USER_ID = 1;

    @FXML
    public void initialize() {
        setupTables();
        setupChart();
        setupOrderInputs();

        // Asset Selection Listener
        tableAssets.getSelectionModel().selectedItemProperty().addListener((obs, old, newVal) -> {
            if (newVal != null) {
                selectedAsset = newVal;
                initialEntryPrice = newVal.getCurrentPrice();
                lblSelected.setText(newVal.getSymbol().toUpperCase() + " / USDT");
                series.getData().clear();
                updateAISignal(newVal.getSymbol());
            }
        });

        // Quantity Listener for Total Calculation
        txtQty.textProperty().addListener((obs, old, newVal) -> calculateTotal());

        startTradingEngine();
    }

    private void setupOrderInputs() {
        comboOrderMode.getItems().setAll("MARKET", "LIMIT");
        comboOrderMode.setValue("MARKET");

        // UI CONTROL: Disable target price box if mode is MARKET
        txtTargetPrice.disableProperty().bind(comboOrderMode.valueProperty().isEqualTo("MARKET"));

        // Visual feedback for disabled field
        txtTargetPrice.disableProperty().addListener((obs, old, val) -> {
            if (val) {
                txtTargetPrice.setText("");
                txtTargetPrice.setStyle("-fx-background-color: #1e2329; -fx-opacity: 0.5;");
            } else {
                txtTargetPrice.setStyle("-fx-background-color: #2b3139; -fx-opacity: 1;");
            }
        });
    }

    private void startTradingEngine() {
        Timeline engine = new Timeline(new KeyFrame(Duration.seconds(2), e -> {
            // Update Prices from Binance
            tableAssets.getItems().forEach(a -> a.setCurrentPrice(marketApi.fetchPrice(a.getSymbol())));
            tableAssets.refresh();

            if (selectedAsset != null) {
                double livePrice = selectedAsset.getCurrentPrice();
                updateLiveChart(livePrice);
                updatePnL(livePrice);
                calculateTotal();
            }

            Platform.runLater(this::updateBalances);
        }));
        engine.setCycleCount(Animation.INDEFINITE);
        engine.play();
    }

    private void updateBalances() {
        try {
            double usdt = wcService.getBalance(MY_WALLET_ID, USDT_ID);
            lblBalance.setText(String.format("%.2f USDT", usdt));
            lblBalanceTND.setText(String.format("≈ %.3f TND", usdt * TND_RATE));
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void updateAISignal(String symbol) {
        new Thread(() -> {
            double rsi = marketApi.calculateRSI(symbol);
            Platform.runLater(() -> {
                String advice = (rsi < 30) ? "STRONG BUY" : (rsi > 70) ? "STRONG SELL" : "NEUTRAL";
                String color = (rsi < 30) ? "#0ecb81" : (rsi > 70) ? "#f6465d" : "#fcd535";
                lblAdvice.setText("AI SIGNAL: " + advice + " (RSI: " + String.format("%.2f", rsi) + ")");
                lblAdvice.setStyle("-fx-text-fill: " + color + "; -fx-font-weight: bold;");
            });
        }).start();
    }

    private void calculateTotal() {
        if (selectedAsset != null && !txtQty.getText().isEmpty()) {
            try {
                double qty = Double.parseDouble(txtQty.getText().replace(",", "."));
                lblTotal.setText(String.format("%.2f USDT", qty * selectedAsset.getCurrentPrice()));
            } catch (Exception e) { lblTotal.setText("0.00 USDT"); }
        }
    }

    private void updatePnL(double currentPrice) {
        if (initialEntryPrice <= 0) return;
        double pnl = ((currentPrice - initialEntryPrice) / initialEntryPrice) * 100;
        lblPnL.setText(String.format("%+.2f%%", pnl));
        lblPnL.setStyle("-fx-text-fill: " + (pnl >= 0 ? "#0ecb81" : "#f6465d") + "; -fx-font-size: 22; -fx-font-weight: bold;");
    }

    @FXML
    private void onBuy() { processTrade(TradeType.BUY); }

    @FXML
    private void onSell() { processTrade(TradeType.SELL); }

    private void processTrade(TradeType type) {
        if (selectedAsset == null || txtQty.getText().isEmpty()) {
            showAlert("Error", "Please select an asset and quantity.");
            return;
        }

        try {
            double qty = Double.parseDouble(txtQty.getText());
            OrderMode mode = OrderMode.valueOf(comboOrderMode.getValue());
            double price = mode == OrderMode.MARKET ? selectedAsset.getCurrentPrice() : Double.parseDouble(txtTargetPrice.getText());

            Trade trade = new Trade(0, CURRENT_USER_ID, selectedAsset.getId(), type, mode, price, qty,
                    (mode == OrderMode.MARKET ? Status.COMPLETED : Status.PENDING),
                    LocalDateTime.now(), (mode == OrderMode.MARKET ? LocalDateTime.now() : null));

            tradeService.insertOne(trade);
            if(mode == OrderMode.MARKET) wcService.updateBalanceAfterTrade(MY_WALLET_ID, USDT_ID, qty * price, type);

            showAlert("Success", mode + " order placed successfully!");
        } catch (Exception e) { showAlert("Error", "Invalid input data."); }
    }

    // Navigation & Helpers
    @FXML private void showMarket() { paneMarket.setVisible(true); paneHistory.setVisible(false); }
    @FXML private void showHistory() { paneMarket.setVisible(false); paneHistory.setVisible(true); loadTradeHistory(); }

    private void loadTradeHistory() {
        try { tableHistory.getItems().setAll(tradeService.SelectAll()); } catch (SQLException e) {}
    }

    private void setupTables() {
        colSymbol.setCellValueFactory(new PropertyValueFactory<>("symbol"));
        colPrice.setCellValueFactory(new PropertyValueFactory<>("currentPrice"));
        colHistSymbol.setCellValueFactory(new PropertyValueFactory<>("asset_id"));
        colHistQty.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        colHistPrice.setCellValueFactory(new PropertyValueFactory<>("price"));
        colHistType.setCellValueFactory(new PropertyValueFactory<>("tradeType"));
        colHistStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        try { tableAssets.getItems().setAll(assetService.SelectAll()); } catch (SQLException e) {}
    }

    private void setupChart() {
        priceChart.getData().add(series);
        priceChart.setCreateSymbols(false);
    }

    private void updateLiveChart(double price) {
        String time = LocalDateTime.now().toString().substring(17, 19);
        series.getData().add(new XYChart.Data<>(time, price));
        if (series.getData().size() > 20) series.getData().remove(0);
    }

    private void showAlert(String title, String content) {
        Alert a = new Alert(Alert.AlertType.INFORMATION); a.setTitle(title); a.setContentText(content); a.show();
    }

    @FXML private void openBotWindow() { /* Logic to open Bot View */ }
}