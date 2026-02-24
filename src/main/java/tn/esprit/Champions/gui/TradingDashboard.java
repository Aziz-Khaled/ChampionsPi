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
import java.util.List; // AJOUTÉ

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
    @FXML private VBox vboxNews; // AJOUTÉ : Pour afficher les news dans l'UI

    private XYChart.Series<String, Number> series = new XYChart.Series<>();

    // Services
    private final TradeService tradeService = new TradeService();
    private final MarketApiService marketApi = new MarketApiService();
    private final wallet_currencyService wcService = new wallet_currencyService();
    private final AssetService assetService = new AssetService();
    private final TransactionService transService = new TransactionService();
    private final NewsService newsService = new NewsService(); // AJOUTÉ

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

        // Écouteur de sélection d'actif
        tableAssets.getSelectionModel().selectedItemProperty().addListener((obs, old, newVal) -> {
            if (newVal != null) {
                selectedAsset = newVal;
                initialEntryPrice = newVal.getCurrentPrice();
                lblSelected.setText(newVal.getSymbol().toUpperCase() + " / USDT");
                series.getData().clear();

                // MISE À JOUR : On lance le flux WebSocket pour l'actif sélectionné
                startLiveStreaming(newVal.getSymbol());

                updateAISignal(newVal.getSymbol());

                // AJOUTÉ : Charger les news CryptoPanic
                updateNews(newVal.getSymbol());
            }
        });

        txtQty.textProperty().addListener((obs, old, newVal) -> calculateTotal());

        // On met à jour les soldes une première fois
        updateBalances();
    }

    // --- NOUVELLE MÉTHODE POUR CRYPTOPANIC ---
    private void updateNews(String symbol) {
        new Thread(() -> {
            // On nettoie le symbole (ex: BTCUSDT -> BTC)
            String cleanSymbol = symbol.toUpperCase().replace("USDT", "");
            List<News> newsList = newsService.getLatestNews(cleanSymbol);

            Platform.runLater(() -> {
                if (vboxNews != null) {
                    vboxNews.getChildren().clear();
                    for (News n : newsList) {
                        Label newsLabel = new Label("• " + n.getTitle());
                        newsLabel.setWrapText(true);
                        newsLabel.setPrefWidth(250); // Ajuste selon ton interface

                        // Style selon le sentiment
                        if ("bullish".equals(n.getSentiment())) {
                            newsLabel.setStyle("-fx-text-fill: #2ecc71; -fx-font-weight: bold;");
                        } else if ("bearish".equals(n.getSentiment())) {
                            newsLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
                        }

                        vboxNews.getChildren().add(newsLabel);
                    }
                }
            });
        }).start();
    }

    /**
     * NOUVELLE MÉTHODE : Utilise le WebSocket au lieu du Timer
     */
    private void startLiveStreaming(String symbol) {
        System.out.println("Starting WebSocket stream for: " + symbol);

        marketApi.startPriceStream(symbol, (Double livePrice) -> {
            // Le WebSocket est asynchrone, il faut forcer le retour sur le thread JavaFX
            Platform.runLater(() -> {
                if (selectedAsset != null && selectedAsset.getSymbol().equalsIgnoreCase(symbol)) {
                    // 1. Mettre à jour l'objet et l'UI
                    selectedAsset.setCurrentPrice(livePrice);
                    tableAssets.refresh(); // Met à jour le prix dans le tableau

                    // 2. Mettre à jour les indicateurs visuels
                    updateLiveChart(livePrice);
                    updatePnL(livePrice);
                    calculateTotal();
                    updateBalances();
                }
            });
        });
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

    private void processTrade(TradeType type) {
        if (selectedAsset == null || txtQty.getText().isEmpty()) {
            showAlert("Error", "Please select an asset and quantity.");
            return;
        }

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
                if (type == TradeType.BUY) {
                    t.setIdWalletSource(MY_WALLET_ID);
                    t.setIdWalletDestination(MARKET_WALLET_ID);
                    t.setType(typeTransaction.ACHAT);
                } else {
                    t.setIdWalletSource(MARKET_WALLET_ID);
                    t.setIdWalletDestination(MY_WALLET_ID);
                    t.setType(typeTransaction.VENTE);
                }
                t.setMontant(totalAmount);
                t.setCurrencyId(USDT_ID);
                t.setStatut(StatutTransaction.Completed);
                t.setDateTransaction(LocalDateTime.now());
                transService.insertOne(t);
            }

            showAlert("Success", "Trade and Financial Transaction recorded!");
            updateBalances();
        } catch (Exception e) {
            showAlert("Error", "Transaction failed: " + e.getMessage());
        }
    }

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

    private void calculateTotal() {
        if (selectedAsset != null && !txtQty.getText().isEmpty()) {
            try {
                double qty = Double.parseDouble(txtQty.getText().replace(",", "."));
                lblTotal.setText(String.format("%.2f USDT", qty * selectedAsset.getCurrentPrice()));
            } catch (Exception e) { }
        }
    }

    private void updatePnL(double currentPrice) {
        if (initialEntryPrice <= 0) return;
        double pnl = ((currentPrice - initialEntryPrice) / initialEntryPrice) * 100;
        lblPnL.setText(String.format("%+.2f%%", pnl));
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
        // Utilisation des secondes pour l'axe X
        String time = LocalDateTime.now().toString().substring(17, 19);
        series.getData().add(new XYChart.Data<>(time, price));
        if (series.getData().size() > 30) series.getData().remove(0); // Garder 30 points
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
            stage.setTitle("Trading Bot AI - Configuration");
            stage.setScene(new Scene(root));
            stage.show();

        } catch (IOException e) {
            System.err.println("Failed to load Bot UI: " + e.getMessage());
            showAlert("Error", "Could not load Bot interface.");
            e.printStackTrace();
        }
    }
}