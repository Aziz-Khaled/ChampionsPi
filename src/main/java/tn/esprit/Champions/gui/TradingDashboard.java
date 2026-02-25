package tn.esprit.Champions.gui;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import tn.esprit.Champions.models.*;
import tn.esprit.Champions.services.*;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
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
    @FXML private VBox  paneHistory;
    @FXML private Pane paneMarket;
    @FXML private VBox vboxNews;

    private XYChart.Series<String, Number> series = new XYChart.Series<>();

    // Services
    private final TradeService tradeService = new TradeService();
    private final MarketApiService marketApi = new MarketApiService();
    private final wallet_currencyService wcService = new wallet_currencyService();
    private final AssetService assetService = new AssetService();
    private final TransactionService transService = new TransactionService();
    private final NewsService newsService = new NewsService();

    // Configuration
    private Asset selectedAsset;
    private double initialEntryPrice = 0.0;
    private final double TND_RATE = 3.12;
    private final int MY_WALLET_ID = 3;
    private final int MARKET_WALLET_ID = 4;
    private final int USDT_ID = 1;
    private final int CURRENT_USER_ID = 1;

    @FXML
    public void initialize() {
        setupTables();
        setupChart();
        setupOrderInputs();

        tableAssets.getSelectionModel().selectedItemProperty().addListener((obs, old, newVal) -> {
            if (newVal != null) {
                selectedAsset = newVal;
                // Important : On fixe le prix d'entrée au moment du clic pour éviter le PnL géant
                initialEntryPrice = newVal.getCurrentPrice();
                lblSelected.setText(newVal.getSymbol().toUpperCase() + " / USDT");
                series.getData().clear();

                startLiveStreaming(newVal.getSymbol());
                updateAISignal(newVal.getSymbol());
                updateNews(newVal.getSymbol());
            }
        });

        txtQty.textProperty().addListener((obs, old, newVal) -> calculateTotal());
        updateBalances();
    }

    private void updateNews(String symbol) {
        new Thread(() -> {
            String cleanSymbol = symbol.toUpperCase().replace("USDT", "");
            List<News> newsList = newsService.getLatestNews(cleanSymbol);

            Platform.runLater(() -> {
                if (vboxNews != null) {
                    vboxNews.getChildren().clear();
                    for (News n : newsList) {
                        Label newsLabel = new Label("• " + n.getTitle());
                        newsLabel.setWrapText(true);
                        newsLabel.setPrefWidth(250);
                        if ("bullish".equals(n.getSentiment())) newsLabel.setStyle("-fx-text-fill: #2ecc71;");
                        else if ("bearish".equals(n.getSentiment())) newsLabel.setStyle("-fx-text-fill: #e74c3c;");
                        vboxNews.getChildren().add(newsLabel);
                    }
                }
            });
        }).start();
    }

    private void startLiveStreaming(String symbol) {
        marketApi.startPriceStream(symbol, (Double livePrice) -> {
            Platform.runLater(() -> {
                if (selectedAsset != null && selectedAsset.getSymbol().equalsIgnoreCase(symbol)) {
                    selectedAsset.setCurrentPrice(livePrice);
                    tableAssets.refresh();
                    updateLiveChart(livePrice);
                    updatePnL(livePrice);
                    calculateTotal();
                }
            });
        });
    }

    /**
     * MÉTIER AVANCÉ : Calcul du PnL via RiskManager
     */
    private void updatePnL(double currentPrice) {
        if (initialEntryPrice <= 0) {
            lblPnL.setText("0.00%");
            lblPnL.setStyle("-fx-text-fill: white;");
            return;
        }
        double pnl = RiskManager.calculatePnL(currentPrice, initialEntryPrice);
        lblPnL.setText(String.format("%+.2f%%", pnl));
        lblPnL.setStyle("-fx-text-fill: " + RiskManager.getPnLColor(pnl) + ";");
    }

    /**
     * MÉTIER AVANCÉ : Suggestion de quantité via PositionSizer
     */
    @FXML
    private void onSuggestQuantity() {
        try {
            double usdtBalance = wcService.getBalance(MY_WALLET_ID, USDT_ID);
            double stopLoss = Double.parseDouble(txtTargetPrice.getText());

            if (selectedAsset == null || stopLoss >= selectedAsset.getCurrentPrice()) {
                showAlert("Error", "Stop Loss must be lower than current price for a BUY suggestion.");
                return;
            }

            double suggestedQty = PositionSizer.calculateRecommendedQty(
                    usdtBalance,
                    0.01, // Risque 1% du capital
                    selectedAsset.getCurrentPrice(),
                    stopLoss
            );

            txtQty.setText(String.format("%.4f", suggestedQty));
        } catch (Exception e) {
            showAlert("Error", "Check your Target Price (Stop Loss) value.");
        }
    }

    private void calculateTotal() {
        if (selectedAsset != null && !txtQty.getText().isEmpty()) {
            try {
                double qty = Double.parseDouble(txtQty.getText().replace(",", "."));
                // TradingEngine supprimé : calcul direct
                lblTotal.setText(String.format("%.2f USDT", qty * selectedAsset.getCurrentPrice()));
            } catch (Exception e) { }
        }
    }

    private void processTrade(TradeType type) {
        if (selectedAsset == null || txtQty.getText().isEmpty()) return;

        try {
            double qty = Double.parseDouble(txtQty.getText().replace(",", "."));
            OrderMode mode = OrderMode.valueOf(comboOrderMode.getValue());
            double price = mode == OrderMode.MARKET ? selectedAsset.getCurrentPrice() : Double.parseDouble(txtTargetPrice.getText());
            double totalAmount = qty * price;

            Trade trade = new Trade(0, CURRENT_USER_ID, selectedAsset.getId(), type, mode, price, qty,
                    (mode == OrderMode.MARKET ? Status.COMPLETED : Status.PENDING),
                    LocalDateTime.now(), (mode == OrderMode.MARKET ? LocalDateTime.now() : null));

            tradeService.insertOne(trade);

            if (mode == OrderMode.MARKET) {
                transaction t = new transaction();
                t.setIdWalletSource(type == TradeType.BUY ? MY_WALLET_ID : MARKET_WALLET_ID);
                t.setIdWalletDestination(type == TradeType.BUY ? MARKET_WALLET_ID : MY_WALLET_ID);
                t.setMontant(totalAmount);
                t.setType(type == TradeType.BUY ? typeTransaction.ACHAT : typeTransaction.VENTE);
                t.setCurrencyId(USDT_ID);
                t.setStatut(StatutTransaction.Completed);
                t.setDateTransaction(LocalDateTime.now());
                transService.insertOne(t);
            }

            showAlert("Success", "Order processed.");
            updateBalances();
        } catch (Exception e) {
            showAlert("Error", e.getMessage());
        }
    }

    private void updateBalances() {
        try {
            double usdt = wcService.getBalance(MY_WALLET_ID, USDT_ID);
            lblBalance.setText(String.format("%.2f USDT", usdt));
            lblBalanceTND.setText(String.format("≈ %.3f TND", usdt * TND_RATE));
        } catch (SQLException e) { e.printStackTrace(); }
    }

    @FXML private void onBuy() { processTrade(TradeType.BUY); }
    @FXML private void onSell() { processTrade(TradeType.SELL); }

    private void setupOrderInputs() {
        comboOrderMode.getItems().setAll("MARKET", "LIMIT");
        comboOrderMode.setValue("MARKET");
        txtTargetPrice.disableProperty().bind(comboOrderMode.valueProperty().isEqualTo("MARKET"));
    }

    private void updateAISignal(String symbol) {
        new Thread(() -> {
            double rsi = marketApi.calculateRSI(symbol);
            Platform.runLater(() -> {
                String advice = (rsi < 30) ? "STRONG BUY" : (rsi > 70) ? "STRONG SELL" : "NEUTRAL";
                lblAdvice.setText("AI SIGNAL: " + advice + " (RSI: " + String.format("%.2f", rsi) + ")");
            });
        }).start();
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
        if (series.getData().size() > 30) series.getData().remove(0);
    }

    @FXML private void showMarket() { paneMarket.setVisible(true); paneHistory.setVisible(false); }
    @FXML private void showHistory() { paneMarket.setVisible(false); paneHistory.setVisible(true); loadTradeHistory(); }
    private void loadTradeHistory() { try { tableHistory.getItems().setAll(tradeService.SelectAll()); } catch (SQLException e) {} }
    private void showAlert(String title, String content) { Alert a = new Alert(Alert.AlertType.INFORMATION); a.setTitle(title); a.setContentText(content); a.show(); }

    @FXML
    private void openBotWindow() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/BotView.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle("Trading Bot AI");
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) { e.printStackTrace(); }
    }
}