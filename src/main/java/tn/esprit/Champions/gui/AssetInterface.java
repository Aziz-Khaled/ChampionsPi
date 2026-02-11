package tn.esprit.Champions.gui;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import tn.esprit.Champions.models.Asset;
import tn.esprit.Champions.models.AssetType;
import tn.esprit.Champions.models.Market;
import tn.esprit.Champions.models.Status;
import tn.esprit.Champions.services.AssetService;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Properties;


public class AssetInterface {



        @FXML
        private ComboBox<Market> boxMarket;

        @FXML
        private ComboBox<Status> boxStatus;

        @FXML
        private ComboBox<AssetType> boxType;

        @FXML
        private Button btnCancel;

        @FXML
        private Button btnSubmit;

        @FXML
        private TextField txtCurrentPrice;

        @FXML
        private TextField txtName;

        @FXML
        private TextField txtSymbol;

    private AssetService assetService = new AssetService();

    @FXML
    public void initialize() {

        boxMarket.getItems().setAll(Market.values());
        boxStatus.getItems().setAll(Status.values());
        boxType.getItems().setAll(AssetType.values());
    }

    @FXML
    private void handleSubmit() {
        try {
            // Validation simple
            if (txtSymbol.getText().isEmpty()
                    || txtName.getText().isEmpty()
                    || txtCurrentPrice.getText().isEmpty()
                    || boxType.getValue() == null
                    || boxMarket.getValue() == null
                    || boxStatus.getValue() == null) {

                showAlert(Alert.AlertType.ERROR, "Erreur", "Veuillez remplir tous les champs");
                return;
            }

            Asset asset = new Asset(
                    0,
                    txtSymbol.getText(),
                    txtName.getText(),
                    boxType.getValue(),
                    boxMarket.getValue(),
                    Double.parseDouble(txtCurrentPrice.getText()),
                    boxStatus.getValue(),
                    LocalDateTime.now(),
                    LocalDateTime.now(),
                    1
            );

            assetService.insertOne(asset);
            showAlert(Alert.AlertType.INFORMATION,
                    "Succès",
                    "Asset ajouté avec succès");

            clearFields();

            showAlert(Alert.AlertType.INFORMATION, "Succès", "Asset ajouté avec succès");
            clearFields();

        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur SQL", e.getMessage());
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Le prix doit être un nombre");
        }
    }

    @FXML
    private void handleCancel() {
        clearFields();
    }

    private void clearFields() {
        txtSymbol.clear();
        txtName.clear();
        txtCurrentPrice.clear();
        boxType.setValue(null);
        boxMarket.setValue(null);
        boxStatus.setValue(null);
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setContentText(message);
        alert.show();
        alert.showAndWait();
    }
}










