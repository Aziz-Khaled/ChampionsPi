package tn.esprit.Champions.gui;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import tn.esprit.Champions.models.*;
import tn.esprit.Champions.services.TradeService;

import java.sql.SQLException;
import java.time.LocalDateTime;

public class AddTrade {

    @FXML
    private ComboBox<OrderMode> boxOrder;

    @FXML
    private ComboBox<Status> boxStatus;

    @FXML
    private ComboBox<TradeType> boxType;

    @FXML
    private Button btnCancel;

    @FXML
    private Button btnSubmit;

    @FXML
    private TextField txtPrice;

    @FXML
    private TextField txtQuantity;

    private TradeService tradeService = new TradeService();

    @FXML
    public void initialize() {
        boxType.getItems().setAll(TradeType.values());
        boxStatus.getItems().setAll(Status.values());
        boxOrder.getItems().setAll(OrderMode.values());
    }

    @FXML
    private void handleSubmit() {
        try {
            // Validation
            if (txtPrice.getText().isEmpty()
                    || txtQuantity.getText().isEmpty()
                    || boxType.getValue() == null
                    || boxOrder.getValue() == null
                    || boxStatus.getValue() == null) {

                showAlert(Alert.AlertType.ERROR, "Erreur", "Veuillez remplir tous les champs");
                return;
            }

            Trade trade = new Trade(
                    0,
                    1,
                    1003,
                    boxType.getValue(),
                    boxOrder.getValue(),
                    Double.parseDouble(txtPrice.getText()),
                    Double.parseDouble(txtQuantity.getText()),
                    boxStatus.getValue(),
                    LocalDateTime.now(),
                    LocalDateTime.now(),
                    2
            );

            tradeService.insertOne(trade);
            showAlert(Alert.AlertType.INFORMATION, "Succès", "Trade ajouté avec succès");
            clearFields();

        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur SQL", e.getMessage());
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Le prix et la quantité doivent être des nombres");
        }
    }

    @FXML
    private void handleCancel() {
        clearFields();
    }

    private void clearFields() {
        txtPrice.clear();
        txtQuantity.clear();
        boxType.setValue(null);
        boxOrder.setValue(null);
        boxStatus.setValue(null);
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setContentText(message);
        alert.showAndWait();
    }
}