package tn.esprit.Champions.gui;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import tn.esprit.Champions.models.formations;
import java.io.IOException;
import java.time.temporal.ChronoUnit;

public class FormationDetailsController {

    @FXML private Label lblTitre, lblDomaine, lblPrix, lblDuree;
    @FXML private Text txtDescription;

    private formations selectedFormation;

    // Cette méthode est appelée par le Dashboard avant de changer de page
    public void setFormation(formations f) {
        this.selectedFormation = f;

        lblTitre.setText(f.getTitre());
        lblDomaine.setText(f.getDomaine() != null ? f.getDomaine().toUpperCase() : "FORMATION");
        txtDescription.setText(f.getDescription());
        lblPrix.setText(f.getPrix() + " DT");

        // Calcul dynamique de la durée (GoMyCode Style)
        if (f.getDateDebut() != null && f.getDateFin() != null) {
            long weeks = ChronoUnit.WEEKS.between(f.getDateDebut(), f.getDateFin());
            lblDuree.setText(weeks > 0 ? weeks + " semaines" : "Formation intensive");
        } else {
            lblDuree.setText("Durée flexible");
        }
    }

    @FXML
    private void handleParticipation(ActionEvent event) {
        // Logique de participation (Tu pourras lier ton ParticipationService ici)
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Inscription");
        alert.setHeaderText("Félicitations !");
        alert.setContentText("Vous êtes maintenant inscrit à la formation : " + selectedFormation.getTitre());
        alert.showAndWait();
    }

    @FXML
    private void handleBack(ActionEvent event) {
        try {
            // Retour au Dashboard (Chemin racine)
            Parent root = FXMLLoader.load(getClass().getResource("/student_dashboard.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}