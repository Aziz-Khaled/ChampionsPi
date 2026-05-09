package tn.esprit.Champions.gui.client;

import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.controlsfx.control.Notifications;
import tn.esprit.Champions.models.*;
import tn.esprit.Champions.services.OrderItemService;
import tn.esprit.Champions.services.OrderService;
import tn.esprit.Champions.services.ProductService;
import tn.esprit.Champions.services.TransactionService;
import tn.esprit.Champions.services.WalletService;
import tn.esprit.Champions.services.wallet_currencyService;
import tn.esprit.Champions.models.wallet_currency;
import tn.esprit.Champions.utils.ShoppingCart;

import javafx.application.Platform;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.geometry.Pos;
import tn.esprit.Champions.utils.UserSession;

import java.time.LocalDateTime;
import java.util.List;

public class CartController {

    @FXML
    private TableView<OrderItem> cartTable;

    @FXML
    private TableColumn<OrderItem, String> colProduct;

    @FXML
    private TableColumn<OrderItem, String> colQuantity;

    @FXML
    private TableColumn<OrderItem, String> colPrice;

    @FXML
    private TableColumn<OrderItem, String> colTotal;

    @FXML
    private TableColumn<OrderItem, Void> colAction;

    @FXML
    private Label totalLabel;

    @FXML
    private HBox aiRecommendationBox;

    private final ProductService productService = new ProductService();

    private final OrderService orderService = new OrderService();

    private final OrderItemService orderItemService = new OrderItemService();

    private final TransactionService transactionService = new TransactionService();

    private final WalletService walletService = new WalletService();

    private final wallet_currencyService walletCurrencyService = new wallet_currencyService();

    private final tn.esprit.Champions.services.PdfService pdfService = new tn.esprit.Champions.services.PdfService();

    private final tn.esprit.Champions.services.GeminiService geminiService = new tn.esprit.Champions.services.GeminiService();

    private final tn.esprit.Champions.services.EmailService emailService = new tn.esprit.Champions.services.EmailService();

    @FXML
    // Initialiser le contrôleur, configurer les colonnes et les actions
    public void initialize() {
        // ... previous initialization ...
        colProduct.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getProduct().getName()));

        // Custom cell for Quantity with +/- buttons
        colQuantity.setCellFactory(param -> new TableCell<>() {
            private final Button btnMinus = new Button("-");
            private final Button btnPlus = new Button("+");
            private final Label qtyLabel = new Label();
            private final javafx.scene.layout.HBox pane = new javafx.scene.layout.HBox(5, btnMinus, qtyLabel, btnPlus);

            {
                pane.setAlignment(javafx.geometry.Pos.CENTER);
                btnMinus.getStyleClass().add("btn-secondary");
                btnPlus.getStyleClass().add("btn-secondary");
                btnMinus.setPrefWidth(30);
                btnPlus.setPrefWidth(30);

                btnMinus.setOnAction(event -> {
                    OrderItem item = getTableView().getItems().get(getIndex());
                    updateItemQuantity(item, item.getQuantity() - 1);
                });

                btnPlus.setOnAction(event -> {
                    OrderItem item = getTableView().getItems().get(getIndex());
                    updateItemQuantity(item, item.getQuantity() + 1);
                });
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    OrderItem orderItem = getTableView().getItems().get(getIndex());
                    qtyLabel.setText(String.valueOf(orderItem.getQuantity()));
                    setGraphic(pane);
                }
            }
        });

        colPrice.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getUnitPrice() + " BTC"));
        colTotal.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getSubTotal() + " BTC"));

        // Simple remove button logic
        colAction.setCellFactory(param -> new TableCell<>() {
            private final Button btn = new Button("Supprimer");
            {
                btn.getStyleClass().add("btn-secondary");
                btn.setStyle("-fx-border-color: #EF4444; -fx-text-fill: #EF4444;");
                btn.setOnAction(event -> {
                    OrderItem item = getTableView().getItems().get(getIndex());
                    ShoppingCart.getInstance().removeItem(item);
                    updateTable();
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btn);
            }
        });

        updateTable();
    }

    private void updateItemQuantity(OrderItem item, int newQty) {
        if (newQty < 1)
            return;

        // Check stock
        if (newQty > item.getProduct().getStock()) {
            Notifications.create()
                    .title("Stock Insuffisant")
                    .text("Désolé, seulement " + item.getProduct().getStock() + " articles disponibles.")
                    .showWarning();
            return;
        }

        item.setQuantity(newQty);
        item.setSubTotal(item.getUnitPrice().multiply(java.math.BigDecimal.valueOf(newQty)));
        updateTable();
    }

    // Mettre à jour la table du panier et le montant total
    private void updateTable() {
        cartTable.getItems().setAll(ShoppingCart.getInstance().getItems());
        cartTable.refresh();
        totalLabel.setText(String.format("%.8f BTC", ShoppingCart.getInstance().getTotal()));
    }

    @FXML
    // Gérer le processus de commande (popup logic)
    private void handleCheckout() {
        if (ShoppingCart.getInstance().getItems().isEmpty()) {
            Notifications.create().title("Attention").text("Votre panier est vide").showWarning();
            return;
        }

        try {
            // Load the custom Premium Checkout Modal
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                    getClass().getResource("/views/client/CheckoutModal.fxml"));
            javafx.scene.Parent root = loader.load();

            CheckoutController controller = loader.getController();
            controller.setTotalAmount(ShoppingCart.getInstance().getTotal().doubleValue());

            Stage stage = new Stage();
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            stage.setTitle("Finaliser Commande - Fintech BTC");
            stage.setScene(new javafx.scene.Scene(root));
            stage.showAndWait();

            if (!controller.isConfirmed()) {
                return;
            }

            String address = controller.getAddress();
            String phone = controller.getPhone();

            // 1. Direct Payment Notification
            Notifications.create()
                    .title("Paiement Direct")
                    .text("Commande de " + ShoppingCart.getInstance().getTotal() + " BTC validée.")
                    .showInformation();

            // 2. Validate stock and decrement
            for (OrderItem item : ShoppingCart.getInstance().getItems()) {
                productService.decrementStock(item.getProduct().getId(), item.getQuantity());
            }

            // 3. Create the Order object
            Order order = new Order();
            Utilisateur currentUser = UserSession.getLoggedInUser();
            if (currentUser != null) {
                order.setUserId(currentUser.getId_user());
            } else {
                order.setUserId(2); // Fallback user ID
            }
            order.setOrderDate(LocalDateTime.now());
            order.setTotalAmount(ShoppingCart.getInstance().getTotal());
            order.setStatus(OrderStatus.PAID);
            order.setShippingAddress(address);
            order.setPaymentMethod("Direct Payment");
            order.setPhoneNumber(phone);

            // 4. Save Order to database
            orderService.insertOne(order);

            // 5. Save each OrderItem to database
            List<OrderItem> currentItems = new java.util.ArrayList<>(ShoppingCart.getInstance().getItems());
            for (OrderItem item : currentItems) {
                item.setOrder(order);
                orderItemService.insertOne(item);
            }

            // 6. Direct Payment successful
            System.out.println("✅ Commande validée directement (Paiement Direct).");

            // 7. Generate PDF Receipt
            String uploadsDir = System.getProperty("user.dir") + "/uploads/receipts";
            java.io.File dir = new java.io.File(uploadsDir);
            if (!dir.exists())
                dir.mkdirs();

            String filePath = uploadsDir + "/receipt_order_" + order.getId() + ".pdf";
            pdfService.generateReceipt(order, currentItems, filePath);

            // 8. Show Success Popup with QR Code
            String qrContent = "Order ID: " + order.getId() + "\nTotal: " + order.getTotalAmount()
                    + " BTC\nStatus: PAID";
            byte[] qrImageData = pdfService.generateQRCodeImage(qrContent);
            javafx.scene.image.Image qrFxImage = new javafx.scene.image.Image(
                    new java.io.ByteArrayInputStream(qrImageData));
            javafx.scene.image.ImageView qrView = new javafx.scene.image.ImageView(qrFxImage);
            qrView.setFitWidth(200);
            qrView.setFitHeight(200);

            Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
            successAlert.setTitle("Commande Réussie - Fintech BTC");
            successAlert.setHeaderText("Votre paiement BTC a été confirmé !");

            javafx.scene.layout.VBox alertContent = new javafx.scene.layout.VBox(10);
            alertContent.setAlignment(javafx.geometry.Pos.CENTER);
            alertContent.getChildren().addAll(
                    new Label("Montant Total : " + order.getTotalAmount() + " BTC"),
                    new Label("Scannez ce QR Code pour voir les détails :"),
                    qrView,
                    new Label("Le reçu PDF va s'ouvrir automatiquement."));

            successAlert.getDialogPane().setContent(alertContent);
            successAlert.show();

            // 8. Open PDF automatically
            try {
                java.io.File pdfFile = new java.io.File(filePath);
                if (java.awt.Desktop.isDesktopSupported()) {
                    java.awt.Desktop.getDesktop().open(pdfFile);
                }
            } catch (Exception ex) {
                System.err.println("Impossible d'ouvrir le PDF : " + ex.getMessage());
            }

            // Send Email Notification
            String orderDetails = ShoppingCart.getInstance().getItems().stream()
                    .map(item -> "- " + item.getProduct().getName() + " x" + item.getQuantity())
                    .collect(java.util.stream.Collectors.joining("\n"));

            if (currentUser != null && currentUser.getEmail() != null && !currentUser.getEmail().isEmpty()) {
                String recipientEmail = currentUser.getEmail();

                new Thread(() -> {
                    emailService.sendOrderConfirmation(recipientEmail, orderDetails, order.getTotalAmount().doubleValue());
                }).start();
            } else {
                System.err.println("Impossible d'envoyer l'email : aucun utilisateur connecté ou email vide");
            };

            ShoppingCart.getInstance().clear();
            updateTable();

        } catch (Exception e) {
            Notifications.create().title("Erreur").text("Erreur lors de la commande : " + e.getMessage())
                    .showError();
            e.printStackTrace();
        }
    }

    @FXML
    private void handleAIRecommendations() {
        if (ShoppingCart.getInstance().getItems().isEmpty()) {
            Notifications.create().title("Panier vide")
                    .text("Ajoutez des produits pour recevoir des recommandations IA.").showWarning();
            return;
        }

        aiRecommendationBox.getChildren().clear();
        aiRecommendationBox.getChildren().add(new Label("L'IA analyse votre panier..."));

        new Thread(() -> {
            try {
                List<Product> allProducts = productService.SelectAll();
                List<Integer> recIds = geminiService.getRecommendedProductIds(ShoppingCart.getInstance().getItems(),
                        allProducts);

                List<Product> recommendedProducts = allProducts.stream()
                        .filter(p -> recIds.contains(p.getId()))
                        .collect(java.util.stream.Collectors.toList());

                Platform.runLater(() -> renderRecommendationCards(recommendedProducts));
            } catch (Exception e) {
                Platform.runLater(() -> {
                    aiRecommendationBox.getChildren().clear();
                    Label errorLabel = new Label("Erreur IA: " + e.getMessage());
                    errorLabel.setStyle("-fx-text-fill: #EF4444;");
                    aiRecommendationBox.getChildren().add(errorLabel);
                });
            }
        }).start();
    }

    private void renderRecommendationCards(List<Product> products) {
        aiRecommendationBox.getChildren().clear();
        aiRecommendationBox.getChildren().add(new Label("Recommandations pour vous :"));

        if (products.isEmpty()) {
            aiRecommendationBox.getChildren().add(new Label("Aucune recommandation pour le moment."));
            return;
        }

        for (Product p : products) {
            VBox card = new VBox(10);
            card.getStyleClass().add("card");
            card.setPrefWidth(200);
            card.setAlignment(Pos.CENTER);
            card.setStyle(
                    "-fx-background-color: #f8fafc; " +
                            "-fx-padding: 15; " +
                            "-fx-border-color: #e2e8f0; " +
                            "-fx-border-radius: 12; " +
                            "-fx-background-radius: 12; " +
                            "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.05), 10, 0, 0, 5);");

            // Product Image (if exists)
            javafx.scene.image.ImageView imgView = new javafx.scene.image.ImageView();
            try {
                if (p.getImageUrl() != null && !p.getImageUrl().isEmpty()) {
                    String imgPath = p.getImageUrl();
                    if (!imgPath.startsWith("http") && !imgPath.startsWith("file")) {
                        imgPath = "file:" + System.getProperty("user.dir") + "/" + imgPath;
                    }
                    imgView.setImage(new javafx.scene.image.Image(imgPath, 100, 100, true, true));
                }
            } catch (Exception e) {
                System.err.println("Error loading image for card: " + e.getMessage());
            }
            imgView.setFitWidth(100);
            imgView.setFitHeight(100);

            Label name = new Label(p.getName());
            name.setStyle("-fx-font-weight: bold; -fx-font-size: 14; -fx-text-fill: #1e293b;");
            name.setWrapText(true);
            name.setMaxWidth(180);
            name.setAlignment(Pos.CENTER);

            Label price = new Label(String.format("%.8f BTC", p.getPrice()));
            price.setStyle("-fx-text-fill: #10b981; -fx-font-weight: bold; -fx-font-size: 12;");

            Button addBtn = new Button("Ajouter au panier");
            addBtn.getStyleClass().add("btn-primary");
            addBtn.setStyle("-fx-font-size: 11; -fx-cursor: hand;");
            addBtn.setOnAction(e -> {
                ShoppingCart.getInstance().addProduct(p, 1);
                updateTable();
                Notifications.create().title("Succès").text(p.getName() + " ajouté au panier").showInformation();
            });

            card.getChildren().addAll(imgView, name, price, addBtn);
            aiRecommendationBox.getChildren().add(card);
        }
    }
}