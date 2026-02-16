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
        comboStatus.setValue(projetStatus.ACTIVE);
        dateDebut.setValue(LocalDate.now());

        // --- CONTROLE EN TEMPS RÉEL (UX) ---
        // Empêcher la saisie de texte dans le champ montant (uniquement chiffres et point)
        txtMontant.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("\\d*(\\.\\d*)?")) {
                txtMontant.setText(oldVal);
            }
        });
    }

    @FXML
    private void enregistrer() {
        if (estValide()) { // On utilise la nouvelle méthode de validation
            try {
                projet p = new projet();
                p.setTitle(txtTitre.getText().trim());
                p.setDescription(txtDescription.getText().trim());
                p.setTarget_amount(Float.parseFloat(txtMontant.getText()));
                p.setStatus(comboStatus.getValue());
                p.setStart_date(Timestamp.valueOf(dateDebut.getValue().atStartOfDay()));
                p.setEnd_date(Timestamp.valueOf(dateFin.getValue().atStartOfDay()));
                p.setOwner_id(1); // À dynamiser plus tard

                ps.insertOne(p);
                afficherAlerte(Alert.AlertType.INFORMATION, "Succès", "Projet créé avec succès !");
                annuler();

            } catch (Exception e) {
                afficherAlerte(Alert.AlertType.ERROR, "Erreur", "Erreur technique : " + e.getMessage());
            }
        }
    }

    // --- LE CŒUR DU CONTRÔLE DE SAISIE ---
    private boolean estValide() {
        StringBuilder erreurs = new StringBuilder();

        // 1. Titre : pas vide et longueur minimum
        if (txtTitre.getText().trim().isEmpty() || txtTitre.getText().length() < 3) {
            erreurs.append("- Le titre doit contenir au moins 3 caractères.\n");
        }

        // 2. Montant : non vide et positif
        if (txtMontant.getText().isEmpty()) {
            erreurs.append("- Le montant cible est obligatoire.\n");
        } else {
            try {
                float montant = Float.parseFloat(txtMontant.getText());
                if (montant <= 0) erreurs.append("- Le montant doit être supérieur à zéro.\n");
            } catch (NumberFormatException e) {
                erreurs.append("- Le montant doit être un nombre valide.\n");
            }
        }

        // 3. Dates : Logique chronologique
        if (dateDebut.getValue() == null || dateFin.getValue() == null) {
            erreurs.append("- Les dates de début et de fin sont obligatoires.\n");
        } else {
            if (dateDebut.getValue().isBefore(LocalDate.now()) && !dateDebut.getValue().isEqual(LocalDate.now())) {
                erreurs.append("- La date de début ne peut pas être dans le passé.\n");
            }
            if (dateFin.getValue().isBefore(dateDebut.getValue())) {
                erreurs.append("- La date de fin doit être après la date de début.\n");
            }
        }

        if (erreurs.length() > 0) {
            afficherAlerte(Alert.AlertType.WARNING, "Champs invalides", erreurs.toString());
            return false;
        }
        return true;
    }

    @FXML
    private void annuler() {
        ((Stage) txtTitre.getScene().getWindow()).close();
    }

    // Amélioration de l'alerte pour gérer différents types (Erreur, Info, Warning)
    private void afficherAlerte(Alert.AlertType type, String titre, String msg) {
        Alert alert = new Alert(type);
        alert.setTitle(titre);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}