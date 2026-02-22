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
import java.time.format.DateTimeFormatter;
import java.util.List;

public class TradingDashboard {

    @FXML private TableView<Asset> tableAssets;
    @FXML private TableColumn<Asset, String> colSymbol;
    @FXML private TableColumn<Asset, Double> colPrice;
    @FXML private Label lblBalance, lblBalanceTND, lblSelected, lblTotal, lblEquity, lblPnL;
    @FXML private TextField txtQty;
    @FXML private LineChart<String, Number> priceChart;

    private XYChart.Series<String, Number> series = new XYChart.Series<>();

    // Services
    private TransactionService transService = new TransactionService();
    private wallet_currencyService wcService = new wallet_currencyService();
    private WalletService walletService = new WalletService();
    private AssetService assetService = new AssetService();
    private MarketApiService marketApi = new MarketApiService();

    private Asset selectedAsset;
    private wallet userWallet;
    private wallet marketWallet;

    // Variables pour le calcul du PnL
    private double initialEntryPrice = 0.0;
    private final int USDT_ID = 1;

    @FXML
    public void initialize() {
        setupTable();
        priceChart.getData().add(series);

        // 1. Load data from DB
        loadWalletsFromDatabase();
        refreshWalletUI();

        // 2. Start Real-time Engine
        startLiveEngine();

        // 3. Asset Selection Listener
        tableAssets.getSelectionModel().selectedItemProperty().addListener((obs, old, newVal) -> {
            if (newVal != null) {
                selectedAsset = newVal;
                initialEntryPrice = newVal.getCurrentPrice(); // Simule un prix d'entrée
                series.getData().clear();
                lblSelected.setText(newVal.getSymbol().toUpperCase() + " / USDT");
                updateOrderPreview();
            }
        });

        txtQty.textProperty().addListener((obs, old, newVal) -> updateOrderPreview());
    }

    private void loadWalletsFromDatabase() {
        try {
            List<wallet> allWallets = walletService.SelectAll();
            for (wallet w : allWallets) {
                if (w.getIdWallet() == 3) userWallet = w;
                if (w.getIdWallet() == 4) marketWallet = w;
            }
        } catch (SQLException e) {
            showNotification("DB Error", "Error loading wallets: " + e.getMessage());
        }
    }

    private void refreshWalletUI() {
        if (userWallet == null) return;
        try {
            wallet_currency wc = wcService.getWalletCurrencyByWalletAndId(userWallet.getIdWallet(), USDT_ID);
            if (wc != null) {
                double solde = wc.getSolde();
                lblBalance.setText(String.format("%.2f USDT", solde));
                lblBalanceTND.setText(String.format("≈ %.3f TND", solde * 3.25));
                lblEquity.setText(String.format("%.2f USDT", solde));
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    /**
     * Logic for Real-time PnL Calculation
     */
    private void calculatePnL() {
        if (selectedAsset != null && initialEntryPrice > 0) {
            double currentPrice = selectedAsset.getCurrentPrice();
            double pnlPercent = ((currentPrice - initialEntryPrice) / initialEntryPrice) * 100;

            lblPnL.setText(String.format("%.2f %%", pnlPercent));

            // Color coding: Green for profit, Red for loss
            if (pnlPercent >= 0) {
                lblPnL.setStyle("-fx-text-fill: #00ff88; -fx-font-weight: bold;");
            } else {
                lblPnL.setStyle("-fx-text-fill: #f23645; -fx-font-weight: bold;");
            }
        }
    }

    @FXML
    private void handleTrade(typeTransaction type) {
        if (selectedAsset == null || txtQty.getText().isEmpty() || userWallet == null) {
            showNotification("Warning", "Please select an asset and quantity.");
            return;
        }

        try {
            double totalCost = Double.parseDouble(txtQty.getText()) * selectedAsset.getCurrentPrice();
            transaction t = new transaction();

            // Respecting the colleague's Service constraints
            if (type == typeTransaction.ACHAT) {
                t.setIdWalletSource(userWallet.getIdWallet());
                t.setIdWalletDestination(marketWallet.getIdWallet());
                t.setType(typeTransaction.ACHAT);
            } else {
                t.setIdWalletSource(marketWallet.getIdWallet());
                t.setIdWalletDestination(userWallet.getIdWallet());
                t.setType(typeTransaction.VENTE);
            }

            t.setMontant(totalCost);
            t.setCurrencyId(USDT_ID);
            t.setStatut(StatutTransaction.Completed);
            t.setDateTransaction(LocalDateTime.now());

            // EXECUTION & DB RECORDING
            transService.insertOne(t);

            refreshWalletUI();
            showNotification("Trade Successful", "Order registered in Database!");

        } catch (Exception e) {
            showNotification("Transaction Error", e.getMessage());
        }
    }

    private void startLiveEngine() {
        Timeline engine = new Timeline(new KeyFrame(Duration.seconds(2), e -> {
            tableAssets.getItems().forEach(asset -> {
                double livePrice = marketApi.fetchPrice(asset.getSymbol());
                asset.setCurrentPrice(livePrice);
            });
            tableAssets.refresh();

            if (selectedAsset != null) {
                updateChart(selectedAsset.getCurrentPrice());
                calculatePnL(); // Update PnL every 2 seconds
                updateOrderPreview();
            }
        }));
        engine.setCycleCount(Animation.INDEFINITE);
        engine.play();
    }

    private void setupTable() {
        colSymbol.setCellValueFactory(new PropertyValueFactory<>("symbol"));
        colPrice.setCellValueFactory(new PropertyValueFactory<>("currentPrice"));
        try { tableAssets.getItems().setAll(assetService.SelectAll()); } catch (Exception e) { e.printStackTrace(); }
    }

    private void updateChart(double price) {
        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        series.getData().add(new XYChart.Data<>(time, price));
        if (series.getData().size() > 15) series.getData().remove(0);
    }

    private void updateOrderPreview() {
        try {
            double q = Double.parseDouble(txtQty.getText());
            lblTotal.setText(String.format("Total: %.2f USDT", q * selectedAsset.getCurrentPrice()));
        } catch (Exception e) { lblTotal.setText("Total: 0.00 USDT"); }
    }

    @FXML private void onBuyAction() { handleTrade(typeTransaction.ACHAT); }
    @FXML private void onSellAction() { handleTrade(typeTransaction.VENTE); }

    private void showNotification(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}