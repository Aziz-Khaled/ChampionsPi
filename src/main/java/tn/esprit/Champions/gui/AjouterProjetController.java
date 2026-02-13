package tn.esprit.Champions.gui;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.Stage;
import tn.esprit.Champions.models.projet;
import tn.esprit.Champions.models.projetStatus;
import tn.esprit.Champions.services.projetService;

import java.net.URL;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ResourceBundle;

public class AjouterProjetController implements Initializable {

    @FXML private TextField txtTitre, txtMontant;
    @FXML private TextArea txtDescription;
    @FXML private ComboBox<projetStatus> comboStatus;
    @FXML private DatePicker dateDebut, dateFin;

    private projetService ps = new projetService();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        comboStatus.getItems().setAll(projetStatus.values());
        comboStatus.setValue(projetStatus.ACTIVE); // Statut par défaut
        dateDebut.setValue(LocalDate.now()); // Date du jour par défaut
    }

    @FXML
    private void enregistrer() {
        try {
            // Validation basique
            if (txtTitre.getText().isEmpty() || txtMontant.getText().isEmpty()) {
                afficherAlerte("Erreur", "Veuillez remplir les champs obligatoires.");
                return;
            }

            projet p = new projet();
            p.setTitle(txtTitre.getText());
            p.setDescription(txtDescription.getText());
            p.setTarget_amount(Float.parseFloat(txtMontant.getText()));
            p.setStatus(comboStatus.getValue());

            // Conversion LocalDate -> Timestamp
            p.setStart_date(Timestamp.valueOf(dateDebut.getValue().atStartOfDay()));
            p.setEnd_date(Timestamp.valueOf(dateFin.getValue().atStartOfDay()));

            // ID du propriétaire (Owner) - À adapter selon l'utilisateur connecté
            p.setOwner_id(1);

            ps.insertOne(p);

            afficherAlerte("Succès", "Projet créé avec succès !");
            annuler();

        } catch (NumberFormatException e) {
            afficherAlerte("Erreur", "Le montant doit être un nombre valide.");
        } catch (Exception e) {
            e.printStackTrace();
            afficherAlerte("Erreur", "Impossible de sauvegarder : " + e.getMessage());
        }
    }

    @FXML
    private void annuler() {
        ((Stage) txtTitre.getScene().getWindow()).close();
    }

    private void afficherAlerte(String titre, String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(titre);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}