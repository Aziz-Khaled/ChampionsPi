package tn.esprit.Champions.gui;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import tn.esprit.Champions.models.Reclamation;
import tn.esprit.Champions.models.StatutReclamation;
import tn.esprit.Champions.models.formations; // Assure-toi que l'import est correct
import tn.esprit.Champions.services.ReclamationService;

import java.sql.SQLException;

public class ReclamationController {

    @FXML private TextField txtSujet;
    @FXML private TextArea txtDescription;

    private final ReclamationService rs = new ReclamationService();
    private formations formationSelectionnee; // L'objet passé depuis l'autre vue

    /**
     * Reçoit la formation sélectionnée et pré-remplit le sujet
     */
    public void setFormation(formations f) {
        this.formationSelectionnee = f;
        if (txtSujet != null && f != null) {
            txtSujet.setText("Réclamation : " + f.getTitre());
        }
    }

    @FXML
    private void handleEnvoyer(ActionEvent event) {
        String sujet = txtSujet.getText().trim();
        String desc = txtDescription.getText().trim();

        if (sujet.isEmpty() || desc.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Champs requis", "Veuillez remplir le sujet et la description.");
            return;
        }

        try {
            Reclamation rec = new Reclamation();
            rec.setIdUtilisateur(1);

            // Utilise l'ID de la formation récupérée via setFormation
            if (formationSelectionnee != null) {
                rec.setIdFormation(formationSelectionnee.getIdFormation());
            } else {
                rec.setIdFormation(9); // Valeur de secours
            }

            rec.setSujet(sujet);
            rec.setDescription(desc);
            rec.setStatut(StatutReclamation.EN_ATTENTE);

            rs.insertOne(rec);

            showAlert(Alert.AlertType.INFORMATION, "Succès", "Votre réclamation a été envoyée avec succès.");
            closeWindow();

        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur SQL", "Impossible d'enregistrer la réclamation.");
        }
    }

    @FXML private void handleCancel(ActionEvent event) { closeWindow(); }

    private void closeWindow() {
        if (txtSujet.getScene() != null) {
            ((Stage) txtSujet.getScene().getWindow()).close();
        }
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}