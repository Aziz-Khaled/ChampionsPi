package tn.esprit.Champions.gui;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import tn.esprit.Champions.models.*;
import tn.esprit.Champions.services.*;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Controller for the English Trading Dashboard
 * Features: Real-time price updates, RSI signals, PnL tracking,
 * and Database history consultation.
 */
public class TradingDashboard {

    // --- FXML UI Elements ---
    @FXML private TableView<Asset> tableAssets;
    @FXML private TableColumn<Asset, String> colSymbol;
    @FXML private TableColumn<Asset, Double> colPrice;

    @FXML private TableView<Trade> tableHistory;
    @FXML private TableColumn<Trade, String> colHistSymbol;
    @FXML private TableColumn<Trade, Double> colHistQty;
    @FXML private TableColumn<Trade, Double> colHistPrice;
    @FXML private TableColumn<Trade, String> colHistType;
    @FXML private TableColumn<Trade, Status> colHistStatus;

    @FXML private Label lblBalance, lblBalanceTND, lblSelected, lblTotal, lblPnL, lblAdvice;
    @FXML private TextField txtQty;
    @FXML private LineChart<String, Number> priceChart;
    @FXML private VBox paneMarket, paneHistory;

    private XYChart.Series<String, Number> series = new XYChart.Series<>();

    // --- Services ---
    private TransactionService transService = new TransactionService();
    private TradeService tradeService = new TradeService();
    private MarketApiService marketApi = new MarketApiService();
    private TechnicalAnalysisService techService = new TechnicalAnalysisService();
    private wallet_currencyService wcService = new wallet_currencyService();
    private AssetService assetService = new AssetService();

    // --- State Variables ---
    private Asset selectedAsset;
    private double initialEntryPrice = 0.0;
    private final int MY_WALLET_ID = 3;
    private final int MARKET_WALLET_ID = 4;
    private final int USDT_ID = 1;
    private final int CURRENT_USER_ID = 1;

    @FXML
    public void initialize() {
        setupTables();
        if (priceChart != null) {
            priceChart.getData().add(series);
        }

        loadBalance();
        loadTradeHistory();
        startTradingEngine();

        // Listener for Asset Selection
        tableAssets.getSelectionModel().selectedItemProperty().addListener((obs, old, newVal) -> {
            if (newVal != null) {
                selectedAsset = newVal;
                initialEntryPrice = newVal.getCurrentPrice();
                if (lblSelected != null) lblSelected.setText(newVal.getSymbol().toUpperCase() + " / USDT");
                series.getData().clear();

                // Fetch RSI Signal
                double rsi = techService.fetchRSI(newVal.getSymbol());
                if (lblAdvice != null) lblAdvice.setText("RSI Signal: " + techService.getAdvice(rsi));
            }
        });
    }

    // --- NAVIGATION LOGIC ---
    @FXML
    private void showMarket() {
        paneMarket.setVisible(true);
        paneHistory.setVisible(false);
    }

    @FXML
    private void showHistory() {
        paneMarket.setVisible(false);
        paneHistory.setVisible(true);
        loadTradeHistory(); // Auto-refresh from DB when switching
    }

    // --- TRADING ENGINE (Real-time) ---
    private void startTradingEngine() {
        Timeline engine = new Timeline(new KeyFrame(Duration.seconds(2), e -> {
            // Update live prices in table
            List<Asset> assets = tableAssets.getItems();
            if (assets != null) {
                assets.forEach(a -> a.setCurrentPrice(marketApi.fetchPrice(a.getSymbol())));
                tableAssets.refresh();
            }

            if (selectedAsset != null) {
                updateChart(selectedAsset.getCurrentPrice());

                // Real-time PnL Calculation
                double pnl = RiskManager.calculatePnL(selectedAsset.getCurrentPrice(), initialEntryPrice);
                if (lblPnL != null) {
                    lblPnL.setText(String.format("%.2f %%", pnl));
                    lblPnL.setStyle("-fx-text-fill: " + RiskManager.getPnLColor(pnl) + ";");
                }
                updatePreview();
            }
        }));
        engine.setCycleCount(Animation.INDEFINITE);
        engine.play();
    }

    // --- CORE TRADING ACTIONS ---
    @FXML
    private void handleAction(typeTransaction type) {
        if (selectedAsset == null || txtQty.getText().isEmpty()) {
            showNotification("Warning", "Please select an asset and enter a quantity.");
            return;
        }

        try {
            double qty = Double.parseDouble(txtQty.getText());
            double currentPrice = selectedAsset.getCurrentPrice();

            // Business Logic: Applying Fees (isBuy logic handled via separate methods)
            double finalAmount = (type == typeTransaction.ACHAT)
                    ? TradingEngine.calculateTotal(qty, currentPrice)
                    : TradingEngine.calculateSaleGain(qty, currentPrice);

            // 1. Record Transaction (Wallet movement)
            transaction t = new transaction();
            t.setIdWalletSource(type == typeTransaction.ACHAT ? MY_WALLET_ID : MARKET_WALLET_ID);
            t.setIdWalletDestination(type == typeTransaction.ACHAT ? MARKET_WALLET_ID : MY_WALLET_ID);
            t.setMontant(finalAmount);
            t.setType(type);
            t.setCurrencyId(USDT_ID);
            t.setStatut(StatutTransaction.Completed);
            t.setDateTransaction(LocalDateTime.now());
            transService.insertOne(t);

            // 2. Record Trade (Order log with new Statuses)
            Trade trade = new Trade();
            trade.setId_user(CURRENT_USER_ID);
            trade.setAsset_id(selectedAsset.getId());
            trade.setTradeType(type == typeTransaction.ACHAT ? TradeType.BUY : TradeType.SELL);
            trade.setOrderMode(OrderMode.MARKET);
            trade.setPrice(currentPrice);
            trade.setQuantity(qty);

            // Logic: Buy = ACTIVE position, Sell = COMPLETED position
            trade.setStatus(type == typeTransaction.ACHAT ? Status.ACTIVE : Status.COMPLETED);

            trade.setCreatedAt(LocalDateTime.now());
            trade.setExecutedAt(LocalDateTime.now());
            tradeService.insertOne(trade);

            // 3. UI Updates
            loadBalance();
            loadTradeHistory();
            showNotification("Success", "Order executed! Status: " + trade.getStatus());

        } catch (NumberFormatException e) {
            showNotification("Error", "Invalid quantity format.");
        } catch (Exception e) {
            showNotification("Error", "Transaction failed: " + e.getMessage());
        }
    }

    // --- DATABASE DATA LOADING ---
    private void setupTables() {
        // Market Table
        colSymbol.setCellValueFactory(new PropertyValueFactory<>("symbol"));
        colPrice.setCellValueFactory(new PropertyValueFactory<>("currentPrice"));
        try { tableAssets.getItems().setAll(assetService.SelectAll()); } catch (Exception e) {}

        // History Table (Mapped to DB columns)
        colHistSymbol.setCellValueFactory(new PropertyValueFactory<>("asset_id"));
        colHistQty.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        colHistPrice.setCellValueFactory(new PropertyValueFactory<>("price"));
        colHistType.setCellValueFactory(new PropertyValueFactory<>("tradeType"));
        colHistStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
    }

    @FXML
    public void loadTradeHistory() {
        try {
            List<Trade> trades = tradeService.SelectAll();
            if (tableHistory != null) {
                tableHistory.getItems().setAll(trades);
            }
        } catch (Exception e) {
            System.err.println("Consultation Error: " + e.getMessage());
        }
    }

    private void loadBalance() {
        try {
            wallet_currency wc = wcService.getWalletCurrencyByWalletAndId(MY_WALLET_ID, USDT_ID);
            if (wc != null) {
                if (lblBalance != null) lblBalance.setText(String.format("%.2f USDT", wc.getSolde()));
                if (lblBalanceTND != null) lblBalanceTND.setText(String.format("≈ %.2f TND", wc.getSolde() * 3.25));
            }
        } catch (SQLException e) {}
    }

    // --- UI HELPERS ---
    private void updatePreview() {
        if (lblTotal == null || selectedAsset == null) return;
        try {
            double q = Double.parseDouble(txtQty.getText());
            double total = TradingEngine.calculateTotal(q, selectedAsset.getCurrentPrice());
            lblTotal.setText(String.format("Total: %.2f USDT", total));
        } catch (Exception e) { lblTotal.setText("Total: 0.00 USDT"); }
    }

    private void updateChart(double price) {
        if (series == null) return;
        series.getData().add(new XYChart.Data<>(LocalDateTime.now().toString().substring(11, 19), price));
        if (series.getData().size() > 15) series.getData().remove(0);
    }

    @FXML private void onBuy() { handleAction(typeTransaction.ACHAT); }
    @FXML private void onSell() { handleAction(typeTransaction.VENTE); }

    private void showNotification(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.show();
    }
}