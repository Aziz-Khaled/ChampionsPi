package tn.esprit.Champions.gui.client;

import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
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
    private final ProductService productService = new ProductService();

    @FXML
    // Initialiser en chargeant la liste des produits
    public void initialize() {
        loadProducts();
    }

    // Charger les produits depuis la base de données et les afficher dans la grille
    private void loadProducts() {
        try {
            List<Product> products = productService.SelectAll();
            if (products == null || products.isEmpty()) {
                System.out.println("Aucun produit trouvé ou connexion échouée.");
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
        } catch (Exception e) {
            Notifications.create()
                    .title("Erreur de Connexion")
                    .text("Impossible de charger les produits. Vérifiez votre base de données.")
                    .showError();
            e.printStackTrace();
        }
    }

    // Créer une carte d'affichage pour un produit donné
    private VBox createProductCard(Product product) {
        VBox card = new VBox(8);
        card.getStyleClass().add("card");
        card.setPrefWidth(220);
        card.setAlignment(Pos.CENTER);

        ImageView imageView = new ImageView();
        try {
            if (product.getImageUrl() != null && !product.getImageUrl().isEmpty()) {
                String imagePath = System.getProperty("user.dir") + "/uploads/" + product.getImageUrl();
                java.io.File imageFile = new java.io.File(imagePath);
                if (imageFile.exists()) {
                    imageView.setImage(new Image(imageFile.toURI().toString(), true));
                } else {
                    // Try treating it as a full URL for backward compatibility
                    imageView.setImage(new Image(product.getImageUrl(), true));
                }
            }
        } catch (Exception e) {
            // Placeholder can be handled here
        }
        imageView.setFitHeight(130);
        imageView.setFitWidth(190);
        imageView.setPreserveRatio(true);

        Label brandLabel = new Label(product.getBrand() != null ? product.getBrand().toUpperCase() : "MARQUE");
        brandLabel.getStyleClass().add("card-brand");

        Label nameLabel = new Label(product.getName());
        nameLabel.getStyleClass().add("card-title");
        nameLabel.setWrapText(true);

        Label ratingLabel = new Label(product.getAvgRating() > 0 ? "⭐ " + product.getAvgRating() : "");
        ratingLabel.getStyleClass().add("card-rating");

        Label stockLabel = new Label("En stock: " + product.getStock());
        stockLabel.setStyle("-fx-text-fill: #6B7280; -fx-font-size: 12px;");

        javafx.scene.layout.HBox priceBox = new javafx.scene.layout.HBox(10);
        priceBox.setAlignment(Pos.CENTER);

        if (product.getDiscountPrice() > 0) {
            Label oldPrice = new Label("$" + product.getPrice());
            oldPrice.getStyleClass().add("price-old");
            oldPrice.setStyle("-fx-strikethrough: true; -fx-text-fill: #EF4444;"); // Red strikethrough
            Label newPrice = new Label("$" + product.getDiscountPrice());
            newPrice.getStyleClass().add("card-price");
            priceBox.getChildren().addAll(oldPrice, newPrice);
        } else {
            Label priceLabel = new Label("$" + product.getPrice());
            priceLabel.getStyleClass().add("card-price");
            priceBox.getChildren().add(priceLabel);
        }

        Button addToCartBtn = new Button();
        addToCartBtn.setMaxWidth(Double.MAX_VALUE);

        if (product.getStock() <= 0) {
            addToCartBtn.setText("Bientôt disponible");
            addToCartBtn.setDisable(true);
            addToCartBtn.getStyleClass().add("btn-disabled");
        } else {
            addToCartBtn.setText("Ajouter");
            addToCartBtn.getStyleClass().add("btn-primary");
            addToCartBtn.setOnAction(e -> handleAddToCart(product));
        }

        VBox.setMargin(addToCartBtn, new javafx.geometry.Insets(10, 0, 0, 0));
        card.getChildren().addAll(imageView, brandLabel, nameLabel, ratingLabel, stockLabel, priceBox, addToCartBtn);
        return card;
    }

    // Gérer l'ajout d'un produit au panier
    private void handleAddToCart(Product product) {
        ShoppingCart.getInstance().addProduct(product, 1);
        Notifications.create()
                .title("Succès")
                .text(product.getName() + " ajouté au panier !")
                .showInformation();
    }
}
