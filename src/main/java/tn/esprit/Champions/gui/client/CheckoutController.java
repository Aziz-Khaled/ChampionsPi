package tn.esprit.Champions.gui.client;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.awt.Desktop;
import java.net.URI;

public class CheckoutController {

    @FXML
    private Label totalAmountLabel;
    @FXML
    private TextField addressField;
    @FXML
    private TextField phoneField;

    private boolean confirmed = false;
    private String address;
    private String phone;

    public void setTotalAmount(double amount) {
        totalAmountLabel.setText(String.format("%.8f BTC", amount));
    }

    @FXML
    private void handleOpenMaps() {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                    getClass().getResource("/views/client/MapModal.fxml"));
            javafx.scene.Parent root = loader.load();

            MapController controller = loader.getController();

            Stage stage = new Stage();
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            stage.setTitle("Localiser - Fintech BTC");
            stage.setScene(new javafx.scene.Scene(root));
            stage.showAndWait();

            if (controller.isConfirmed()) {
                addressField.setText(controller.getSelectedAddress());
            }
        } catch (Exception e) {
            System.err.println("Error opening internal map: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleConfirm() {
        if (addressField.getText().isEmpty() || phoneField.getText().isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Champs manquants");
            alert.setHeaderText(null);
            alert.setContentText("Veuillez remplir l'adresse et le numéro de téléphone.");
            alert.showAndWait();
            return;
        }

        this.address = addressField.getText();
        this.phone = phoneField.getText();
        this.confirmed = true;
        closeStage();
    }

    @FXML
    private void handleCancel() {
        this.confirmed = false;
        closeStage();
    }

    private void closeStage() {
        Stage stage = (Stage) addressField.getScene().getWindow();
        stage.close();
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public String getAddress() {
        return address;
    }

    public String getPhone() {
        return phone;
    }
}