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
import javafx.scene.layout.Priority;
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

    @FXML private VBox paneAIChat;
    @FXML private TextArea chatArea;
    @FXML private TextField txtChatInput;
    @FXML private Button btnSend;

    private final TradeService tradeService = new TradeService();
    private final MarketApiService marketApi = new MarketApiService();
    private final wallet_currencyService wcService = new wallet_currencyService();
    private final AssetService assetService = new AssetService();
    private final NewsService newsService = new NewsService();
    private final TransactionService transactionService = new TransactionService();
    private final GroqService aiService = new GroqService();

    private final int MY_WALLET_ID = 3;
    private final int MARKET_WALLET_ID = 4;
    private final int USDT_ID = 1;
    private final int CURRENT_USER_ID = 1;

    private final Map<String, Image> logoCache = new HashMap<>();
    private final Map<String, List<Label>> tickerPriceLabels = new HashMap<>();
    private Map<Integer, String> assetNamesCache;
    private Asset selectedAsset;
    private double initialEntryPrice = 0.0;

    @FXML
    public void initialize() {
        loadAssetCache();
        setupTables();
        setupOrderInputs();
        setupTickerLoop();

        tableAssets.getSelectionModel().selectedItemProperty().addListener((obs, old, newVal) -> {
            if (newVal != null) {
                selectedAsset = newVal;
                initialEntryPrice = newVal.getCurrentPrice() != null ? newVal.getCurrentPrice() : 0.0;
                lblSelected.setText(newVal.getSymbol().toUpperCase() + " / USDT");
                updateTradingView(newVal.getSymbol());
                updateNews(newVal.getSymbol());
                updateAISignal(newVal.getSymbol());
                calculateTotal();
            }
        });

        txtQty.textProperty().addListener((obs, old, newVal) -> calculateTotal());
        txtTargetPrice.textProperty().addListener((obs, old, newVal) -> calculateTotal());
        comboOrderMode.valueProperty().addListener((obs, old, newVal) -> calculateTotal());

        updateBalances();
        startGlobalPriceUpdates();
        updateAllNews();
    }

    private void updateAllNews() {
        new Thread(() -> {
            List<News> newsList = newsService.getLatestNews("CRYPTO");
            Platform.runLater(() -> {
                vboxNews.getChildren().clear();
                vboxNews.setSpacing(10);
                for (News n : newsList) {
                    Label l = new Label("• " + n.getTitle());
                    l.setWrapText(true);
                    l.setMaxWidth(230); // Largeur max pour forcer le retour à la ligne
                    l.setStyle("-fx-text-fill: white; -fx-padding: 8; -fx-border-color: #2b3139; -fx-border-width: 0 0 1 0;");
                    vboxNews.getChildren().add(l);
                }
            });
        }).start();
    }

    private void updateNews(String symbol) {
        new Thread(() -> {
            String cleanSymbol = symbol.toUpperCase().replace("USDT", "");
            List<News> newsList = newsService.getLatestNews(cleanSymbol);
            Platform.runLater(() -> {
                vboxNews.getChildren().clear();
                vboxNews.setSpacing(10);
                for (News n : newsList) {
                    Label l = new Label("• " + n.getTitle());
                    l.setWrapText(true);
                    l.setMaxWidth(230);
                    l.setStyle("-fx-text-fill: white; -fx-padding: 8; -fx-border-color: #2b3139; -fx-border-width: 0 0 1 0;");
                    vboxNews.getChildren().add(l);
                }
            });
        }).start();
    }

    @FXML
    private void showAIChat() {
        paneMarket.setVisible(false); paneHistory.setVisible(false);
        if (paneAIChat != null) paneAIChat.setVisible(true);
    }

    @FXML
    private void showMarket() {
        paneMarket.setVisible(true); paneHistory.setVisible(false);
        if (paneAIChat != null) paneAIChat.setVisible(false);
    }

    @FXML
    private void showHistory() {
        paneMarket.setVisible(false); paneHistory.setVisible(true);
        if (paneAIChat != null) paneAIChat.setVisible(false);
        loadTradeHistory();
    }

    @FXML
    private void onSendMessage() {
        String userText = txtChatInput.getText();
        if (userText == null || userText.trim().isEmpty()) return;
        chatArea.appendText("Moi: " + userText + "\n");
        txtChatInput.clear();
        if(btnSend != null) btnSend.setDisable(true);
        new Thread(() -> {
            try {
                String context = (selectedAsset != null) ? "Actif: " + selectedAsset.getSymbol() : "Marché global.";
                String response = aiService.askAI(userText, context);
                Platform.runLater(() -> {
                    chatArea.appendText("AI Expert (Groq): " + response + "\n\n");
                    if(btnSend != null) btnSend.setDisable(false);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    chatArea.appendText("Erreur IA.\n");
                    if(btnSend != null) btnSend.setDisable(false);
                });
            }
        }).start();
    }

    private void processTrade(TradeType type) {
        if (selectedAsset == null || txtQty.getText().isEmpty()) return;
        try {
            double qty = Double.parseDouble(txtQty.getText().replace(",", "."));
            OrderMode mode = OrderMode.valueOf(comboOrderMode.getValue());
            double price = mode == OrderMode.MARKET ? selectedAsset.getCurrentPrice() : Double.parseDouble(txtTargetPrice.getText());
            Trade trade = new Trade(0, CURRENT_USER_ID, selectedAsset.getId(), type, mode, price, qty, (mode == OrderMode.MARKET ? Status.COMPLETED : Status.PENDING), LocalDateTime.now(), (mode == OrderMode.MARKET ? LocalDateTime.now() : null));
            tradeService.insertOne(trade);
            if (mode == OrderMode.MARKET) {
                transaction t = new transaction();
                t.setIdWalletSource(type == TradeType.BUY ? MY_WALLET_ID : MARKET_WALLET_ID);
                t.setIdWalletDestination(type == TradeType.BUY ? MARKET_WALLET_ID : MY_WALLET_ID);
                t.setMontant(qty * price);
                t.setType(type == TradeType.BUY ? typeTransaction.ACHAT : typeTransaction.VENTE);
                t.setCurrencyId(USDT_ID);
                t.setStatut(StatutTransaction.Completed);
                t.setDateTransaction(LocalDateTime.now());
                transactionService.insertOne(t);
            }
            showAlert("Success", "Order Placed!");
            updateBalances();
            if (paneHistory.isVisible()) loadTradeHistory();
        } catch (Exception e) { showAlert("Error", e.getMessage()); }
    }

    private void updateBalances() {
        try {
            double usdt = wcService.getBalance(MY_WALLET_ID, USDT_ID);
            lblBalance.setText(String.format("%.2f USDT", usdt));
            lblBalanceTND.setText(String.format("≈ %.3f TND", usdt * 3.12));
        } catch (Exception e) {}
    }

    private void setupTables() {
        colSymbol.setCellFactory(column -> new TableCell<>() {
            private final ImageView img = new ImageView();
            private final Label lbl = new Label();
            private final HBox box = new HBox(10, img, lbl);
            { box.setAlignment(Pos.CENTER_LEFT); img.setFitHeight(20); img.setFitWidth(20); }
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) setGraphic(null);
                else {
                    lbl.setText(item.toUpperCase()); lbl.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");
                    String sym = item.toLowerCase();
                    if (!logoCache.containsKey(sym)) {
                        logoCache.put(sym, new Image("https://raw.githubusercontent.com/spothq/cryptocurrency-icons/master/128/color/" + sym + ".png", 20, 20, true, true, true));
                    }
                    img.setImage(logoCache.get(sym)); setGraphic(box);
                }
            }
        });
        colPrice.setCellValueFactory(new PropertyValueFactory<>("currentPrice"));
        colSymbol.setCellValueFactory(new PropertyValueFactory<>("symbol"));
        colHistSymbol.setCellValueFactory(d -> new SimpleStringProperty(assetNamesCache.getOrDefault(d.getValue().getAsset_id(), "Unknown")));
        colHistQty.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        colHistPrice.setCellValueFactory(new PropertyValueFactory<>("price"));
        colHistType.setCellValueFactory(new PropertyValueFactory<>("tradeType"));
        colHistStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        loadInitialAssets();
    }

    private void startGlobalPriceUpdates() {
        for (Asset asset : tableAssets.getItems()) {
            marketApi.startPriceStream(asset.getSymbol().toUpperCase() + "USDT", (Double livePrice) -> {
                if (livePrice != null && livePrice > 0) {
                    Platform.runLater(() -> {
                        asset.setCurrentPrice(livePrice); tableAssets.refresh();
                        if (selectedAsset != null && selectedAsset.getSymbol().equalsIgnoreCase(asset.getSymbol())) {
                            updatePnL(livePrice); calculateTotal();
                        }
                    });
                }
            });
        }
    }

    private void calculateTotal() {
        if (selectedAsset == null) return;
        try {
            double qty = Double.parseDouble(txtQty.getText().replace(",", "."));
            double price = "LIMIT".equals(comboOrderMode.getValue()) ? Double.parseDouble(txtTargetPrice.getText()) : selectedAsset.getCurrentPrice();
            lblTotal.setText(String.format("%.2f USDT", qty * price));
        } catch (Exception e) { lblTotal.setText("0.00 USDT"); }
    }

    private void loadInitialAssets() { try { tableAssets.getItems().setAll(assetService.SelectAll()); } catch (Exception e) {} }
    private void loadAssetCache() { try { assetNamesCache = assetService.SelectAll().stream().collect(Collectors.toMap(Asset::getId, Asset::getSymbol)); } catch (Exception e) {} }
    private void setupOrderInputs() { comboOrderMode.getItems().setAll("MARKET", "LIMIT"); comboOrderMode.setValue("MARKET"); txtTargetPrice.disableProperty().bind(comboOrderMode.valueProperty().isEqualTo("MARKET")); }
    private void updateTradingView(String symbol) { Platform.runLater(() -> chartWebView.getEngine().load("https://s.tradingview.com/widgetembed/?symbol=BINANCE:" + symbol.toUpperCase() + "USDT&theme=dark")); }
    private void updateAISignal(String symbol) { new Thread(() -> { double rsi = marketApi.calculateRSI(symbol); Platform.runLater(() -> lblAdvice.setText("AI SIGNAL: " + (rsi < 35 ? "BUY" : rsi > 65 ? "SELL" : "NEUTRAL") + " (RSI: " + String.format("%.2f", rsi) + ")")); }).start(); }
    private void setupTickerLoop() { Timeline timeline = new Timeline(new KeyFrame(Duration.millis(50), e -> { if (tickerContainer != null) { tickerContainer.setLayoutX(tickerContainer.getLayoutX() - 1); if (tickerContainer.getLayoutX() < -500) tickerContainer.setLayoutX(800); } })); timeline.setCycleCount(Animation.INDEFINITE); timeline.play(); }
    private void updatePnL(double currentPrice) { if (initialEntryPrice <= 0) return; double pnl = (currentPrice - initialEntryPrice) / initialEntryPrice * 100; lblPnL.setText(String.format("%+.2f%%", pnl)); lblPnL.setStyle("-fx-text-fill: " + (pnl >= 0 ? "#0ecb81" : "#f6465d") + ";"); }
    @FXML private void onBuy() { processTrade(TradeType.BUY); }
    @FXML private void onSell() { processTrade(TradeType.SELL); }
    private void loadTradeHistory() { try { tableHistory.getItems().setAll(tradeService.SelectAll()); } catch (Exception e) {} }
    @FXML private void onSuggestQuantity() { if (selectedAsset == null) return; try { double bal = wcService.getBalance(MY_WALLET_ID, USDT_ID); txtQty.setText(String.format("%.4f", (bal * 0.1) / selectedAsset.getCurrentPrice())); } catch (Exception e) {} }
    private void showAlert(String title, String content) { Alert a = new Alert(Alert.AlertType.INFORMATION); a.setTitle(title); a.setHeaderText(null); a.setContentText(content); a.show(); }
    @FXML private void openBotWindow() { try { Parent root = FXMLLoader.load(getClass().getResource("/BotView.fxml")); Stage s = new Stage(); s.setScene(new Scene(root)); s.show(); } catch (IOException e) {} }
}