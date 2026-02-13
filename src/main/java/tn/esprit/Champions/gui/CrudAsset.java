package tn.esprit.Champions.gui;


import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import tn.esprit.Champions.models.*;
import tn.esprit.Champions.services.AssetService;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

public class CrudAsset {






    @FXML private TextField txtSymbol;
    @FXML private TextField txtName;
    @FXML private ComboBox<AssetType> boxType;
    @FXML private ComboBox<Market> boxMarket;
    @FXML private TextField txtCurrentPrice;
    @FXML private ComboBox<Status> boxStatus;


    @FXML private Button btnAdd;
    @FXML private Button btnUpdate;
    @FXML private Button btnClear;
    @FXML private Button btnDelete;


    @FXML private TableView<Asset> table;
    @FXML private TableColumn<Asset, Integer> id_asset;
    @FXML private TableColumn<Asset, String> symbol;
    @FXML private TableColumn<Asset, String> name;
    @FXML private TableColumn<Asset, AssetType> type;
    @FXML private TableColumn<Asset, Market> market;
    @FXML private TableColumn<Asset, Double> price;
    @FXML private TableColumn<Asset, Status> status;
    @FXML private TableColumn<Asset, LocalDateTime> created;
    @FXML private TableColumn<Asset, LocalDateTime> updated;
    @FXML private TableColumn<Asset, Void> colAction;


    @FXML private TextField txtSearch;
    @FXML private Button btnClearSearch;
    @FXML private Label labelStatus;
    @FXML private Label labelCount;


    private AssetService assetService = new AssetService();
    private List<Asset> allAssets;
    private Asset currentAsset = null;

    @FXML
    public void initialize() {
        initializeComboBoxes();
        initializeTableColumns();
        loadAssets();
        addActionButtons();
        setupTableRowSelection();
    }



    private void initializeComboBoxes() {
        boxType.getItems().setAll(AssetType.values());
        boxMarket.getItems().setAll(Market.values());
        boxStatus.getItems().setAll(Status.values());
    }

    private void initializeTableColumns() {
        id_asset.setCellValueFactory(new PropertyValueFactory<>("id"));
        symbol.setCellValueFactory(new PropertyValueFactory<>("symbol"));
        name.setCellValueFactory(new PropertyValueFactory<>("name"));
        type.setCellValueFactory(new PropertyValueFactory<>("type"));
        market.setCellValueFactory(new PropertyValueFactory<>("market"));
        price.setCellValueFactory(new PropertyValueFactory<>("currentPrice"));
        status.setCellValueFactory(new PropertyValueFactory<>("status"));
        created.setCellValueFactory(new PropertyValueFactory<>("createdAt"));
        updated.setCellValueFactory(new PropertyValueFactory<>("updatedAt"));
    }

    private void loadAssets() {
        try {
            allAssets = assetService.SelectAll();
            refreshTableView();
            updateStatusLabel();
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Database Error", "Failed to load assets: " + e.getMessage());
        }
    }



    @FXML
    private void handleAdd() {
        try {
            if (!validateForm()) {
                return;
            }

            Asset newAsset = new Asset(
                    0,
                    txtSymbol.getText().trim(),
                    txtName.getText().trim(),
                    boxType.getValue(),
                    boxMarket.getValue(),
                    Double.parseDouble(txtCurrentPrice.getText()),
                    boxStatus.getValue(),
                    LocalDateTime.now(),
                    LocalDateTime.now(),
                    1
            );

            assetService.insertOne(newAsset);
            showAlert(Alert.AlertType.INFORMATION, "Success", "Asset added successfully!");
            loadAssets();
            clearForm();
            currentAsset = null;

        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Validation Error", "Price must be a valid number");
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Database Error", e.getMessage());
        }
    }

    @FXML
    private void handleUpdate() {
        try {
            if (currentAsset == null) {
                showAlert(Alert.AlertType.WARNING, "Warning", "Please select an asset to update");
                return;
            }

            if (!validateForm()) {
                return;
            }

            currentAsset.setSymbol(txtSymbol.getText().trim());
            currentAsset.setName(txtName.getText().trim());
            currentAsset.setType(boxType.getValue());
            currentAsset.setMarket(boxMarket.getValue());
            currentAsset.setCurrentPrice(Double.parseDouble(txtCurrentPrice.getText()));
            currentAsset.setStatus(boxStatus.getValue());
            currentAsset.setUpdatedAt(LocalDateTime.now());

            assetService.updateOne(currentAsset);
            showAlert(Alert.AlertType.INFORMATION, "Success", "Asset updated successfully!");
            loadAssets();
            clearForm();
            currentAsset = null;

        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Validation Error", "Price must be a valid number");
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Database Error", e.getMessage());
        }
    }

    @FXML
    private void handleDelete() {
        try {
            if (currentAsset == null) {
                showAlert(Alert.AlertType.WARNING, "Warning", "Please select an asset to delete");
                return;
            }

            Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
            confirmAlert.setTitle("Confirm Delete");
            confirmAlert.setHeaderText("Delete Asset: " + currentAsset.getSymbol());
            confirmAlert.setContentText("Are you sure you want to delete this asset?");

            if (confirmAlert.showAndWait().get() == ButtonType.OK) {
                assetService.deleteOne(currentAsset);
                showAlert(Alert.AlertType.INFORMATION, "Success", "Asset deleted successfully!");
                loadAssets();
                clearForm();
                currentAsset = null;
            }

        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Database Error", e.getMessage());
        }
    }

    @FXML
    private void handleClear() {
        clearForm();
        currentAsset = null;
        table.getSelectionModel().clearSelection();
    }


    @FXML
    private void handleSearch() {
        String searchText = txtSearch.getText().toLowerCase().trim();

        if (searchText.isEmpty()) {
            refreshTableView();
        } else {
            List<Asset> filteredAssets = allAssets.stream()
                    .filter(asset -> asset.getSymbol().toLowerCase().contains(searchText)
                            || asset.getName().toLowerCase().contains(searchText)
                            || asset.getType().toString().toLowerCase().contains(searchText)
                            || asset.getMarket().toString().toLowerCase().contains(searchText))
                    .collect(Collectors.toList());

            table.getItems().clear();
            table.getItems().addAll(filteredAssets);
        }

        updateStatusLabel();
    }

    @FXML
    private void handleClearSearch() {
        txtSearch.clear();
        refreshTableView();
    }



    private void setupTableRowSelection() {
        table.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                populateFormWithAsset(newVal);
            }
        });
    }

    private void populateFormWithAsset(Asset asset) {
        currentAsset = asset;
        txtSymbol.setText(asset.getSymbol());
        txtName.setText(asset.getName());
        boxType.setValue(asset.getType());
        boxMarket.setValue(asset.getMarket());
        txtCurrentPrice.setText(String.valueOf(asset.getCurrentPrice()));
        boxStatus.setValue(asset.getStatus());
    }

    private void addActionButtons() {
        colAction.setCellFactory(param -> new TableCell<>() {

            private final Button btnEdit = new Button("✏️");
            private final Button btnDelete = new Button("🗑");

            {
                btnEdit.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-padding: 6 10 6 10; -fx-font-size: 11px;");
                btnDelete.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-padding: 6 10 6 10; -fx-font-size: 11px;");

                btnEdit.setOnAction(event -> {
                    Asset asset = getTableView().getItems().get(getIndex());
                    table.getSelectionModel().select(asset);
                    populateFormWithAsset(asset);
                });

                btnDelete.setOnAction(event -> {
                    Asset asset = getTableView().getItems().get(getIndex());
                    deleteAssetDirectly(asset);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);

                if (empty) {
                    setGraphic(null);
                } else {
                    HBox box = new HBox(6, btnEdit, btnDelete);
                    box.setPadding(new Insets(4));
                    setGraphic(box);
                }
            }
        });
    }

    private void deleteAssetDirectly(Asset asset) {
        try {
            Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
            confirmAlert.setTitle("Confirm Delete");
            confirmAlert.setHeaderText("Delete Asset: " + asset.getSymbol());
            confirmAlert.setContentText("Are you sure?");

            if (confirmAlert.showAndWait().get() == ButtonType.OK) {
                assetService.deleteOne(asset);
                showAlert(Alert.AlertType.INFORMATION, "Success", "Asset deleted successfully!");
                loadAssets();
                if (currentAsset != null && currentAsset.getId() == asset.getId()) {
                    clearForm();
                    currentAsset = null;
                }
            }

        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Database Error", e.getMessage());
        }
    }

    // ==================== HELPER METHODS ====================

    private boolean validateForm() {
        if (txtSymbol.getText().trim().isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Validation Error", "Symbol is required");
            return false;
        }

        if (txtName.getText().trim().isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Validation Error", "Name is required");
            return false;
        }

        if (txtCurrentPrice.getText().trim().isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Validation Error", "Price is required");
            return false;
        }

        if (boxType.getValue() == null) {
            showAlert(Alert.AlertType.ERROR, "Validation Error", "Type is required");
            return false;
        }

        if (boxMarket.getValue() == null) {
            showAlert(Alert.AlertType.ERROR, "Validation Error", "Market is required");
            return false;
        }

        if (boxStatus.getValue() == null) {
            showAlert(Alert.AlertType.ERROR, "Validation Error", "Status is required");
            return false;
        }

        return true;
    }

    private void clearForm() {
        txtSymbol.clear();
        txtName.clear();
        txtCurrentPrice.clear();
        boxType.setValue(null);
        boxMarket.setValue(null);
        boxStatus.setValue(null);
    }

    private void refreshTableView() {
        table.getItems().clear();
        table.getItems().addAll(allAssets);
    }

    private void updateStatusLabel() {
        int displayed = table.getItems().size();
        int total = allAssets.size();
        labelStatus.setText("Showing " + displayed + " of " + total + " assets");
        labelCount.setText("Total: " + total);
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}