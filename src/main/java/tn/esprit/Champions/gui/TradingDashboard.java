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
import java.util.logging.Level;
import java.util.logging.Logger;

public class TradingDashboard {

    private static final Logger LOGGER = Logger.getLogger(TradingDashboard.class.getName());

    // Constantes de Configuration
    private static final double USDT_TND_RATE = 3.12;
    private static final int MY_WALLET_ID = 3;
    private static final int MARKET_WALLET_ID = 4;
    private static final int USDT_ID = 1;
    private static final int CURRENT_USER_ID = 1;

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
    private Timeline mainEngine;

    // Services
    private final TradeService tradeService = new TradeService();
    private final MarketApiService marketApi = new MarketApiService();
    private final TechnicalAnalysisService techService = new TechnicalAnalysisService();
    private final wallet_currencyService wcService = new wallet_currencyService();
    private final AssetService assetService = new AssetService();
    private final TransactionService transService = new TransactionService();

    private Asset selectedAsset;
    private double initialEntryPrice = 0.0;

    @FXML
    public void initialize() {
        configureUI();
        setupTables();
        setupChart();
        startGlobalEngine();
    }

    private void configureUI() {
        if (comboOrderMode != null) {
            comboOrderMode.getItems().setAll("MARKET", "LIMIT");
            comboOrderMode.setValue("MARKET");
            txtTargetPrice.disableProperty().bind(comboOrderMode.valueProperty().isEqualTo("MARKET"));
        }

        txtQty.textProperty().addListener((obs, old, newVal) -> refreshTotalLabel());

        tableAssets.getSelectionModel().selectedItemProperty().addListener((obs, old, newVal) -> {
            if (newVal != null) handleAssetSelection(newVal);
        });
    }

    // --- NOUVELLE MÉTHODE (C'est elle qui manquait !) ---
    @FXML
    private void openBotWindow() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/BotView.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle("🤖 Champions Bot Engine");
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Impossible d'ouvrir la fenêtre du Bot", e);
            showAlert("Erreur", "Le fichier BotView.fxml est introuvable.");
        }
    }

    private void setupTables() {
        colSymbol.setCellValueFactory(new PropertyValueFactory<>("symbol"));
        colPrice.setCellValueFactory(new PropertyValueFactory<>("currentPrice"));

        colPrice.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(Double price, boolean empty) {
                super.updateItem(price, empty);
                setText(empty || price == null ? null : String.format("%.4f", price));
            }
        });

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

    private void handleAssetSelection(Asset asset) {
        selectedAsset = asset;
        initialEntryPrice = asset.getCurrentPrice();
        lblSelected.setText(asset.getSymbol().toUpperCase() + " / USDT");
        series.getData().clear();
        refreshTotalLabel();
        updateTechnicalAnalysis(asset.getSymbol());
    }

    private void startGlobalEngine() {
        mainEngine = new Timeline(new KeyFrame(Duration.seconds(2), e -> {
            updateMarketData();
            if (selectedAsset != null) updateSelectedAssetUI();
            loadBalance();
        }));
        mainEngine.setCycleCount(Animation.INDEFINITE);
        mainEngine.play();
    }

    private void updateMarketData() {
        tableAssets.getItems().forEach(a -> a.setCurrentPrice(marketApi.fetchPrice(a.getSymbol())));
        tableAssets.refresh();
    }

    private void updateSelectedAssetUI() {
        double livePrice = selectedAsset.getCurrentPrice();
        updateChart(livePrice);
        refreshTotalLabel();
        updatePnL(livePrice);
    }

    private void refreshTotalLabel() {
        try {
            double qty = Double.parseDouble(txtQty.getText().replace(",", "."));
            double price = (comboOrderMode.getValue().equals("LIMIT") && !txtTargetPrice.getText().isEmpty())
                    ? Double.parseDouble(txtTargetPrice.getText())
                    : selectedAsset.getCurrentPrice();
            lblTotal.setText(String.format("%.2f USDT", qty * price));
        } catch (Exception e) {
            lblTotal.setText("0.00 USDT");
        }
    }

    private void updatePnL(double currentPrice) {
        if (initialEntryPrice <= 0) return;
        double pnl = ((currentPrice - initialEntryPrice) / initialEntryPrice) * 100;
        lblPnL.setText(String.format("%+.2f %%", pnl));
        lblPnL.setStyle("-fx-text-fill: " + (pnl >= 0 ? "#00ff88" : "#f23645") + "; -fx-font-size: 24; -fx-font-weight: bold;");
    }

    private void updateTechnicalAnalysis(String symbol) {
        Platform.runLater(() -> {
            double rsi = techService.fetchRSI(symbol);
            lblAdvice.setText("RSI (14): " + String.format("%.2f", rsi) + " | " + techService.getAdvice(rsi));
        });
    }

    @FXML private void onBuy() { handleTrade(typeTransaction.ACHAT); }
    @FXML private void onSell() { handleTrade(typeTransaction.VENTE); }

    private void handleTrade(typeTransaction type) {
        if (selectedAsset == null || txtQty.getText().isEmpty()) {
            showAlert("Erreur", "Sélectionnez un actif et une quantité.");
            return;
        }
        try {
            double qty = Double.parseDouble(txtQty.getText());
            if ("LIMIT".equals(comboOrderMode.getValue())) {
                placeLimitOrder(type, qty, Double.parseDouble(txtTargetPrice.getText()));
            } else {
                executeMarketTrade(type, qty);
            }
        } catch (Exception e) {
            showAlert("Erreur", "Données invalides.");
        }
    }

    private void executeMarketTrade(typeTransaction type, double qty) throws SQLException {
        double price = selectedAsset.getCurrentPrice();
        double total = qty * price;
        TradeType tType = (type == typeTransaction.ACHAT) ? TradeType.BUY : TradeType.SELL;

        wcService.updateBalanceAfterTrade(MY_WALLET_ID, USDT_ID, total, tType);

        Trade trade = new Trade(CURRENT_USER_ID, selectedAsset.getId(), tType, price, qty, Status.COMPLETED);
        trade.setOrderMode(OrderMode.MARKET);
        trade.setCreatedAt(LocalDateTime.now());
        tradeService.insertOne(trade);

        saveTransactionRecord(type, total);

        Platform.runLater(() -> {
            loadBalance();
            loadTradeHistory();
            showAlert("Succès", "Ordre au marché exécuté !");
        });
    }

    private void placeLimitOrder(typeTransaction type, double qty, double target) throws SQLException {
        Trade limit = new Trade(CURRENT_USER_ID, selectedAsset.getId(),
                (type == typeTransaction.ACHAT ? TradeType.BUY : TradeType.SELL), target, qty, Status.PENDING);
        limit.setOrderMode(OrderMode.LIMIT);
        limit.setCreatedAt(LocalDateTime.now());
        tradeService.insertOne(limit);
        showAlert("Ordre Limite", "Placé à " + target + " USDT.");
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
            Platform.runLater(() -> {
                lblBalance.setText(String.format("%.2f USDT", usdt));
                lblBalanceTND.setText(String.format("%.2f TND", usdt * USDT_TND_RATE));
            });
        } catch (SQLException e) { LOGGER.warning("Balance introuvable"); }
    }

    public void loadTradeHistory() {
        try { tableHistory.getItems().setAll(tradeService.SelectAll()); } catch (SQLException e) { }
    }

    private void refreshAssetList() {
        try { tableAssets.getItems().setAll(assetService.SelectAll()); } catch (SQLException e) { }
    }

    private void updateChart(double price) {
        String time = LocalDateTime.now().toString().substring(11, 19);
        series.getData().add(new XYChart.Data<>(time, price));
        if (series.getData().size() > 20) series.getData().remove(0);
    }

    @FXML private void showMarket() { paneMarket.setVisible(true); paneHistory.setVisible(false); }
    @FXML private void showHistory() { paneMarket.setVisible(false); paneHistory.setVisible(true); loadTradeHistory(); }

    private void showAlert(String title, String content) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(content);
            alert.show();
        });
    }
}