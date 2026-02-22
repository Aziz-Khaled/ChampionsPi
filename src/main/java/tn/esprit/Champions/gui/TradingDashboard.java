package tn.esprit.Champions.gui;

import javafx.animation.*;
import javafx.fxml.FXML;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.util.Duration;
import tn.esprit.Champions.models.*;
import tn.esprit.Champions.services.*;
import java.sql.SQLException;
import java.time.LocalDateTime;

public class TradingDashboard {

    @FXML private TableView<Asset> tableAssets;
    @FXML private TableColumn<Asset, String> colSymbol;
    @FXML private TableColumn<Asset, Double> colPrice;
    @FXML private Label lblBalance, lblBalanceTND, lblSelected, lblTotal, lblPnL, lblAdvice;
    @FXML private TextField txtQty;
    @FXML private LineChart<String, Number> priceChart;

    private XYChart.Series<String, Number> series = new XYChart.Series<>();

    // Services
    private TransactionService transService = new TransactionService();
    private MarketApiService marketApi = new MarketApiService();
    private TechnicalAnalysisService techService = new TechnicalAnalysisService();
    private wallet_currencyService wcService = new wallet_currencyService();
    private WalletService walletService = new WalletService();
    private AssetService assetService = new AssetService();

    private Asset selectedAsset;
    private double initialEntryPrice = 0.0;
    private final int MY_WALLET_ID = 3;
    private final int MARKET_WALLET_ID = 4;
    private final int USDT_ID = 1;

    @FXML
    public void initialize() {
        setupTable();
        priceChart.getData().add(series);
        loadBalance();

        // 1. Moteur temps réel (API Market + Risk Manager)
        startTradingEngine();

        // 2. Listener sur la sélection du tableau
        tableAssets.getSelectionModel().selectedItemProperty().addListener((obs, old, newVal) -> {
            if (newVal != null) {
                selectedAsset = newVal;
                initialEntryPrice = newVal.getCurrentPrice();
                lblSelected.setText(newVal.getSymbol().toUpperCase() + " / USDT");
                series.getData().clear();

                // Appel API RSI (Technical Analysis)
                double rsi = techService.fetchRSI(newVal.getSymbol());
                lblAdvice.setText("Signal: " + techService.getAdvice(rsi));
            }
        });
    }

    private void startTradingEngine() {
        Timeline engine = new Timeline(new KeyFrame(Duration.seconds(2), e -> {
            // Refresh prices from Market API
            tableAssets.getItems().forEach(a -> a.setCurrentPrice(marketApi.fetchPrice(a.getSymbol())));
            tableAssets.refresh();

            if (selectedAsset != null) {
                // Update UI Chart
                updateChart(selectedAsset.getCurrentPrice());

                // Real-time PnL from RiskManager
                double pnl = RiskManager.calculatePnL(selectedAsset.getCurrentPrice(), initialEntryPrice);
                lblPnL.setText(String.format("%.2f %%", pnl));
                lblPnL.setStyle("-fx-text-fill: " + RiskManager.getPnLColor(pnl) + ";");

                // Real-time Total from TradingEngine
                updatePreview();
            }
        }));
        engine.setCycleCount(Animation.INDEFINITE);
        engine.play();
    }

    private void updatePreview() {
        try {
            double q = Double.parseDouble(txtQty.getText());
            double total = TradingEngine.calculateTotal(q, selectedAsset.getCurrentPrice());
            lblTotal.setText(String.format("Total (incl. Fee): %.2f USDT", total));
        } catch (Exception e) { lblTotal.setText("Total: 0.00 USDT"); }
    }

    @FXML
    private void handleAction(typeTransaction type) {
        if (selectedAsset == null || txtQty.getText().isEmpty()) return;

        try {
            double qty = Double.parseDouble(txtQty.getText());
            double finalAmount = (type == typeTransaction.ACHAT)
                    ? TradingEngine.calculateTotal(qty, selectedAsset.getCurrentPrice())
                    : TradingEngine.calculateSaleGain(qty, selectedAsset.getCurrentPrice());

            transaction t = new transaction();
            t.setIdWalletSource(type == typeTransaction.ACHAT ? MY_WALLET_ID : MARKET_WALLET_ID);
            t.setIdWalletDestination(type == typeTransaction.ACHAT ? MARKET_WALLET_ID : MY_WALLET_ID);
            t.setMontant(finalAmount);
            t.setType(type);
            t.setCurrencyId(USDT_ID);
            t.setStatut(StatutTransaction.Completed);
            t.setDateTransaction(LocalDateTime.now());

            transService.insertOne(t); // Persistence via le service de votre collègue
            loadBalance();
            showNotification("Success", "Transaction recorded in DB!");
        } catch (Exception e) {
            showNotification("Error", e.getMessage());
        }
    }

    private void setupTable() {
        colSymbol.setCellValueFactory(new PropertyValueFactory<>("symbol"));
        colPrice.setCellValueFactory(new PropertyValueFactory<>("currentPrice"));
        try { tableAssets.getItems().setAll(assetService.SelectAll()); } catch (Exception e) {}
    }

    private void loadBalance() {
        try {
            wallet_currency wc = wcService.getWalletCurrencyByWalletAndId(MY_WALLET_ID, USDT_ID);
            if (wc != null) {
                lblBalance.setText(String.format("%.2f USDT", wc.getSolde()));
                lblBalanceTND.setText(String.format("≈ %.2f TND", wc.getSolde() * 3.25));
            }
        } catch (SQLException e) {}
    }

    private void updateChart(double p) {
        series.getData().add(new XYChart.Data<>(LocalDateTime.now().toString().substring(11, 19), p));
        if (series.getData().size() > 10) series.getData().remove(0);
    }

    @FXML private void onBuy() { handleAction(typeTransaction.ACHAT); }
    @FXML private void onSell() { handleAction(typeTransaction.VENTE); }

    private void showNotification(String t, String c) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(t); a.setContentText(c); a.show();
    }
}