package tn.esprit.Champions.gui.client;

import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import org.controlsfx.control.Notifications;
import tn.esprit.Champions.models.Order;
import tn.esprit.Champions.models.OrderItem;
import tn.esprit.Champions.models.OrderStatus;
import tn.esprit.Champions.services.OrderItemService;
import tn.esprit.Champions.services.OrderService;
import tn.esprit.Champions.services.ProductService;
import tn.esprit.Champions.utils.ShoppingCart;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

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

    private final ProductService productService = new ProductService();
    private final OrderService orderService = new OrderService();
    private final OrderItemService orderItemService = new OrderItemService();

    @FXML
    // Initialiser le contrôleur, configurer les colonnes et les actions
    public void initialize() {
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

        colPrice.setCellValueFactory(data -> new SimpleStringProperty("$" + data.getValue().getUnitPrice()));
        colTotal.setCellValueFactory(data -> new SimpleStringProperty("$" + data.getValue().getSubTotal()));

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
        item.setSubTotal(item.getUnitPrice() * newQty);
        updateTable();
    }

    // Mettre à jour la table du panier et le montant total
    private void updateTable() {
        cartTable.getItems().setAll(ShoppingCart.getInstance().getItems());
        cartTable.refresh();
        totalLabel.setText(String.format("$%.2f", ShoppingCart.getInstance().getTotal()));
    }

    @FXML
    // Gérer le processus de commande (popup logic)
    private void handleCheckout() {
        if (ShoppingCart.getInstance().getItems().isEmpty()) {
            Notifications.create().title("Attention").text("Votre panier est vide").showWarning();
            return;
        }

        // --- Custom Popup Dialog ---
        Dialog<javafx.util.Pair<String, String>> dialog = new Dialog<>();
        dialog.setTitle("Informations de Livraison");
        dialog.setHeaderText("Veuillez entrer vos coordonnées pour finaliser la commande");

        ButtonType confirmButtonType = new ButtonType("Confirmer la commande", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(confirmButtonType, ButtonType.CANCEL);

        javafx.scene.layout.GridPane grid = new javafx.scene.layout.GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new javafx.geometry.Insets(20, 150, 10, 10));

        TextField addressField = new TextField();
        addressField.setPromptText("Adresse de livraison");
        TextField phoneField = new TextField();
        phoneField.setPromptText("Numéro de téléphone");

        grid.add(new Label("Adresse:"), 0, 0);
        grid.add(addressField, 1, 0);
        grid.add(new Label("Téléphone:"), 0, 1);
        grid.add(phoneField, 1, 1);
        grid.add(new Label("Paiement:"), 0, 2);
        grid.add(new Label("CRYPTO (Automatique)"), 1, 2);

        dialog.getDialogPane().setContent(grid);

        // Convert the result to address/phone pair when the confirm button is clicked.
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == confirmButtonType) {
                return new javafx.util.Pair<>(addressField.getText(), phoneField.getText());
            }
            return null;
        });

        Optional<javafx.util.Pair<String, String>> result = dialog.showAndWait();

        result.ifPresent(details -> {
            String address = details.getKey();
            String phone = details.getValue();

            if (address.isEmpty() || phone.isEmpty()) {
                Notifications.create().title("Erreur").text("Veuillez remplir tous les champs").showError();
                return;
            }

            try {
                // 1. Validate stock one last time and decrement
                for (OrderItem item : ShoppingCart.getInstance().getItems()) {
                    productService.decrementStock(item.getProduct().getId(), item.getQuantity());
                }

                // 2. Create the Order object
                Order order = new Order();
                order.setUserId(2); // Static user ID for now
                order.setOrderDate(LocalDateTime.now());
                order.setTotalAmount(ShoppingCart.getInstance().getTotal());
                order.setStatus(OrderStatus.PAID);
                order.setShippingAddress(address);
                order.setPaymentMethod("CRYPTO");
                order.setPhoneNumber(phone);

                // 3. Save Order to database
                orderService.insertOne(order);

                // 4. Save each OrderItem to database
                for (OrderItem item : ShoppingCart.getInstance().getItems()) {
                    item.setOrder(order);
                    orderItemService.insertOne(item);
                }

                Notifications.create()
                        .title("Succès")
                        .text("Commande passée avec succès !")
                        .showConfirm();

                ShoppingCart.getInstance().clear();
                updateTable();

            } catch (Exception e) {
                Notifications.create().title("Erreur").text("Erreur lors de la commande : " + e.getMessage())
                        .showError();
                e.printStackTrace();
            }
        });
    }
}
