package tn.esprit.Champions.gui.fournisseur;

import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import org.controlsfx.control.Notifications;
import tn.esprit.Champions.models.Product;
import tn.esprit.Champions.models.ProductCategory;
import tn.esprit.Champions.models.ProductStatus;
import tn.esprit.Champions.services.ProductService;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class FournisseurDashboardController {

    @FXML
    private TextField nameField;
    @FXML
    private TextArea descField;
    @FXML
    private TextField priceField;
    @FXML
    private Spinner<Integer> stockSpinner;
    @FXML
    private Label imageStatus;
    @FXML
    private TextField brandField;
    @FXML
    private ComboBox<ProductCategory> categoryCombo;
    @FXML
    private TextField discountPriceField;
    @FXML
    private Label nameError;
    @FXML
    private Label priceError;

    @FXML
    private TableView<Product> productsTable;
    @FXML
    private TableColumn<Product, String> colName;
    @FXML
    private TableColumn<Product, String> colPrice;
    @FXML
    private TableColumn<Product, String> colDiscount;
    @FXML
    private TableColumn<Product, String> colStock;
    @FXML
    private TableColumn<Product, String> colStatus;
    @FXML
    private TableColumn<Product, Void> colAction;

    private final ProductService productService = new ProductService();
    private String selectedImagePath = "";
    private File selectedFile = null;
    private Product editingProduct = null;
    @FXML
    private Button saveButton;

    @FXML
    // Initialiser le contrôleur et configurer les éléments de l'interface
    public void initialize() {
        stockSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 1000, 1));
        categoryCombo.getItems().setAll(ProductCategory.values());

        colName.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getName()));
        colPrice.setCellValueFactory(data -> new SimpleStringProperty("$" + data.getValue().getPrice()));
        colDiscount.setCellValueFactory(data -> new SimpleStringProperty("$" + data.getValue().getDiscountPrice()));
        colStock.setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(data.getValue().getStock())));
        colStatus.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getStatus().name()));

        setupActionColumn();
        refreshTable();
    }

    // Configurer la colonne des actions avec les boutons modifier et supprimer
    private void setupActionColumn() {
        colAction.setCellFactory(param -> new TableCell<>() {
            private final Button editBtn = new Button("Modifier");
            private final Button deleteBtn = new Button("Supprimer");
            private final javafx.scene.layout.HBox container = new javafx.scene.layout.HBox(10, editBtn, deleteBtn);

            {
                editBtn.getStyleClass().add("btn-secondary");
                deleteBtn.setStyle("-fx-background-color: #EF4444; -fx-text-fill: white; -fx-background-radius: 8;");

                editBtn.setOnAction(event -> {
                    Product p = getTableView().getItems().get(getIndex());
                    handleEdit(p);
                });

                deleteBtn.setOnAction(event -> {
                    Product p = getTableView().getItems().get(getIndex());
                    handleDelete(p);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : container);
            }
        });
    }

    // Gérer l'action de modification d'un produit
    private void handleEdit(Product p) {
        editingProduct = p;
        nameField.setText(p.getName());
        descField.setText(p.getDescription());
        priceField.setText(String.valueOf(p.getPrice()));
        discountPriceField.setText(String.valueOf(p.getDiscountPrice()));
        brandField.setText(p.getBrand());
        categoryCombo.setValue(p.getCategory());
        stockSpinner.getValueFactory().setValue(p.getStock());
        selectedImagePath = p.getImageUrl();
        selectedFile = null; // Reset selected file when editing existing product
        imageStatus
                .setText(selectedImagePath != null && !selectedImagePath.isEmpty() ? "Image chargée" : "Aucune image");
        saveButton.setText("Mettre à jour");
    }

    // Gérer l'action de suppression d'un produit avec confirmation
    private void handleDelete(Product p) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation de suppression");
        alert.setHeaderText("Supprimer " + p.getName() + " ?");
        alert.setContentText("Cette action est irréversible.");

        if (alert.showAndWait().get() == ButtonType.OK) {
            try {
                productService.deleteOne(p);
                Notifications.create().title("Supprimé").text("Produit supprimé avec succès.").showInformation();
                refreshTable();
            } catch (Exception e) {
                Notifications.create().title("Erreur").text("Impossible de supprimer le produit.").showError();
            }
        }
    }

    @FXML
    // Gérer le téléchargement d'une image pour le produit
    private void handleUploadImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choisir une image");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg"));
        File file = fileChooser.showOpenDialog(null);
        if (file != null) {
            selectedFile = file;
            selectedImagePath = file.getName();
            imageStatus.setText(file.getName());
        }
    }

    @FXML
    // Sauvegarder (ajouter ou mettre à jour) le produit dans la base de données
    private void handleSave() {
        if (!validateForm())
            return;

        try {
            Product p = (editingProduct != null) ? editingProduct : new Product();
            p.setName(nameField.getText());
            p.setDescription(descField.getText());
            p.setPrice(Double.parseDouble(priceField.getText()));
            p.setDiscountPrice(
                    discountPriceField.getText().isEmpty() ? 0 : Double.parseDouble(discountPriceField.getText()));
            p.setBrand(brandField.getText());
            p.setCategory(categoryCombo.getValue() != null ? categoryCombo.getValue() : ProductCategory.ELECTRONICS);
            p.setStock(stockSpinner.getValue());
            if (selectedFile != null) {
                try {
                    String uploadsDir = System.getProperty("user.dir") + "/uploads";
                    File dir = new File(uploadsDir);
                    if (!dir.exists())
                        dir.mkdirs();

                    String fileName = UUID.randomUUID().toString() + "_" + selectedFile.getName();
                    Path targetPath = Paths.get(uploadsDir, fileName);
                    Files.copy(selectedFile.toPath(), targetPath, StandardCopyOption.REPLACE_EXISTING);

                    p.setImageUrl(fileName); // Save only the filename
                } catch (IOException e) {
                    System.err.println("Failed to copy image: " + e.getMessage());
                }
            } else {
                p.setImageUrl(selectedImagePath);
            }

            p.setStatus(ProductStatus.AVAILABLE);
            p.setUserId(1);
            p.setUpdatedAt(LocalDateTime.now());

            if (editingProduct == null) {
                p.setAvgRating(0.0); // New products start with 0
            }

            if (editingProduct != null) {
                productService.updateOne(p);
                Notifications.create().title("Succès").text("Produit mis à jour !").showInformation();
            } else {
                p.setCreatedAt(LocalDateTime.now());
                productService.insertOne(p);
                Notifications.create().title("Succès").text("Produit ajouté !").showInformation();
            }

            clearForm();
            refreshTable();
        } catch (Exception e) {
            Notifications.create().title("Erreur")
                    .text("Erreur lors de la sauvegarde.").showError();
            e.printStackTrace();
        }
    }

    // Valider les champs du formulaire avant la sauvegarde
    private boolean validateForm() {
        boolean valid = true;

        if (nameField.getText().isEmpty()) {
            nameField.getStyleClass().add("field-error");
            nameError.setText("Le nom est requis");
            nameError.setVisible(true);
            valid = false;
        } else {
            nameField.getStyleClass().remove("field-error");
            nameError.setVisible(false);
        }

        try {
            double price = Double.parseDouble(priceField.getText());
            if (price <= 0)
                throw new NumberFormatException();
            priceField.getStyleClass().remove("field-error");
            priceError.setVisible(false);

            String discountText = discountPriceField.getText();
            if (discountText != null && !discountText.isEmpty()) {
                double discount = Double.parseDouble(discountText);
                if (discount < 0) {
                    discountPriceField.getStyleClass().add("field-error");
                    valid = false;
                } else if (discount > price) {
                    discountPriceField.getStyleClass().add("field-error");
                    Notifications.create()
                            .title("Erreur de prix")
                            .text("Le prix promotionnel ne peut pas dépasser le prix initial.")
                            .showError();
                    valid = false;
                } else {
                    discountPriceField.getStyleClass().remove("field-error");
                }
            }
        } catch (NumberFormatException e) {
            priceField.getStyleClass().add("field-error");
            priceError.setText("Prix invalide");
            priceError.setVisible(true);
            valid = false;
        }

        return valid;
    }

    // Vider le formulaire après l'enregistrement ou pour un nouvel ajout
    private void clearForm() {
        nameField.clear();
        descField.clear();
        priceField.clear();
        discountPriceField.clear();
        brandField.clear();
        categoryCombo.setValue(null);
        stockSpinner.getValueFactory().setValue(1);
        selectedImagePath = "";
        selectedFile = null;
        imageStatus.setText("Aucune image choisie");
        editingProduct = null;
        saveButton.setText("Enregistrer le Produit");
    }

    // Actualiser la table des produits avec les données de la base de données
    private void refreshTable() {
        try {
            List<Product> products = productService.SelectAll();
            if (products != null) {
                productsTable.getItems().setAll(products);
            }
        } catch (Exception e) {
            System.err.println("Database connection failed during refresh.");
        }
    }
}
