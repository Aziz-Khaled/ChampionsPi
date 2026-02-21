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

    @FXML private TableView<Asset> tableAssets;
    @FXML private TableColumn<Asset, String> colSymbol;
    @FXML private TableColumn<Asset, Double> colPrice;
    @FXML private Label lblBalance, lblBalanceTND, lblSelected, lblTotal, lblSL, lblTP;
    @FXML private Label lblRSI, lblSignal;
    @FXML private TextField txtQty;
    @FXML private ComboBox<OrderMode> comboMode;
    @FXML private LineChart<String, Number> priceChart;

    private XYChart.Series<String, Number> series = new XYChart.Series<>();

    // Services existants (Trading)
    private AssetService assetService = new AssetService();
    private TradeService tradeService = new TradeService();
    private MarketApiService marketApi = new MarketApiService();
    private TechnicalAnalysisService taApi = new TechnicalAnalysisService();

    // Services de la collègue (Wallet)
    private wallet_currencyService wcService = new wallet_currencyService();

    private Asset selectedAsset;
    private wallet_currency usdtWallet;
    private final int currentWalletId = 1; // ID de ton wallet en DB
    private final double TND_RATE = 3.12;

    @FXML
    public void initialize() {
        // 1. Configurer la table des actifs
        colSymbol.setCellValueFactory(new PropertyValueFactory<>("symbol"));
        colPrice.setCellValueFactory(new PropertyValueFactory<>("currentPrice"));

        // 2. Configurer le graphique et les modes d'ordre
        priceChart.getData().add(series);
        comboMode.getItems().setAll(OrderMode.values());
        comboMode.setValue(OrderMode.MARKET);

        // 3. Charger les DONNÉES RÉELLES de la base
        loadRealWalletData();
        loadInitialAssets();

        // 4. Lancer le moteur de mise à jour (Prix et RSI)
        startLiveEngine();

        // 5. Listener pour changer d'actif
        tableAssets.getSelectionModel().selectedItemProperty().addListener((obs, old, newVal) -> {
            if (newVal != null) {
                selectedAsset = newVal;
                series.getData().clear();
                lblSelected.setText(newVal.getSymbol().toUpperCase() + " / USDT");
                updateTechnicalAnalysis();
                updateMetrics();
            }
        });
    }

    // RÉCUPÉRATION RÉELLE DEPUIS LA DB
    private void loadRealWalletData() {
        try {
            // On cherche l'ID de la monnaie USDT en base
            int usdtId = wcService.getCurrencyIdByName("USDT");

            // On récupère la ligne de solde pour ce wallet et cette monnaie
            usdtWallet = wcService.getWalletCurrencyByWalletAndId(currentWalletId, usdtId);

            if (usdtWallet != null) {
                refreshBalanceUI();
            } else {
                lblBalance.setText("0.00 USDT");
                lblBalanceTND.setText("Vérifiez votre table wallet_currency");
            }
        } catch (SQLException e) {
            System.err.println("Erreur SQL lors du chargement du solde : " + e.getMessage());
        }
    }

    private void refreshBalanceUI() {
        double solde = usdtWallet.getSolde();
        lblBalance.setText(String.format("%.2f USDT", solde));
        lblBalanceTND.setText(String.format("≈ %.3f TND", solde * TND_RATE));
    }

    private void startLiveEngine() {
        // Mise à jour des prix toutes les 2 secondes
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

        // Mise à jour Analyse Technique toutes les 10 secondes
        Timeline taEngine = new Timeline(new KeyFrame(Duration.seconds(10), e -> updateTechnicalAnalysis()));

        engine.setCycleCount(Animation.INDEFINITE);
        taEngine.setCycleCount(Animation.INDEFINITE);
        engine.play();
        taEngine.play();
    }

    @FXML
    private void onBuy() {
        handleTrade(TradeType.BUY);
    }

    @FXML
    private void onSell() {
        handleTrade(TradeType.SELL);
    }

    private void handleTrade(TradeType type) {
        if (selectedAsset == null || txtQty.getText().isEmpty() || usdtWallet == null) return;

        try {
            double qty = Double.parseDouble(txtQty.getText());
            double price = selectedAsset.getCurrentPrice();
            double totalCost = qty * price;

            // Vérification solde réel
            if (type == TradeType.BUY && totalCost > usdtWallet.getSolde()) {
                showAlert(Alert.AlertType.ERROR, "Solde insuffisant !");
                return;
            }

            // 1. Mise à jour de l'objet solde
            double nouveauSolde = (type == TradeType.BUY)
                    ? usdtWallet.getSolde() - totalCost
                    : usdtWallet.getSolde() + totalCost;

            usdtWallet.setSolde(nouveauSolde);

            // 2. SAUVEGARDE RÉELLE DANS LA BASE (via le service de ta collègue)
            wcService.updateOne(usdtWallet);

            // 3. Enregistrement du Trade (ton service)
            Trade t = new Trade(0, currentWalletId, selectedAsset.getId(), type, comboMode.getValue(), price, qty,
                    TradingEngine.resolveStatus(comboMode.getValue()), LocalDateTime.now(), LocalDateTime.now());
            tradeService.insertOne(t);

            // 4. Mise à jour Interface
            refreshBalanceUI();
            showAlert(Alert.AlertType.INFORMATION, "Ordre " + type + " effectué avec succès !");

        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Erreur : " + e.getMessage());
        }
    }

    // Méthodes utilitaires
    private void updateChart(double price) {
        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        series.getData().add(new XYChart.Data<>(time, price));
        if (series.getData().size() > 15) series.getData().remove(0);
    }

    private void updateTechnicalAnalysis() {
        if (selectedAsset == null) return;
        double rsi = taApi.fetchRSI(selectedAsset.getSymbol());
        lblRSI.setText(String.format("RSI (14): %.2f", rsi));
        lblSignal.setText(taApi.getAdvice(rsi));
    }

    private void updateMetrics() {
        if (selectedAsset == null || txtQty.getText().isEmpty()) return;
        double q = Double.parseDouble(txtQty.getText());
        lblTotal.setText(String.format("Total: %.2f USDT", q * selectedAsset.getCurrentPrice()));
    }

    private void loadInitialAssets() {
        try { tableAssets.getItems().setAll(assetService.SelectAll()); } catch (Exception e) { e.printStackTrace(); }
    }

    private void showAlert(Alert.AlertType type, String content) {
        new Alert(type, content).show();
    }
}