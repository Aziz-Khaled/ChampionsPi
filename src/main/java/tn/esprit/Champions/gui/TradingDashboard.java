package tn.esprit.Champions.gui;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import javafx.util.Duration;
import tn.esprit.Champions.models.*;
import tn.esprit.Champions.services.*;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
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
    @FXML private VBox paneHistory, vboxNews, paneMarket;
    @FXML private WebView chartWebView;
    @FXML private HBox tickerContainer;

    // Services
    private final TradeService tradeService = new TradeService();
    private final MarketApiService marketApi = new MarketApiService();
    private final wallet_currencyService wcService = new wallet_currencyService();
    private final AssetService assetService = new AssetService();
    private final TransactionService transService = new TransactionService();
    private final NewsService newsService = new NewsService();

    // Session & Real-time Data
    private Asset selectedAsset;
    private double initialEntryPrice = 0.0;
    private final double TND_RATE = 3.12;
    private final int MY_WALLET_ID = 3;
    private final int USDT_ID = 1;
    private final int CURRENT_USER_ID = 1;
    private Map<Integer, String> assetNamesCache;

    // Map pour stocker les labels du ticker et les mettre à jour en temps réel
    private final Map<String, List<Label>> tickerPriceLabels = new HashMap<>();

    @FXML
    public void initialize() {
        loadAssetCache();
        setupTables();
        setupOrderInputs();
        setupTickerLoop(); // Initialise le bandeau défilant infini

        // Listener de sélection d'actif
        tableAssets.getSelectionModel().selectedItemProperty().addListener((obs, old, newVal) -> {
            if (newVal != null) {
                selectedAsset = newVal;
                initialEntryPrice = newVal.getCurrentPrice();
                lblSelected.setText(newVal.getSymbol().toUpperCase() + " / USDT");
                updateTradingView(newVal.getSymbol());
                updateNews(newVal.getSymbol());
                updateAISignal(newVal.getSymbol());
                calculateTotal();
            }
        });

        // Listeners pour calcul auto
        txtQty.textProperty().addListener((obs, old, newVal) -> calculateTotal());
        txtTargetPrice.textProperty().addListener((obs, old, newVal) -> calculateTotal());
        comboOrderMode.valueProperty().addListener((obs, old, newVal) -> calculateTotal());

        updateBalances();
        startGlobalPriceUpdates(); // Lance le monitoring de tous les prix
    }

    // --- LOGIQUE DU TICKER INFINI ET TEMPS RÉEL ---

    private void setupTickerLoop() {
        try {
            List<Asset> assets = assetService.SelectAll();
            tickerContainer.getChildren().clear();
            tickerPriceLabels.clear();

            // On ajoute les éléments DEUX FOIS pour créer l'effet de boucle infinie sans saut
            for (int i = 0; i < 2; i++) {
                for (Asset a : assets) {
                    HBox item = new HBox(10);
                    item.setAlignment(Pos.CENTER_LEFT);

                    Label sym = new Label(a.getSymbol().toUpperCase());
                    sym.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 11;");

                    Label prc = new Label(String.format("%.2f", a.getCurrentPrice()));
                    prc.setStyle("-fx-text-fill: #eaecef; -fx-font-size: 11;");

                    // Enregistrement des labels pour mise à jour dynamique
                    tickerPriceLabels.computeIfAbsent(a.getSymbol().toLowerCase(), k -> new ArrayList<>()).add(prc);

                    double mockChange = (Math.random() * 4) - 2;
                    Label pct = new Label(String.format("%+.2f%%", mockChange));
                    pct.setStyle("-fx-text-fill: " + (mockChange >= 0 ? "#0ecb81;" : "#f6465d;") + "; -fx-font-size: 11;");

                    item.getChildren().addAll(sym, prc, pct, new Label("  |  "));
                    tickerContainer.getChildren().add(item);
                }
            }

            // Animation de défilement
            Timeline scrollAnim = new Timeline(new KeyFrame(Duration.millis(25), e -> {
                tickerContainer.setTranslateX(tickerContainer.getTranslateX() - 1);
                // Si la première moitié est passée, on reset à 0 instantanément
                if (Math.abs(tickerContainer.getTranslateX()) >= (tickerContainer.getWidth() / 2)) {
                    tickerContainer.setTranslateX(0);
                }
            }));
            scrollAnim.setCycleCount(Animation.INDEFINITE);
            scrollAnim.play();

        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void startGlobalPriceUpdates() {
        try {
            List<Asset> assets = assetService.SelectAll();
            for (Asset a : assets) {
                marketApi.startPriceStream(a.getSymbol(), (Double livePrice) -> {
                    Platform.runLater(() -> {
                        // 1. Update Ticker (Boucle infinie)
                        List<Label> labels = tickerPriceLabels.get(a.getSymbol().toLowerCase());
                        if (labels != null) {
                            labels.forEach(l -> l.setText(String.format("%.2f", livePrice)));
                        }

                        // 2. Update Dashboard si sélectionné
                        if (selectedAsset != null && selectedAsset.getSymbol().equalsIgnoreCase(a.getSymbol())) {
                            selectedAsset.setCurrentPrice(livePrice);
                            tableAssets.refresh();
                            updatePnL(livePrice);
                            calculateTotal();
                        }
                    });
                });
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    // --- TRADING & CALCULS ---

    private void calculateTotal() {
        if (selectedAsset == null || lblTotal == null) return;
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

    @FXML private void onBuy() { processTrade(TradeType.BUY); }
    @FXML private void onSell() { processTrade(TradeType.SELL); }

    private void processTrade(TradeType type) {
        if (selectedAsset == null || txtQty.getText().isEmpty()) return;
        try {
            double qty = Double.parseDouble(txtQty.getText().replace(",", "."));
            OrderMode mode = OrderMode.valueOf(comboOrderMode.getValue());
            double price = (mode == OrderMode.MARKET) ? selectedAsset.getCurrentPrice() : Double.parseDouble(txtTargetPrice.getText());

            Trade t = new Trade(0, CURRENT_USER_ID, selectedAsset.getId(), type, mode, price, qty,
                    (mode == OrderMode.MARKET ? Status.COMPLETED : Status.PENDING), LocalDateTime.now(), null);

            tradeService.insertOne(t);
            updateBalances();
            showAlert("Order Success", "Executed " + type + " for " + selectedAsset.getSymbol());
        } catch (Exception e) { showAlert("Error", e.getMessage()); }
    }

    // --- UI HELPERS ---

    private void updateBalances() {
        try {
            double usdt = wcService.getBalance(MY_WALLET_ID, USDT_ID);
            lblBalance.setText(String.format("%.2f USDT", usdt));
            lblBalanceTND.setText(String.format("≈ %.3f TND", usdt * TND_RATE));
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void updatePnL(double currentPrice) {
        if (initialEntryPrice <= 0) return;
        double pnl = (currentPrice - initialEntryPrice) / initialEntryPrice * 100;
        lblPnL.setText(String.format("%+.2f%%", pnl));
        lblPnL.setStyle("-fx-text-fill: " + (pnl >= 0 ? "#0ecb81" : "#f6465d") + ";");
    }

    private void setupTables() {
        colSymbol.setCellValueFactory(new PropertyValueFactory<>("symbol"));
        colPrice.setCellValueFactory(new PropertyValueFactory<>("currentPrice"));
        colHistSymbol.setCellValueFactory(d -> new SimpleStringProperty(assetNamesCache.getOrDefault(d.getValue().getAsset_id(), "Unknown")));
        colHistQty.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        colHistPrice.setCellValueFactory(new PropertyValueFactory<>("price"));
        colHistType.setCellValueFactory(new PropertyValueFactory<>("tradeType"));
        colHistStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        try { tableAssets.getItems().setAll(assetService.SelectAll()); } catch (Exception e) {}
    }

    private void loadAssetCache() {
        try {
            assetNamesCache = assetService.SelectAll().stream()
                    .collect(Collectors.toMap(Asset::getId, Asset::getSymbol));
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void setupOrderInputs() {
        comboOrderMode.getItems().setAll("MARKET", "LIMIT");
        comboOrderMode.setValue("MARKET");
        txtTargetPrice.disableProperty().bind(comboOrderMode.valueProperty().isEqualTo("MARKET"));
    }

    private void updateTradingView(String symbol) {
        String pair = symbol.toUpperCase().endsWith("USDT") ? symbol.toUpperCase() : symbol.toUpperCase() + "USDT";
        Platform.runLater(() -> chartWebView.getEngine().load("https://s.tradingview.com/widgetembed/?symbol=BINANCE:" + pair + "&theme=dark"));
    }

    private void updateNews(String symbol) {
        new Thread(() -> {
            try {
                List<News> news = newsService.getLatestNews(symbol.replace("USDT", ""));
                Platform.runLater(() -> {
                    vboxNews.getChildren().clear();
                    news.forEach(n -> {
                        Label l = new Label("• " + n.getTitle());
                        l.setStyle("-fx-text-fill: white; -fx-padding: 5;");
                        l.setWrapText(true);
                        vboxNews.getChildren().add(l);
                    });
                });
            } catch (Exception e) {}
        }).start();
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

    @FXML private void showMarket() { paneMarket.setVisible(true); paneHistory.setVisible(false); }
    @FXML private void showHistory() { paneMarket.setVisible(false); paneHistory.setVisible(true); loadTradeHistory(); }
    private void loadTradeHistory() { try { tableHistory.getItems().setAll(tradeService.SelectAll()); } catch (SQLException e) {} }

    @FXML private void onSuggestQuantity() {
        if (selectedAsset == null) return;
        try {
            double bal = wcService.getBalance(MY_WALLET_ID, USDT_ID);
            txtQty.setText(String.format("%.4f", (bal * 0.1) / selectedAsset.getCurrentPrice()));
        } catch (Exception e) {}
    }

    private void showAlert(String title, String content) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(title); a.setHeaderText(null); a.setContentText(content); a.show();
    }

    @FXML private void openBotWindow() {  try {
        Parent root = FXMLLoader.load(getClass().getResource("/BotView.fxml"));
        Stage s = new Stage(); s.setScene(new Scene(root)); s.setTitle("AI Trading Bot"); s.show();
    } catch (IOException e) { e.printStackTrace(); }}
}