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
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import javafx.util.Duration;
import tn.esprit.Champions.models.*;
import tn.esprit.Champions.services.*;

import java.io.IOException;
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
    private final NewsService newsService = new NewsService();

    // Cache et Session
    private final Map<String, Image> logoCache = new HashMap<>();
    private final Map<String, List<Label>> tickerPriceLabels = new HashMap<>();
    private Map<Integer, String> assetNamesCache;
    private Asset selectedAsset;
    private double initialEntryPrice = 0.0;
    private final int MY_WALLET_ID = 3;
    private final int USDT_ID = 1;

    @FXML
    public void initialize() {
        loadAssetCache();
        setupTables();
        setupOrderInputs();
        setupTickerLoop();

        // Listener de sélection
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
        startGlobalPriceUpdates();
    }

    private void setupTables() {
        // --- COLONNE SYMBOLE + LOGO ---
        colSymbol.setCellFactory(column -> new TableCell<>() {
            private final ImageView img = new ImageView();
            private final Label lbl = new Label();
            private final HBox box = new HBox(10, img, lbl);
            {
                box.setAlignment(Pos.CENTER_LEFT);
                img.setFitHeight(20); img.setFitWidth(20);
                img.setPreserveRatio(true);
            }
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); }
                else {
                    lbl.setText(item.toUpperCase());
                    lbl.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");
                    String sym = item.toLowerCase();
                    if (!logoCache.containsKey(sym)) {
                        String url = "https://raw.githubusercontent.com/spothq/cryptocurrency-icons/master/128/color/" + sym + ".png";
                        logoCache.put(sym, new Image(url, 20, 20, true, true, true));
                    }
                    img.setImage(logoCache.get(sym));
                    setGraphic(box);
                }
            }
        });

        // --- COLONNE PRIX (TOUT EN BLANC + PRECISION) ---
        colPrice.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Double price, boolean empty) {
                super.updateItem(price, empty);
                if (empty || price == null) { setText(null); }
                else {
                    // Gestion de la précision pour XRP, ESP et autres petits prix
                    if (price < 1.0) setText(String.format("%.5f", price));
                    else setText(String.format("%.2f", price));

                    // Style forcé en BLANC
                    setStyle("-fx-text-fill: white; -fx-alignment: CENTER-RIGHT; -fx-font-family: 'Monospaced'; -fx-font-weight: bold;");
                }
            }
        });

        colSymbol.setCellValueFactory(new PropertyValueFactory<>("symbol"));
        colPrice.setCellValueFactory(new PropertyValueFactory<>("currentPrice"));

        // Configuration Historique
        colHistSymbol.setCellValueFactory(d -> new SimpleStringProperty(assetNamesCache.getOrDefault(d.getValue().getAsset_id(), "Unknown")));
        colHistQty.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        colHistPrice.setCellValueFactory(new PropertyValueFactory<>("price"));
        colHistType.setCellValueFactory(new PropertyValueFactory<>("tradeType"));
        colHistStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        try { tableAssets.getItems().setAll(assetService.SelectAll()); } catch (Exception e) { e.printStackTrace(); }
    }

    private void startGlobalPriceUpdates() {
        // Regroupement des symboles pour éviter les erreurs de connexion WebSocket (Connection Reset)
        for (Asset asset : tableAssets.getItems()) {
            String symbol = asset.getSymbol().toUpperCase();
            if (!symbol.endsWith("USDT")) symbol += "USDT";

            marketApi.startPriceStream(symbol, (Double livePrice) -> {
                if (livePrice != null && livePrice > 0) {
                    Platform.runLater(() -> {
                        asset.setCurrentPrice(livePrice);
                        tableAssets.refresh(); // Met à jour toute la table en blanc

                        // Mise à jour du Ticker
                        List<Label> labels = tickerPriceLabels.get(asset.getSymbol().toLowerCase());
                        if (labels != null) {
                            labels.forEach(l -> l.setText(livePrice < 1.0 ? String.format("%.5f", livePrice) : String.format("%.2f", livePrice)));
                        }

                        // Détails si l'actif est sélectionné
                        if (selectedAsset != null && selectedAsset.getSymbol().equalsIgnoreCase(asset.getSymbol())) {
                            updatePnL(livePrice);
                            calculateTotal();
                        }
                    });
                }
            });
        }
    }

    private void updateNews(String symbol) {
        new Thread(() -> {
            try {
                String query = symbol.replace("USDT", "");
                List<News> news = newsService.getLatestNews(query);
                Platform.runLater(() -> {
                    vboxNews.getChildren().clear();
                    news.forEach(n -> {
                        Label l = new Label("• " + n.getTitle());
                        // Style propre pour la liste des news
                        l.setStyle("-fx-text-fill: #e1e1e1; -fx-padding: 10; -fx-font-size: 12; -fx-border-color: #2b3139; -fx-border-width: 0 0 1 0;");
                        l.setWrapText(true);
                        vboxNews.getChildren().add(l);
                    });
                });
            } catch (Exception e) { e.printStackTrace(); }
        }).start();
    }

    private void setupTickerLoop() {
        try {
            List<Asset> assets = assetService.SelectAll();
            tickerContainer.getChildren().clear();
            for (int i = 0; i < 2; i++) {
                for (Asset a : assets) {
                    HBox item = new HBox(8); item.setAlignment(Pos.CENTER_LEFT);
                    Label sym = new Label(a.getSymbol().toUpperCase()); sym.setStyle("-fx-text-fill: #0ecb81; -fx-font-weight: bold;");
                    Label prc = new Label(String.format("%.2f", a.getCurrentPrice())); prc.setStyle("-fx-text-fill: white;");
                    tickerPriceLabels.computeIfAbsent(a.getSymbol().toLowerCase(), k -> new ArrayList<>()).add(prc);
                    item.getChildren().addAll(sym, prc, new Label(" | "));
                    tickerContainer.getChildren().add(item);
                }
            }
            Timeline scroll = new Timeline(new KeyFrame(Duration.millis(30), e -> {
                tickerContainer.setTranslateX(tickerContainer.getTranslateX() - 1);
                if (Math.abs(tickerContainer.getTranslateX()) >= (tickerContainer.getWidth() / 2)) tickerContainer.setTranslateX(0);
            }));
            scroll.setCycleCount(Animation.INDEFINITE); scroll.play();
        } catch (Exception e) {}
    }

    private void calculateTotal() {
        if (selectedAsset == null) return;
        try {
            double qty = Double.parseDouble(txtQty.getText().replace(",", "."));
            double price = "LIMIT".equals(comboOrderMode.getValue()) ? Double.parseDouble(txtTargetPrice.getText().replace(",", ".")) : selectedAsset.getCurrentPrice();
            lblTotal.setText(String.format("%.2f USDT", qty * price));
        } catch (Exception e) { lblTotal.setText("0.00 USDT"); }
    }

    private void updateBalances() {
        try {
            double usdt = wcService.getBalance(MY_WALLET_ID, USDT_ID);
            lblBalance.setText(String.format("%.2f USDT", usdt));
            lblBalanceTND.setText(String.format("≈ %.3f TND", usdt * 3.12));
        } catch (Exception e) {}
    }

    private void updatePnL(double currentPrice) {
        if (initialEntryPrice <= 0) return;
        double pnl = (currentPrice - initialEntryPrice) / initialEntryPrice * 100;
        lblPnL.setText(String.format("%+.2f%%", pnl));
        lblPnL.setStyle("-fx-text-fill: " + (pnl >= 0 ? "#0ecb81" : "#f6465d") + ";");
    }

    private void loadAssetCache() {
        try { assetNamesCache = assetService.SelectAll().stream().collect(Collectors.toMap(Asset::getId, Asset::getSymbol)); } catch (Exception e) {}
    }

    private void setupOrderInputs() {
        comboOrderMode.getItems().setAll("MARKET", "LIMIT");
        comboOrderMode.setValue("MARKET");
        txtTargetPrice.disableProperty().bind(comboOrderMode.valueProperty().isEqualTo("MARKET"));
    }

    private void updateTradingView(String symbol) {
        String pair = "BINANCE:" + symbol.toUpperCase() + "USDT";
        Platform.runLater(() -> chartWebView.getEngine().load("https://s.tradingview.com/widgetembed/?symbol=" + pair + "&theme=dark"));
    }

    private void updateAISignal(String symbol) {
        new Thread(() -> {
            double rsi = marketApi.calculateRSI(symbol);
            Platform.runLater(() -> {
                String adv = (rsi < 35) ? "STRONG BUY" : (rsi > 65) ? "STRONG SELL" : "NEUTRAL";
                lblAdvice.setText("AI SIGNAL: " + adv + " (RSI: " + String.format("%.2f", rsi) + ")");
            });
        }).start();
    }

    @FXML private void onBuy() { processTrade(TradeType.BUY); }
    @FXML private void onSell() { processTrade(TradeType.SELL); }

    private void processTrade(TradeType type) {
        if (selectedAsset == null || txtQty.getText().isEmpty()) return;
        try {
            double qty = Double.parseDouble(txtQty.getText().replace(",", "."));
            OrderMode mode = OrderMode.valueOf(comboOrderMode.getValue());
            double price = (mode == OrderMode.MARKET) ? selectedAsset.getCurrentPrice() : Double.parseDouble(txtTargetPrice.getText());
            Trade t = new Trade(0, 1, selectedAsset.getId(), type, mode, price, qty, (mode == OrderMode.MARKET ? Status.COMPLETED : Status.PENDING), LocalDateTime.now(), null);
            tradeService.insertOne(t);
            updateBalances();
            showAlert("Order Status", "Order for " + type + " has been sent.");
        } catch (Exception e) { showAlert("Error", e.getMessage()); }
    }

    @FXML private void showMarket() { paneMarket.setVisible(true); paneHistory.setVisible(false); }
    @FXML private void showHistory() { paneMarket.setVisible(false); paneHistory.setVisible(true); loadTradeHistory(); }
    private void loadTradeHistory() { try { tableHistory.getItems().setAll(tradeService.SelectAll()); } catch (Exception e) {} }
    @FXML private void onSuggestQuantity() { if (selectedAsset == null) return; try { double bal = wcService.getBalance(MY_WALLET_ID, USDT_ID); txtQty.setText(String.format("%.4f", (bal * 0.1) / selectedAsset.getCurrentPrice())); } catch (Exception e) {} }
    private void showAlert(String title, String content) { Alert a = new Alert(Alert.AlertType.INFORMATION); a.setTitle(title); a.setHeaderText(null); a.setContentText(content); a.show(); }
    @FXML private void openBotWindow() { try { Parent root = FXMLLoader.load(getClass().getResource("/BotView.fxml")); Stage s = new Stage(); s.setScene(new Scene(root)); s.setTitle("Trading Bot Engine"); s.show(); } catch (IOException e) {} }
}