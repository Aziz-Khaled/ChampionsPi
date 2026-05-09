package tn.esprit.Champions.gui.fournisseur;

import javafx.fxml.FXML;
import javafx.scene.chart.PieChart;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import tn.esprit.Champions.models.Product;
import tn.esprit.Champions.services.ProductService;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class StatsModalController {

    @FXML
    private Label totalProductsLabel;
    @FXML
    private Label totalValueLabel;
    @FXML
    private PieChart categoryChart;
    @FXML
    private javafx.scene.chart.BarChart<String, Number> stockBarChart;

    private final ProductService productService = new ProductService();

    @FXML
    public void initialize() {
        try {
            List<Product> products = productService.SelectAll();
            if (products != null) {
                updateStats(products);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateStats(List<Product> products) {
        totalProductsLabel.setText(String.valueOf(products.size()));

        java.math.BigDecimal totalValue = products.stream()
                .map(p -> p.getPrice().multiply(java.math.BigDecimal.valueOf(p.getStock())))
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
        totalValueLabel.setText(String.format("$%.2f", totalValue.doubleValue()));

        Map<String, Long> categoryCounts = products.stream()
                .collect(Collectors.groupingBy(p -> p.getCategory().name(), Collectors.counting()));

        categoryChart.getData().clear();
        categoryCounts.forEach((cat, count) -> {
            categoryChart.getData().add(new PieChart.Data(cat + " (" + count + ")", count));
        });

        // Bar Chart: Stock Levels
        stockBarChart.getData().clear();
        javafx.scene.chart.XYChart.Series<String, Number> series = new javafx.scene.chart.XYChart.Series<>();
        series.setName("Stock");

        // Take top 10 products by stock to avoid overcrowding
        products.stream()
                .sorted((p1, p2) -> Integer.compare(p2.getStock(), p1.getStock()))
                .limit(10)
                .forEach(p -> {
                    series.getData().add(new javafx.scene.chart.XYChart.Data<>(p.getName(), p.getStock()));
                });

        stockBarChart.getData().add(series);
    }

    @FXML
    private void handleClose() {
        ((Stage) totalProductsLabel.getScene().getWindow()).close();
    }
}