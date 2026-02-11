package tn.esprit.Champions.gui;


import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;

import java.sql.SQLException;
import java.time.LocalDateTime;

import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import tn.esprit.Champions.models.*;
import tn.esprit.Champions.services.AssetService;

public class CrudAsset {

    @FXML
    private TableView<Asset> table;

    @FXML
    private TableColumn<Asset, Integer> id_asset;
    @FXML
    private TableColumn<Asset, String> symbol;
    @FXML
    private TableColumn<Asset, String> name;
    @FXML
    private TableColumn<Asset, AssetType> type;
    @FXML
    private TableColumn<Asset, Market> market;
    @FXML
    private TableColumn<Asset, Double> price;
    @FXML
    private TableColumn<Asset, Status> status;
    @FXML
    private TableColumn<Asset, LocalDateTime> created;
    @FXML
    private TableColumn<Asset, LocalDateTime> updated;
    @FXML
    private TableColumn<Asset, Integer> user_id;
    @FXML
    private TableColumn<Asset, Void> colAction;

    private AssetService assetService = new AssetService();

    @FXML
    public void initialize() {
        initColumns();
        loadAssets();
        addActionButtons(); // ⚡ important
    }

    private void initColumns() {
        id_asset.setCellValueFactory(new PropertyValueFactory<>("id"));
        symbol.setCellValueFactory(new PropertyValueFactory<>("symbol"));
        name.setCellValueFactory(new PropertyValueFactory<>("name"));
        type.setCellValueFactory(new PropertyValueFactory<>("type"));
        market.setCellValueFactory(new PropertyValueFactory<>("market"));
        price.setCellValueFactory(new PropertyValueFactory<>("currentPrice"));
        status.setCellValueFactory(new PropertyValueFactory<>("status"));
        created.setCellValueFactory(new PropertyValueFactory<>("createdAt"));
        updated.setCellValueFactory(new PropertyValueFactory<>("updatedAt"));
        user_id.setCellValueFactory(new PropertyValueFactory<>("userId"));
    }

    private void loadAssets() {
        try {
            table.getItems().clear();
            table.getItems().addAll(assetService.SelectAll());
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void addActionButtons() {
        colAction.setCellFactory(param -> new TableCell<>() {

            private final Button btnEdit = new Button("✏️");
            private final Button btnDelete = new Button("🗑");

            {
                // Styles des boutons
                btnEdit.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");
                btnDelete.setStyle("-fx-background-color: #F44336; -fx-text-fill: white;");

                // Action Edit
                btnEdit.setOnAction(event -> {
                    Asset asset = getTableView().getItems().get(getIndex());
                    openEditDialog(asset);
                });

                // Action Delete
                btnDelete.setOnAction(event -> {
                    Asset asset = getTableView().getItems().get(getIndex());
                    deleteAsset(asset);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);

                if (empty) {
                    setGraphic(null);
                } else {

                    HBox box = new HBox(10, btnEdit, btnDelete);
                    setGraphic(box);
                }
            }
        });
    }

    private void deleteAsset(Asset asset) {
        try {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Confirmation");
            alert.setHeaderText(null);
            alert.setContentText("Supprimer cet asset ?");

            if (alert.showAndWait().get() == ButtonType.OK) {
                assetService.deleteOne(asset);  // passe l'objet complet
                table.getItems().remove(asset); // supprime de la TableView
                showAlert(Alert.AlertType.INFORMATION, "Succès", "Asset supprimé avec succès");
            }

        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", e.getMessage());
        }
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void openEditDialog(Asset asset) {

        Dialog<Asset> dialog = new Dialog<>();
        dialog.setTitle("Modifier Asset");
        dialog.setHeaderText("Modifier les informations de l'asset");


        ButtonType okButtonType = new ButtonType("Modifier", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(okButtonType, ButtonType.CANCEL);


        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));


        TextField txtSymbol = new TextField(asset.getSymbol());
        TextField txtName = new TextField(asset.getName());
        TextField txtPrice = new TextField(String.valueOf(asset.getCurrentPrice()));

        ComboBox<AssetType> boxType = new ComboBox<>();
        boxType.getItems().setAll(AssetType.values());
        boxType.setValue(asset.getType());

        ComboBox<Market> boxMarket = new ComboBox<>();
        boxMarket.getItems().setAll(Market.values());
        boxMarket.setValue(asset.getMarket());

        ComboBox<Status> boxStatus = new ComboBox<>();
        boxStatus.getItems().setAll(Status.values());
        boxStatus.setValue(asset.getStatus());


        grid.add(new Label("Symbol:"), 0, 0);
        grid.add(txtSymbol, 1, 0);
        grid.add(new Label("Name:"), 0, 1);
        grid.add(txtName, 1, 1);
        grid.add(new Label("Price:"), 0, 2);
        grid.add(txtPrice, 1, 2);
        grid.add(new Label("Type:"), 0, 3);
        grid.add(boxType, 1, 3);
        grid.add(new Label("Market:"), 0, 4);
        grid.add(boxMarket, 1, 4);
        grid.add(new Label("Status:"), 0, 5);
        grid.add(boxStatus, 1, 5);

        dialog.getDialogPane().setContent(grid);


        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == okButtonType) {
                asset.setSymbol(txtSymbol.getText());
                asset.setName(txtName.getText());
                asset.setCurrentPrice(Double.parseDouble(txtPrice.getText()));
                asset.setType(boxType.getValue());
                asset.setMarket(boxMarket.getValue());
                asset.setStatus(boxStatus.getValue());
                asset.setUpdatedAt(java.time.LocalDateTime.now());
                return asset;
            }
            return null;
        });


        dialog.showAndWait().ifPresent(updatedAsset -> {
            try {
                assetService.updateOne(updatedAsset);
                table.refresh();
                showAlert(Alert.AlertType.INFORMATION, "Succès", "Asset modifié avec succès");
            } catch (Exception e) {
                e.printStackTrace();
                showAlert(Alert.AlertType.ERROR, "Erreur", e.getMessage());
            }
        });
    }






}