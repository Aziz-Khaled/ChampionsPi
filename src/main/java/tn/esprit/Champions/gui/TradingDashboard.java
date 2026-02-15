package tn.esprit.Champions.gui;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.util.Duration;
import tn.esprit.Champions.models.Asset;
import tn.esprit.Champions.models.Trade;
import tn.esprit.Champions.models.Status;
import tn.esprit.Champions.services.AssetService;
import tn.esprit.Champions.services.TradeService;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Random;

public class TradingDashboard {

    @FXML private Label labelDateTime, labelUser, labelStatus;
    @FXML private Label labelTotalAssets, labelActiveAssets, labelTotalTrades, labelActiveTrades;

    @FXML private TableView<Asset> assetsTable;
    @FXML private TableColumn<Asset, String> colSymbol;
    @FXML private TableColumn<Asset, String> colName;
    @FXML private TableColumn<Asset, String> colType;
    @FXML private TableColumn<Asset, Double> colPrice;
    @FXML private TableColumn<Asset, String> colMarket;
    @FXML private TableColumn<Asset, Status> colStatus;
    @FXML private TableColumn<Asset, LocalDateTime> colUpdatedAt;

    @FXML private TableView<Trade> tradesTable;
    @FXML private TableColumn<Trade, Integer> colTradeID;
    @FXML private TableColumn<Trade, Integer> colTradeUser;
    @FXML private TableColumn<Trade, Integer> colTradeAsset;
    @FXML private TableColumn<Trade, String> colTradeType;
    @FXML private TableColumn<Trade, String> colOrderMode;
    @FXML private TableColumn<Trade, Double> colTradePrice;
    @FXML private TableColumn<Trade, Double> colTradeQty;
    @FXML private TableColumn<Trade, Double> colTradeTotal;
    @FXML private TableColumn<Trade, Status> colTradeStatus;
    @FXML private TableColumn<Trade, LocalDateTime> colTradeCreated;

    @FXML private LineChart<Number, Number> trendChart, priceChart;
    @FXML private BarChart<String, Number> statsChart;

    private final AssetService assetService = new AssetService();
    private final TradeService tradeService = new TradeService();

    private final ObservableList<Asset> allAssets = FXCollections.observableArrayList();
    private final ObservableList<Trade> allTrades = FXCollections.observableArrayList();
    private final Random random = new Random();

    @FXML
    public void initialize() {
        labelUser.setText("ADMIN");
        labelStatus.setText("CONNECTED");
        startDateTime();

        loadData();
        initTables();
        initCharts();
        updateStatistics();
    }

    private void startDateTime() {
        Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
            labelDateTime.setText(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        }));
        timeline.setCycleCount(Animation.INDEFINITE);
        timeline.play();
    }

    private void loadData() {
        List<Asset> assets = null;
        try {
            assets = assetService.SelectAll();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        if (assets != null) allAssets.setAll(assets);
        assetsTable.setItems(allAssets);

        List<Trade> trades = null;
        try {
            trades = tradeService.SelectAll();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        if (trades != null) allTrades.setAll(trades);
        tradesTable.setItems(allTrades);
    }

    private void initTables() {
        colSymbol.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("symbol"));
        colName.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("name"));
        colType.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("type"));
        colPrice.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("price"));
        colMarket.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("market"));
        colStatus.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("status"));
        colUpdatedAt.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("updatedAt"));

        colTradeID.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("id"));
        colTradeUser.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("userId"));
        colTradeAsset.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("assetId"));
        colTradeType.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("tradeType"));
        colOrderMode.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("orderMode"));
        colTradePrice.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("price"));
        colTradeQty.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("quantity"));
        colTradeTotal.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("total"));
        colTradeStatus.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("status"));
        colTradeCreated.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("createdAt"));
    }

    private void initCharts() {
        trendChart.getData().clear();
        priceChart.getData().clear();
        statsChart.getData().clear();

        // Trend chart - 24h
        XYChart.Series<Number, Number> seriesTrend = new XYChart.Series<>();
        seriesTrend.setName("BTC");
        for (int i = 0; i < 24; i++) seriesTrend.getData().add(new XYChart.Data<>(i, 50000 + random.nextDouble() * 1000));
        trendChart.getData().add(seriesTrend);

        // Price chart - 30 jours
        XYChart.Series<Number, Number> seriesPrice = new XYChart.Series<>();
        seriesPrice.setName("ETH");
        for (int i = 0; i < 30; i++) seriesPrice.getData().add(new XYChart.Data<>(i, 3000 + random.nextDouble() * 500));
        priceChart.getData().add(seriesPrice);

        // Stats BarChart
        XYChart.Series<String, Number> seriesStats = new XYChart.Series<>();
        seriesStats.setName("Performance");
        seriesStats.getData().add(new XYChart.Data<>("BTC", 120));
        seriesStats.getData().add(new XYChart.Data<>("ETH", 80));
        seriesStats.getData().add(new XYChart.Data<>("XRP", 45));
        statsChart.getData().add(seriesStats);
    }

    private void updateStatistics() {
        labelTotalAssets.setText(String.valueOf(allAssets.size()));
        labelActiveAssets.setText(String.valueOf(allAssets.stream().filter(a -> a.getStatus() == Status.ACTIVE).count()));
        labelTotalTrades.setText(String.valueOf(allTrades.size()));
        labelActiveTrades.setText(String.valueOf(allTrades.stream().filter(t -> t.getStatus() == Status.PENDING).count()));
    }
}
