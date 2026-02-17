package tn.esprit.Champions.gui;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.Stage;
import tn.esprit.Champions.models.projet;
import tn.esprit.Champions.models.projetStatus;
import tn.esprit.Champions.models.Utilisateur;
import tn.esprit.Champions.services.ImageAiService;
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

    private final projetService ps = new projetService();
    private Utilisateur connectedOwner;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        configurerFormulaire();
        appliquerControlesSaisieUX();
    }

    private void configurerFormulaire() {
        comboStatus.getItems().setAll(projetStatus.values());
        comboStatus.setValue(projetStatus.ACTIVE);
        dateDebut.setValue(LocalDate.now());
        // Optionnel : mettre une date de fin par défaut à +30 jours
        dateFin.setValue(LocalDate.now().plusDays(30));
    }

    public void setConnectedOwner(Utilisateur owner) {
        this.connectedOwner = owner;
    }

    private void appliquerControlesSaisieUX() {
        txtMontant.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("\\d*(\\.\\d*)?")) {
                txtMontant.setText(oldVal);
            }
        });
    }

    @FXML
    private void enregistrer() {
        if (estValide()) {
            try {
                projet p = new projet();

                // 1. Données de base
                p.setTitle(txtTitre.getText().trim());
                p.setDescription(txtDescription.getText().trim());
                p.setTarget_amount(Double.parseDouble(txtMontant.getText()));
                p.setStatus(comboStatus.getValue());

                // 2. Conversion des dates (LocalDate -> Timestamp pour la DB)
                p.setStart_date(Timestamp.valueOf(dateDebut.getValue().atStartOfDay()));
                p.setEnd_date(Timestamp.valueOf(dateFin.getValue().atStartOfDay()));

                // 3. --- GÉNÉRATION IMAGE IA ---
                // On utilise le titre et la description pour que l'IA choisisse la bonne image
                String imageUrl = ImageAiService.generateProjectImageUrl(p.getTitle(), p.getDescription());
                p.setImageUrl(imageUrl);
                // ------------------------------

                // 4. Propriétaire
                p.setOwner_id(this.connectedOwner);

                // 5. Sauvegarde
                ps.insertOne(p);

                afficherAlerte(Alert.AlertType.INFORMATION, "Succès",
                        "Projet '" + p.getTitle() + "' créé avec succès !\nUne image IA a été assignée automatiquement.");

                annuler(); // Ferme la fenêtre après succès

            } catch (Exception e) {
                e.printStackTrace();
                afficherAlerte(Alert.AlertType.ERROR, "Erreur de sauvegarde", "Impossible d'enregistrer le projet : " + e.getMessage());
            }
        }
    }

    private boolean estValide() {
        StringBuilder erreurs = new StringBuilder();

        if (connectedOwner == null) {
            erreurs.append("- Propriétaire non défini (vérifiez la session).\n");
        }
        if (txtTitre.getText().trim().isEmpty()) {
            erreurs.append("- Le titre est obligatoire.\n");
        }
        if (txtMontant.getText().isEmpty()) {
            erreurs.append("- Le montant cible est obligatoire.\n");
        }
        if (dateDebut.getValue() == null || dateFin.getValue() == null) {
            erreurs.append("- Les dates sont obligatoires.\n");
        } else if (dateFin.getValue().isBefore(dateDebut.getValue())) {
            erreurs.append("- La date de fin ne peut pas être antérieure à la date de début.\n");
        }

        if (erreurs.length() > 0) {
            afficherAlerte(Alert.AlertType.WARNING, "Validation", erreurs.toString());
            return false;
        }
        return true;
    }

    @FXML
    private void annuler() {
        if (txtTitre.getScene() != null) {
            Stage stage = (Stage) txtTitre.getScene().getWindow();
            stage.close();
        }
    }

    private void afficherAlerte(Alert.AlertType type, String titre, String msg) {
        Alert alert = new Alert(type);
        alert.setTitle(titre);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}