package tn.esprit.Champions.gui.client;

import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.ComboBox;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import org.controlsfx.control.Notifications;
import tn.esprit.Champions.models.Product;
import tn.esprit.Champions.services.ProductService;
import tn.esprit.Champions.utils.ShoppingCart;

import java.util.List;

public class ClientDashboardController {

    @FXML
    private GridPane productGrid;
    @FXML
    private TextField searchField;
    @FXML
    private ComboBox<String> categoryFilter;

    private final ProductService productService = new ProductService();
    private List<Product> allProducts;

    @FXML
    public void initialize() {
        setupFilters();
        loadProducts();
    }

    private void setupFilters() {
        categoryFilter.getItems().add("Toutes les catégories");

        for (tn.esprit.Champions.models.ProductCategory cat :
                tn.esprit.Champions.models.ProductCategory.values()) {
            categoryFilter.getItems().add(cat.name());
        }

        categoryFilter.setValue("Toutes les catégories");

        searchField.textProperty().addListener((obs, oldVal, newVal) -> applyFilters());
        categoryFilter.valueProperty().addListener((obs, oldVal, newVal) -> applyFilters());
    }

    private void loadProducts() {
        try {
            allProducts = productService.SelectAll();
            applyFilters();
        } catch (Exception e) {
            Notifications.create()
                    .title("Erreur de Connexion")
                    .text("Impossible de charger les produits. Vérifiez votre base de données.")
                    .showError();
            e.printStackTrace();
        }
    }

    private void applyFilters() {

        if (allProducts == null) return;

        String searchText = searchField.getText() == null
                ? ""
                : searchField.getText().toLowerCase();

        String selectedCategory = categoryFilter.getValue();

        List<Product> filtered = allProducts.stream()

                .filter(p -> {

                    String name = p.getName() == null ? "" : p.getName().toLowerCase();
                    String brand = p.getBrand() == null ? "" : p.getBrand().toLowerCase();
                    String desc = p.getDescription() == null ? "" : p.getDescription().toLowerCase();

                    return name.contains(searchText)
                            || brand.contains(searchText)
                            || desc.contains(searchText);
                })

                .filter(p -> selectedCategory.equals("Toutes les catégories")
                        || (p.getCategory() != null
                        && p.getCategory().name().equals(selectedCategory)))

                .toList();

        displayProducts(filtered);
    }

    private void displayProducts(List<Product> products) {

        productGrid.getChildren().clear();

        if (products == null || products.isEmpty()) {
            System.out.println("Aucun produit trouvé.");
            return;
        }

        int column = 0;
        int row = 0;

        for (Product product : products) {
            VBox card = createProductCard(product);
            productGrid.add(card, column++, row);

            if (column == 4) {
                column = 0;
                row++;
            }
        }
    }

    private VBox createProductCard(Product product) {

        VBox card = new VBox(8);
        card.getStyleClass().add("card");
        card.setPrefWidth(220);
        card.setAlignment(Pos.CENTER);

        ImageView imageView = new ImageView();

        try {
            if (product.getImageUrl() != null && !product.getImageUrl().isEmpty()) {

                String imagePath = System.getProperty("user.dir")
                        + "/uploads/" + product.getImageUrl();

                java.io.File imageFile = new java.io.File(imagePath);

                if (imageFile.exists()) {
                    imageView.setImage(new Image(imageFile.toURI().toString(), true));
                } else {
                    imageView.setImage(new Image(product.getImageUrl(), true));
                }
            }
        } catch (Exception ignored) {}

        imageView.setFitHeight(130);
        imageView.setFitWidth(190);
        imageView.setPreserveRatio(true);

        Label brandLabel = new Label(
                product.getBrand() != null
                        ? product.getBrand().toUpperCase()
                        : "MARQUE"
        );
        brandLabel.getStyleClass().add("card-brand");

        Label nameLabel = new Label(product.getName());
        nameLabel.getStyleClass().add("card-title");
        nameLabel.setWrapText(true);

        Label ratingLabel = new Label(
                product.getAvgRating() > 0
                        ? "⭐ " + product.getAvgRating()
                        : ""
        );

        Label stockLabel = new Label("En stock: " + product.getStock());
        stockLabel.setStyle("-fx-text-fill: #6B7280; -fx-font-size: 12px;");

        javafx.scene.layout.HBox priceBox = new javafx.scene.layout.HBox(10);
        priceBox.setAlignment(Pos.CENTER);

        if (product.getDiscountPrice() != null && product.getDiscountPrice().compareTo(java.math.BigDecimal.ZERO) > 0) {

            Label oldPrice = new Label("BTC" + product.getPrice());
            oldPrice.setStyle("-fx-strikethrough: true; -fx-text-fill: #EF4444;");

            Label newPrice = new Label("BTC" + product.getDiscountPrice());
            newPrice.getStyleClass().add("card-price");

            priceBox.getChildren().addAll(oldPrice, newPrice);

        } else {

            Label priceLabel = new Label("BTC" + product.getPrice());
            priceLabel.getStyleClass().add("card-price");

            priceBox.getChildren().add(priceLabel);
        }

        Button addToCartBtn = new Button();
        addToCartBtn.setMaxWidth(Double.MAX_VALUE);

        if (product.getStock() <= 0) {
            addToCartBtn.setText("Bientôt disponible");
            addToCartBtn.setDisable(true);
        } else {
            addToCartBtn.setText("Ajouter");
            addToCartBtn.getStyleClass().add("btn-primary");
            addToCartBtn.setOnAction(e -> handleAddToCart(product));
        }

        card.getChildren().addAll(
                imageView,
                brandLabel,
                nameLabel,
                ratingLabel,
                stockLabel,
                priceBox,
                addToCartBtn
        );

        return card;
    }

    private void handleAddToCart(Product product) {

        ShoppingCart.getInstance().addProduct(product, 1);

        Notifications.create()
                .title("Succès")
                .text(product.getName() + " ajouté au panier !")
                .showInformation();
    }
}
