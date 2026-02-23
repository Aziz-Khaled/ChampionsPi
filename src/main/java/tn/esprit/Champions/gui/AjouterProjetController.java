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
                // 1. Création de l'instance
                projet p = new projet();
                p.setTitle(txtTitre.getText().trim());
                p.setDescription(txtDescription.getText().trim());
                p.setTarget_amount(Double.parseDouble(txtMontant.getText()));
                p.setStatus(comboStatus.getValue());
                p.setStart_date(Timestamp.valueOf(dateDebut.getValue().atStartOfDay()));
                p.setEnd_date(Timestamp.valueOf(dateFin.getValue().atStartOfDay()));
                p.setOwner_id(this.connectedOwner);

                // 2. --- GÉNÉRATION & SAUVEGARDE IMAGE IA ---
                // On affiche une petite info car l'IA peut prendre quelques secondes
                System.out.println("Génération de l'image IA en cours...");

                // Appel au nouveau service Gemini + Flux
                // Cette méthode sauvegarde le .png dans /uploads/ et retourne le chemin relatif
                String imagePath = ImageAiService.generateAndSaveAiImage(p.getTitle(), p.getDescription());

                p.setImageUrl(imagePath);
                // --------------------------------------------

                // 3. Sauvegarde en Base de Données
                ps.insertOne(p);
                // Optionnel : Forcer le rafraîchissement si tu restes sur la même page
                System.out.println("Image générée avec succès à l'emplacement : " + p.getImageUrl());

                afficherAlerte(Alert.AlertType.INFORMATION, "Succès",
                        "Projet '" + p.getTitle() + "' créé avec succès !\n" +
                                "Une image unique a été générée par l'IA et enregistrée.");

                annuler();

            } catch (Exception e) {
                e.printStackTrace();
                afficherAlerte(Alert.AlertType.ERROR, "Erreur de sauvegarde",
                        "Impossible de créer le projet : " + e.getMessage());
            }
        }
    }

    private boolean estValide() {
        StringBuilder erreurs = new StringBuilder();

        if (connectedOwner == null) {
            // Pour le test, on peut simuler un utilisateur si besoin,
            // mais en production, la session doit être active.
            erreurs.append("- Session utilisateur introuvable.\n");
        }
        if (txtTitre.getText().trim().isEmpty()) {
            erreurs.append("- Le titre est obligatoire.\n");
        }
        if (txtDescription.getText().trim().length() < 10) {
            erreurs.append("- La description doit être plus détaillée pour l'IA.\n");
        }
        if (txtMontant.getText().isEmpty()) {
            erreurs.append("- Le montant cible est obligatoire.\n");
        }
        if (dateDebut.getValue() == null || dateFin.getValue() == null) {
            erreurs.append("- Les dates sont obligatoires.\n");
        } else if (dateFin.getValue().isBefore(dateDebut.getValue())) {
            erreurs.append("- La date de fin est invalide.\n");
        }

        if (erreurs.length() > 0) {
            afficherAlerte(Alert.AlertType.WARNING, "Champs requis", erreurs.toString());
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