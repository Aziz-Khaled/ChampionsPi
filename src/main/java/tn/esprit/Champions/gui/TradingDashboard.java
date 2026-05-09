package tn.esprit.Champions.gui;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
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
import javafx.util.StringConverter;
import tn.esprit.Champions.models.*;
import tn.esprit.Champions.services.*;
import tn.esprit.Champions.utils.UserSession;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class TradingDashboard {

    // --- Éléments FXML ---
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
    @FXML private ComboBox<wallet> comboWalletSelection;

    // --- Services ---
    private final TradeService tradeService = new TradeService();
    private final MarketApiService marketApi = new MarketApiService();
    private final wallet_currencyService wcService = new wallet_currencyService();
    private final AssetService assetService = new AssetService();
    private final NewsService newsService = new NewsService();
    private final TransactionService transactionService = new TransactionService();
    private final GroqService aiService = new GroqService();
    private final WalletService walletService = new WalletService();
    private final CurrencyService currencyService = new CurrencyService(); // ✅ CORRECTION: AJOUTÉ



    private final Map<String, Image> logoCache = new HashMap<>();
    private Map<Integer, String> assetNamesCache;
    private Asset selectedAsset;
    private double initialEntryPrice = 0.0;

    @FXML
    public void initialize() {
        if (UserSession.getLoggedInUser() == null) {
            System.err.println("ERROR: No user logged in! Redirecting...");
            // Optionally redirect to login page
            return;
        }

        loadAssetCache();
        setupTables();
        setupOrderInputs();
        setupTickerLoop();
        setupWalletSelector();
        setupAutoRefresh();



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

        startGlobalPriceUpdates();
        updateAllNews();
    }



    private int getCurrentUserId() {
        Utilisateur user = UserSession.getLoggedInUser();
        if (user == null) {
            System.err.println("ERROR: No user logged in!");
            return -1;
        }
        return user.getId_user();
    }
    private void setupWalletSelector() {
        try {
            int userId = getCurrentUserId();
            if (userId <= 0) {
                lblBalance.setText("User not logged in");
                return;
            }

            List<wallet> tradingWallets = walletService.SelectAll().stream()
                    .filter(w -> w.getIdUser() == userId)  // ✅ Changed from CURRENT_USER_ID
                    .filter(w -> w.getTypeWallet() == typeWallet.trading)
                    .collect(Collectors.toList());

            comboWalletSelection.getItems().clear();
            comboWalletSelection.getItems().addAll(tradingWallets);

            comboWalletSelection.setConverter(new StringConverter<wallet>() {
                @Override
                public String toString(wallet w) { return w == null ? "" : "RIB: " + w.getRib(); }
                @Override
                public wallet fromString(String string) { return null; }
            });

            comboWalletSelection.getSelectionModel().selectedItemProperty().addListener((obs, oldW, newW) -> {
                if (newW != null) {
                    updateSpecificWalletBalance(newW.getIdWallet());
                }
            });

            if (!tradingWallets.isEmpty()) {
                comboWalletSelection.getSelectionModel().selectFirst();
            } else {
                lblBalance.setText("Aucun Wallet Trading");
                lblBalanceTND.setText("0.00 TND");
            }

        } catch (SQLException e) {
            e.printStackTrace();
            lblBalance.setText("Erreur chargement");
        }
    }
    private void setupAutoRefresh() {
        Timeline autoRefresh = new Timeline(new KeyFrame(Duration.seconds(3), event -> {
            if (comboWalletSelection.getValue() != null) {
                updateSpecificWalletBalance(comboWalletSelection.getValue().getIdWallet());
            }

            if (paneHistory.isVisible()) {
                loadTradeHistory();
            }
        }));
        autoRefresh.setCycleCount(Animation.INDEFINITE);
        autoRefresh.play();
    }

    private void updateSpecificWalletBalance(int walletId) {
        try {
            List<wallet_currency> allCurrenciesInWallet = wcService.SelectAll().stream()
                    .filter(wc -> wc.getId_wallet() == walletId)
                    .collect(Collectors.toList());

            // ✅ CORRECTION: Recherche plus robuste de l'USDT
            wallet_currency usdtWalletCurrency = allCurrenciesInWallet.stream()
                    .filter(wc -> wc.getNom_currency() != null && wc.getNom_currency().equalsIgnoreCase("USDT"))
                    .findFirst()
                    .orElse(null);

            if (usdtWalletCurrency != null) {
                lblBalance.setText(String.format("%.2f USDT", usdtWalletCurrency.getSolde()));
            } else {
                lblBalance.setText("0.00 USDT");
            }
            lblBalanceTND.setText("0.00 TND");
        } catch (SQLException e) {
            lblBalance.setText("Error");
            e.printStackTrace();
        }
    }
    private void processTrade(TradeType type) {
        if (selectedAsset == null || txtQty.getText().isEmpty() || comboWalletSelection.getValue() == null) {
            showAlert("Erreur", "Champs manquants.");
            return;
        }

        try {
            int userId = getCurrentUserId();  // ✅ Get current user
            if (userId <= 0) {
                showAlert("Erreur", "Utilisateur non connecté!");
                return;
            }

            double qty = Double.parseDouble(txtQty.getText().replace(",", "."));
            OrderMode mode = OrderMode.valueOf(comboOrderMode.getValue());
            double price = (mode == OrderMode.MARKET) ?
                    selectedAsset.getCurrentPrice() :
                    Double.parseDouble(txtTargetPrice.getText().replace(",", "."));

            int userWalletId = comboWalletSelection.getValue().getIdWallet();

            Trade trade = new Trade(
                    0, userId, selectedAsset.getId(), type, mode, price, qty,  // ✅ Changed from CURRENT_USER_ID
                    (mode == OrderMode.MARKET ? Status.COMPLETED : Status.PENDING),
                    LocalDateTime.now(), (mode == OrderMode.MARKET ? LocalDateTime.now() : null)
            );

            tradeService.insertOne(trade);

            if (mode == OrderMode.MARKET) {
                executeImmediateTransaction(userWalletId, type, qty, price);
                showAlert("Success", "Market order executed successfully!");
            } else {
                showAlert("Order Placed", "LIMIT order is pending and monitored by the bot.");
            }

            updateSpecificWalletBalance(userWalletId);
            loadTradeHistory();

        } catch (Exception e) {
            showAlert("Erreur", e.getMessage());
            e.printStackTrace();
        }
    }


    /**
     * Méthode pour exécuter la partie financière pour les ordres MARKET uniquement
     */
    private void executeImmediateTransaction(int walletId, TradeType type, double qty, double price) throws Exception {
        currency usdt = currencyService.getByName("USDT");
        currency asset = currencyService.getByName(selectedAsset.getSymbol().toUpperCase());

        double totalUSDT = qty * price;

        Conversion conv = new Conversion();
        conv.setExchangeRate(price);

        if (type == TradeType.BUY) {
            conv.setAmountFrom(totalUSDT); conv.setCurrencyFrom(usdt.getId_currency());
            conv.setAmountTo(qty); conv.setCurrencyTo(asset.getId_currency());
        } else {
            conv.setAmountFrom(qty); conv.setCurrencyFrom(asset.getId_currency());
            conv.setAmountTo(totalUSDT); conv.setCurrencyTo(usdt.getId_currency());
        }

        transaction t = new transaction();
        t.setIdWalletSource(walletId);
        t.setIdWalletDestination(walletId);
        t.setMontant(conv.getAmountFrom());
        t.setCurrencyId(conv.getCurrencyFrom());
        t.setType(type == TradeType.BUY ? typeTransaction.ACHAT : typeTransaction.VENTE);
        t.setStatut(StatutTransaction.Completed);
        t.setDateTransaction(LocalDateTime.now());

        transactionService.insertExchange(t, conv);
    }



    private void showAlert(String title, String content) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(content);
        a.showAndWait();
    }

    @FXML private void onBuy() { processTrade(TradeType.BUY); }
    @FXML private void onSell() { processTrade(TradeType.SELL); }

    @FXML
    private void onSuggestQuantity() {
        if (selectedAsset == null || comboWalletSelection.getValue() == null) return;
        try {
            wallet selectedWallet = comboWalletSelection.getValue();
            List<wallet_currency> allCurrencies = wcService.SelectAll().stream()
                    .filter(wc -> wc.getId_wallet() == selectedWallet.getIdWallet())
                    .collect(Collectors.toList());
            wallet_currency usdtWC = allCurrencies.stream()
                    .filter(wc -> wc.getNom_currency() != null && wc.getNom_currency().equalsIgnoreCase("USDT"))
                    .findFirst()
                    .orElse(null);

            double bal = (usdtWC != null) ? usdtWC.getSolde() : 0;
            if (bal <= 0) {
                showAlert("Info", "Insufficient USDT balance!");
                return;
            }

            double tenPercent = bal * 0.1;
            double qty = tenPercent / selectedAsset.getCurrentPrice();
            txtQty.setText(String.format("%.4f", qty));

        } catch (Exception e) { e.printStackTrace(); }
    }
    // --- Fonctions utilitaires ---
    private void updateAllNews() {
        new Thread(() -> {
            List<News> newsList = newsService.getLatestNews("CRYPTO");
            Platform.runLater(() -> renderNews(newsList));
        }).start();
    }
    private void updateNews(String symbol) {
        new Thread(() -> {
            String cleanSymbol = symbol.toUpperCase().replace("USDT", "");
            List<News> newsList = newsService.getLatestNews(cleanSymbol);
            Platform.runLater(() -> renderNews(newsList));
        }).start();
    }
    private void renderNews(List<News> newsList) {
        vboxNews.getChildren().clear();
        vboxNews.setSpacing(10);
        for (News n : newsList) {
            Label l = new Label("• " + n.getTitle());
            l.setWrapText(true);
            l.setMaxWidth(230);
            l.setStyle("-fx-text-fill: white; -fx-padding: 8; -fx-border-color: #2b3139; -fx-border-width: 0 0 1 0;");
            vboxNews.getChildren().add(l);
        }
    }

    @FXML private void showAIChat() {
        paneMarket.setVisible(false); paneHistory.setVisible(false);
        if (paneAIChat != null) paneAIChat.setVisible(true);
    }
    @FXML private void showMarket() {
        paneMarket.setVisible(true); paneHistory.setVisible(false);
        if (paneAIChat != null) paneAIChat.setVisible(false);
    }
    @FXML private void showHistory() {
        paneMarket.setVisible(false); paneHistory.setVisible(true);
        if (paneAIChat != null) paneAIChat.setVisible(false);
        loadTradeHistory();
    }
    @FXML private void onSendMessage() {
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
        colHistSymbol.setCellValueFactory(d -> {
            int assetId = d.getValue().getAsset_id();
            if (assetNamesCache == null || assetNamesCache.isEmpty()) {
                return new SimpleStringProperty("Cache empty (ID:" + assetId + ")");
            }
            String name = assetNamesCache.get(assetId);
            return new SimpleStringProperty(name != null ? name : "Unknown (ID:" + assetId + ")");
        });
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
            String qtyStr = txtQty.getText().replace(",", ".");
            if (qtyStr.isEmpty()) {
                lblTotal.setText("0.00 USDT");
                return;
            }

            double qty = Double.parseDouble(qtyStr);
            double price;

            // Si on est en mode LIMIT, on prend le prix saisi par l'utilisateur
            if ("LIMIT".equals(comboOrderMode.getValue())) {
                String targetPriceStr = txtTargetPrice.getText().replace(",", ".");
                price = targetPriceStr.isEmpty() ? 0 : Double.parseDouble(targetPriceStr);
            } else {
                // Sinon (MARKET), on prend le prix actuel de l'asset
                price = (selectedAsset.getCurrentPrice() != null) ? selectedAsset.getCurrentPrice() : 0;
            }

            double total = qty * price;
            Platform.runLater(() -> lblTotal.setText(String.format("%.2f USDT", total)));

        } catch (NumberFormatException e) {
            lblTotal.setText("Err");
        }
    }
    private void loadInitialAssets() { try { tableAssets.getItems().setAll(assetService.SelectAll()); } catch (Exception e) {} }
    private void loadAssetCache() {
        try {
            List<Asset> assets = assetService.SelectAll();
            if (assets != null && !assets.isEmpty()) {
                assetNamesCache = assets.stream()
                        .collect(Collectors.toMap(Asset::getId, Asset::getSymbol));
                System.out.println("✅ Asset cache loaded: " + assetNamesCache.size() + " assets");
                assetNamesCache.forEach((id, symbol) ->
                        System.out.println("  Asset ID: " + id + " -> Symbol: " + symbol));

                // ✅ Refresh history table now that cache is ready
                Platform.runLater(() -> tableHistory.refresh());
            } else {
                assetNamesCache = new HashMap<>();
                System.err.println("⚠️ No assets found in database");
            }
        } catch (Exception e) {
            assetNamesCache = new HashMap<>();
            System.err.println("❌ Error loading asset cache: " + e.getMessage());
        }
    }
    private void setupOrderInputs() { comboOrderMode.getItems().setAll("MARKET", "LIMIT"); comboOrderMode.setValue("MARKET"); txtTargetPrice.disableProperty().bind(comboOrderMode.valueProperty().isEqualTo("MARKET")); }
    private void updateTradingView(String symbol) {
        // On s'assure que l'exécution se fait après le rendu initial
        Platform.runLater(() -> {
            if (chartWebView != null && chartWebView.getEngine() != null) {
                String url = "https://s.tradingview.com/widgetembed/?symbol=BINANCE:"
                        + symbol.toUpperCase() + "USDT&theme=dark";
                chartWebView.getEngine().load(url);
            }
        });

    }
    @FXML
    private void handleBackToWallet(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/DashboardWalletClient.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.getScene().setRoot(root);
        } catch (IOException e) {
            System.err.println("Erreur lors du retour au dashboard : " + e.getMessage());
        }
    }

    private void updateAISignal(String symbol) { new Thread(() -> { double rsi = marketApi.calculateRSI(symbol); Platform.runLater(() -> lblAdvice.setText("AI SIGNAL: " + (rsi < 35 ? "BUY" : rsi > 65 ? "SELL" : "NEUTRAL") + " (RSI: " + String.format("%.2f", rsi) + ")")); }).start(); }
    private void setupTickerLoop() { Timeline timeline = new Timeline(new KeyFrame(Duration.millis(50), e -> { if (tickerContainer != null) { tickerContainer.setLayoutX(tickerContainer.getLayoutX() - 1); if (tickerContainer.getLayoutX() < -500) tickerContainer.setLayoutX(800); } })); timeline.setCycleCount(Animation.INDEFINITE); timeline.play(); }
    private void updatePnL(double currentPrice) { if (initialEntryPrice <= 0) return; double pnl = (currentPrice - initialEntryPrice) / initialEntryPrice * 100; lblPnL.setText(String.format("%+.2f%%", pnl)); lblPnL.setStyle("-fx-text-fill: " + (pnl >= 0 ? "#0ecb81" : "#f6465d") + ";"); }
    private void loadTradeHistory() {
        try {
            int userId = getCurrentUserId();
            if (userId <= 0) {
                System.err.println("Cannot load trade history: No user logged in!");
                tableHistory.getItems().clear();
                return;
            }

            List<Trade> userTrades = tradeService.getTradesByUserId(userId);

            // ✅ DEBUG: Print trade asset IDs
            System.out.println("📊 Loaded " + userTrades.size() + " trades:");
            userTrades.forEach(trade ->
                    System.out.println("  Trade ID: " + trade.getId() +
                            " -> Asset ID: " + trade.getAsset_id())
            );

            tableHistory.getItems().setAll(userTrades);
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible de charger l'historique: " + e.getMessage());
        }
    }
    @FXML private void openBotWindow() { try { Parent root = FXMLLoader.load(getClass().getResource("/BotView.fxml")); Stage s = new Stage(); s.setScene(new Scene(root)); s.show(); } catch (IOException e) {} }
}