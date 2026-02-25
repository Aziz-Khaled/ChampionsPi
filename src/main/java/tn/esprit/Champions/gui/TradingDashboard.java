package tn.esprit.Champions.gui;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;
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
import java.util.Map;
import java.util.stream.Collectors;

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
    @FXML private VBox paneHistory, vboxNews;
    @FXML private Pane paneMarket;

    @FXML private WebView chartWebView;

    // Services
    private final TradeService tradeService = new TradeService();
    private final MarketApiService marketApi = new MarketApiService();
    private final wallet_currencyService wcService = new wallet_currencyService();
    private final AssetService assetService = new AssetService();
    private final TransactionService transService = new TransactionService();
    private final NewsService newsService = new NewsService();

    // Session Data
    private Asset selectedAsset;
    private double initialEntryPrice = 0.0;
    private final double TND_RATE = 3.12;
    private final int MY_WALLET_ID = 3;
    private final int MARKET_WALLET_ID = 4;
    private final int USDT_ID = 1;
    private final int CURRENT_USER_ID = 1;
    private Map<Integer, String> assetNamesCache;

    @FXML
    public void initialize() {
        loadAssetCache();
        setupTables();
        setupOrderInputs();

        // Listener de sélection d'actif
        tableAssets.getSelectionModel().selectedItemProperty().addListener((obs, old, newVal) -> {
            if (newVal != null) {
                selectedAsset = newVal;
                initialEntryPrice = newVal.getCurrentPrice();
                lblSelected.setText(newVal.getSymbol().toUpperCase() + " / USDT");

                // 1. Mise à jour du graphique (Option A)
                updateTradingView(newVal.getSymbol());

                // 2. Mise à jour des News (Correction API)
                updateNews(newVal.getSymbol());

                // 3. Autres services
                startLiveStreaming(newVal.getSymbol());
                updateAISignal(newVal.getSymbol());
                calculateTotal();
            }
        });

        txtQty.textProperty().addListener((obs, old, newVal) -> calculateTotal());
        txtTargetPrice.textProperty().addListener((obs, old, newVal) -> calculateTotal());
        comboOrderMode.valueProperty().addListener((obs, old, newVal) -> calculateTotal());

        updateBalances();
    }

    /**
     * Correction de l'API News : Chargement asynchrone pour ne pas freeze l'UI
     */
    private void updateNews(String symbol) {
        new Thread(() -> {
            try {
                // On nettoie le symbole (ex: BTCUSDT -> BTC)
                String querySymbol = symbol.replace("USDT", "").replace("usdt", "");
                List<News> news = newsService.getLatestNews(querySymbol);

                Platform.runLater(() -> {
                    vboxNews.getChildren().clear();
                    if (news == null || news.isEmpty()) {
                        Label emptyLabel = new Label("No news found for " + querySymbol);
                        emptyLabel.setStyle("-fx-text-fill: #848e9c; -fx-padding: 10;");
                        vboxNews.getChildren().add(emptyLabel);
                    } else {
                        news.forEach(n -> {
                            Label l = new Label("• " + n.getTitle());
                            l.setWrapText(true);
                            l.setStyle("-fx-text-fill: white; -fx-padding: 5; -fx-border-color: #2b3139; -fx-border-width: 0 0 1 0;");
                            l.setMaxWidth(230); // Éviter que le texte ne dépasse de la sidebar
                            vboxNews.getChildren().add(l);
                        });
                    }
                });
            } catch (Exception e) {
                System.err.println("News API Error: " + e.getMessage());
            }
        }).start();
    }

    private void updateTradingView(String symbol) {
        String pair = symbol.toUpperCase();
        if (!pair.contains("USDT")) pair += "USDT";

        String url = "https://s.tradingview.com/widgetembed/?frameElementId=tradingview_76d4d" +
                "&symbol=BINANCE:" + pair +
                "&interval=D&hidesidetoolbar=1&hidetoptoolbar=1&symboledit=1&saveimage=1" +
                "&toolbarbg=161a1e&theme=dark&style=1&timezone=Etc%2FUTC" +
                "&studies=[{\"id\":\"MAExp@tv-basicstudies\",\"inputs\":{\"length\":7}}," +
                "{\"id\":\"MAExp@tv-basicstudies\",\"inputs\":{\"length\":25}}]";

        Platform.runLater(() -> chartWebView.getEngine().load(url));
    }

    private void loadAssetCache() {
        try {
            assetNamesCache = assetService.SelectAll().stream()
                    .collect(Collectors.toMap(Asset::getId, Asset::getSymbol));
        } catch (SQLException e) {
            System.err.println("Erreur cache actifs: " + e.getMessage());
        }
    }

    private void calculateTotal() {
        if (selectedAsset == null) return;
        try {
            String qtyStr = txtQty.getText().replace(",", ".");
            if (qtyStr.isEmpty()) { lblTotal.setText("0.00 USDT"); return; }
            double qty = Double.parseDouble(qtyStr);
            double price = "LIMIT".equals(comboOrderMode.getValue()) ?
                    Double.parseDouble(txtTargetPrice.getText().replace(",", ".")) :
                    selectedAsset.getCurrentPrice();

            double total = qty * price;
            lblTotal.setText(String.format("%.2f USDT", total));
            double usdtBalance = wcService.getBalance(MY_WALLET_ID, USDT_ID);
            lblTotal.setStyle(total > usdtBalance ? "-fx-text-fill: #f6465d;" : "-fx-text-fill: #fcd535;");
        } catch (Exception e) { lblTotal.setText("0.00 USDT"); }
    }

    private void startLiveStreaming(String symbol) {
        marketApi.startPriceStream(symbol, (Double livePrice) -> {
            Platform.runLater(() -> {
                if (selectedAsset != null && selectedAsset.getSymbol().equalsIgnoreCase(symbol)) {
                    selectedAsset.setCurrentPrice(livePrice);
                    tableAssets.refresh();
                    updatePnL(livePrice);
                    if ("MARKET".equals(comboOrderMode.getValue())) calculateTotal();
                }
            });
        });
    }

    @FXML private void onBuy() { processTrade(TradeType.BUY); }
    @FXML private void onSell() { processTrade(TradeType.SELL); }

    private void processTrade(TradeType type) {
        if (selectedAsset == null || txtQty.getText().isEmpty()) return;
        try {
            double qty = Double.parseDouble(txtQty.getText().replace(",", "."));
            OrderMode mode = OrderMode.valueOf(comboOrderMode.getValue());
            double price = mode == OrderMode.MARKET ? selectedAsset.getCurrentPrice() : Double.parseDouble(txtTargetPrice.getText());
            Trade t = new Trade(0, CURRENT_USER_ID, selectedAsset.getId(), type, mode, price, qty,
                    (mode == OrderMode.MARKET ? Status.COMPLETED : Status.PENDING), LocalDateTime.now(), null);
            tradeService.insertOne(t);
            if (mode == OrderMode.MARKET) saveFinancialTransaction(qty * price, type);
            showAlert("Success", "Order executed.");
            updateBalances();
        } catch (Exception e) { showAlert("Error", e.getMessage()); }
    }

    private void saveFinancialTransaction(double amount, TradeType type) throws SQLException {
        transaction t = new transaction();
        t.setIdWalletSource(type == TradeType.BUY ? MY_WALLET_ID : MARKET_WALLET_ID);
        t.setIdWalletDestination(type == TradeType.BUY ? MARKET_WALLET_ID : MY_WALLET_ID);
        t.setMontant(amount);
        t.setType(type == TradeType.BUY ? typeTransaction.ACHAT : typeTransaction.VENTE);
        t.setCurrencyId(USDT_ID);
        t.setStatut(StatutTransaction.Completed);
        t.setDateTransaction(LocalDateTime.now());
        transService.insertOne(t);
    }

    private void updatePnL(double currentPrice) {
        if (initialEntryPrice <= 0) return;
        double pnl = (currentPrice - initialEntryPrice) / initialEntryPrice * 100;
        lblPnL.setText(String.format("%+.2f%%", pnl));
        lblPnL.setStyle("-fx-text-fill: " + (pnl >= 0 ? "#0ecb81" : "#f6465d") + ";");
    }

    private void updateBalances() {
        try {
            double usdt = wcService.getBalance(MY_WALLET_ID, USDT_ID);
            lblBalance.setText(String.format("%.2f USDT", usdt));
            lblBalanceTND.setText(String.format("≈ %.3f TND", usdt * TND_RATE));
        } catch (SQLException e) {}
    }

    private void setupTables() {
        colSymbol.setCellValueFactory(new PropertyValueFactory<>("symbol"));
        colPrice.setCellValueFactory(new PropertyValueFactory<>("currentPrice"));
        colHistSymbol.setCellValueFactory(cellData -> new SimpleStringProperty(assetNamesCache.getOrDefault(cellData.getValue().getAsset_id(), "Unknown")));
        colHistQty.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        colHistPrice.setCellValueFactory(new PropertyValueFactory<>("price"));
        colHistType.setCellValueFactory(new PropertyValueFactory<>("tradeType"));
        colHistStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        try { tableAssets.getItems().setAll(assetService.SelectAll()); } catch (SQLException e) {}
    }

    private void setupOrderInputs() {
        comboOrderMode.getItems().setAll("MARKET", "LIMIT");
        comboOrderMode.setValue("MARKET");
        txtTargetPrice.disableProperty().bind(comboOrderMode.valueProperty().isEqualTo("MARKET"));
    }

    @FXML private void showMarket() { paneMarket.setVisible(true); paneHistory.setVisible(false); }
    @FXML private void showHistory() { paneMarket.setVisible(false); paneHistory.setVisible(true); loadTradeHistory(); }

    private void loadTradeHistory() {
        try { tableHistory.getItems().setAll(tradeService.SelectAll()); } catch (SQLException e) {}
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

    private void showAlert(String title, String content) {
        Alert a = new Alert(Alert.AlertType.INFORMATION); a.setTitle(title); a.setContentText(content); a.show();
    }

    @FXML
    private void openBotWindow() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/BotView.fxml"));
            Stage s = new Stage(); s.setScene(new Scene(root)); s.setTitle("AI Trading Bot"); s.show();
        } catch (IOException e) { e.printStackTrace(); }
    }
    @FXML
    private void onSuggestQuantity() {
        try {
            if (selectedAsset == null) {
                showAlert("Selection Required", "Please select an asset first.");
                return;
            }

            double balance = wcService.getBalance(MY_WALLET_ID, USDT_ID);
            String targetStr = txtTargetPrice.getText().replace(",", ".");

            if (targetStr.isEmpty()) {
                showAlert("Input Error", "Please enter a Target/Stop Loss price for calculation.");
                return;
            }

            double stopLoss = Double.parseDouble(targetStr);
            double currentPrice = selectedAsset.getCurrentPrice();

            // Calcul suggéré (Risk 1% par trade)
            double suggestedQty = (balance * 0.01) / Math.abs(currentPrice - stopLoss);

            txtQty.setText(String.format("%.6f", suggestedQty));
            calculateTotal();

        } catch (Exception e) {
            showAlert("Error", "Check your Stop Loss (Target Price) value format.");
        }
    }
}