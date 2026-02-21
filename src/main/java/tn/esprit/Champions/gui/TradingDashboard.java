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

public class TradingDashboard {

    // Éléments FXML
    @FXML private TableView<Asset> tableAssets;
    @FXML private TableColumn<Asset, String> colSymbol;
    @FXML private TableColumn<Asset, Double> colPrice;
    @FXML private Label lblBalance, lblSelected, lblTotal, lblSL, lblTP;
    @FXML private Label lblRSI, lblSignal; // NOUVEAU : Aide au trader
    @FXML private TextField txtQty;
    @FXML private ComboBox<OrderMode> comboMode;
    @FXML private LineChart<String, Number> priceChart;

    // Services et Variables
    private XYChart.Series<String, Number> series = new XYChart.Series<>();
    private AssetService assetService = new AssetService();
    private TradeService tradeService = new TradeService();
    private MarketApiService marketApi = new MarketApiService(); // API 1 : Prix
    private TechnicalAnalysisService taApi = new TechnicalAnalysisService(); // API 2 : Aide Trader

    private Asset selectedAsset;
    private double walletBalance = 100000.0;

    @FXML
    public void initialize() {
        // Setup Table
        colSymbol.setCellValueFactory(new PropertyValueFactory<>("symbol"));
        colPrice.setCellValueFactory(new PropertyValueFactory<>("currentPrice"));

        // Setup Chart & Combo
        priceChart.getData().add(series);
        comboMode.getItems().setAll(OrderMode.values());
        comboMode.setValue(OrderMode.MARKET);

        loadInitialData();
        startGlobalEngine();

        // Listener de sélection
        tableAssets.getSelectionModel().selectedItemProperty().addListener((obs, old, newVal) -> {
            if (newVal != null) {
                selectedAsset = newVal;
                series.getData().clear();
                lblSelected.setText(newVal.getSymbol().toUpperCase() + " / USDT");
                updateTechnicalAnalysis(); // Calculer l'aide au trader immédiatement
                updateMetrics();
            }
        });
    }

    private void startGlobalEngine() {
        // Timeline principale (toutes les 2 secondes)
        Timeline engine = new Timeline(new KeyFrame(Duration.seconds(2), e -> {
            tableAssets.getItems().forEach(a -> {
                double newPrice = marketApi.fetchPrice(a.getSymbol());
                a.setCurrentPrice(newPrice);

                if (selectedAsset != null && a.getId() == selectedAsset.getId()) {
                    updateChart(newPrice);
                    updateMetrics();
                }
            });
            tableAssets.refresh();
        }));

        // Timeline pour l'Analyse Technique (toutes les 10 secondes pour l'API 2)
        Timeline taEngine = new Timeline(new KeyFrame(Duration.seconds(10), e -> updateTechnicalAnalysis()));

        engine.setCycleCount(Animation.INDEFINITE);
        taEngine.setCycleCount(Animation.INDEFINITE);
        engine.play();
        taEngine.play();
    }

    private void updateTechnicalAnalysis() {
        if (selectedAsset == null) return;

        double rsi = taApi.fetchRSI(selectedAsset.getSymbol());
        String advice = taApi.getAdvice(rsi);

        lblRSI.setText(String.format("RSI (14): %.2f", rsi));
        lblSignal.setText(advice);

        // Style dynamique pour aider visuellement le trader
        if (rsi >= 70) lblSignal.setStyle("-fx-text-fill: #ff4757; -fx-font-weight: bold;"); // Rouge (Vendre)
        else if (rsi <= 30) lblSignal.setStyle("-fx-text-fill: #2ed573; -fx-font-weight: bold;"); // Vert (Acheter)
        else lblSignal.setStyle("-fx-text-fill: #ffa502; -fx-font-weight: bold;"); // Orange
    }

    private void updateChart(double price) {
        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        series.getData().add(new XYChart.Data<>(time, price));
        if (series.getData().size() > 15) series.getData().remove(0);
    }

    private void updateMetrics() {
        if (selectedAsset == null) return;
        double p = selectedAsset.getCurrentPrice();
        lblSL.setText(String.format("Stop-Loss: %.2f", RiskManager.getStopLoss(p)));
        lblTP.setText(String.format("Take-Profit: %.2f", RiskManager.getTakeProfit(p)));

        if (!txtQty.getText().isEmpty()) {
            try {
                double q = Double.parseDouble(txtQty.getText());
                lblTotal.setText(String.format("Total: %.2f USDT", TradingEngine.calculateTotal(q, p)));
            } catch (Exception ex) { lblTotal.setText("Total: 0.00"); }
        }
    }

    @FXML private void onBuy() { handleTrade(TradeType.BUY); }
    @FXML private void onSell() { handleTrade(TradeType.SELL); }

    private void handleTrade(TradeType type) {
        if (selectedAsset == null || txtQty.getText().isEmpty()) return;

        double qty = Double.parseDouble(txtQty.getText());
        double price = selectedAsset.getCurrentPrice();
        double totalEffect = (type == TradeType.BUY) ? TradingEngine.calculateTotal(qty, price) : TradingEngine.calculateSaleGain(qty, price);

        if (type == TradeType.BUY && totalEffect > walletBalance) {
            new Alert(Alert.AlertType.ERROR, "Solde insuffisant !").show();
            return;
        }

        try {
            Trade t = new Trade(0, 1, selectedAsset.getId(), type, comboMode.getValue(), price, qty,
                    TradingEngine.resolveStatus(comboMode.getValue()), LocalDateTime.now(), LocalDateTime.now());
            tradeService.insertOne(t);

            walletBalance += (type == TradeType.SELL) ? totalEffect : -totalEffect;
            lblBalance.setText(String.format("%.2f USDT", walletBalance));
            new Alert(Alert.AlertType.INFORMATION, "Ordre " + type + " exécuté avec succès !").show();
        } catch (SQLException e) {
            new Alert(Alert.AlertType.ERROR, "Erreur DB: " + e.getMessage()).show();
        }
    }

    private void loadInitialData() {
        try { tableAssets.getItems().setAll(assetService.SelectAll()); } catch (Exception e) { e.printStackTrace(); }
    }
}