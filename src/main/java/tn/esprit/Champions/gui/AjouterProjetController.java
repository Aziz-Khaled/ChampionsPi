package tn.esprit.Champions.gui;

import javafx.application.Platform;
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
    @FXML private Button btnEnregistrer; // Ajouter l'id dans le FXML pour pouvoir le désactiver

    private final projetService ps = new projetService();
    private Utilisateur connectedOwner;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        configurerFormulaire();
        appliquerControlesSaisieUX();
    }

    private void configurerFormulaire() {
        // Remplissage du ComboBox avec les valeurs de l'Enum
        comboStatus.getItems().setAll(projetStatus.values());
        comboStatus.setValue(projetStatus.ACTIVE);

        dateDebut.setValue(LocalDate.now());
        dateFin.setValue(LocalDate.now().plusDays(30));
    }

    public void setConnectedOwner(Utilisateur owner) {
        this.connectedOwner = owner;
    }

    private void appliquerControlesSaisieUX() {
        // Restriction numérique pour le montant
        txtMontant.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("\\d*(\\.\\d*)?")) {
                txtMontant.setText(oldVal);
            }
        });
    }

    @FXML
    private void enregistrer() {
        if (estValide()) {
            // Verrouiller le bouton pour éviter les doubles clics pendant le travail de l'IA
            btnEnregistrer.setDisable(true);
            btnEnregistrer.setText("IA en cours...");

            // Lancer le processus dans un thread séparé pour ne pas bloquer l'interface
            new Thread(() -> {
                try {
                    // 1. Création de l'objet projet
                    projet p = new projet();
                    p.setTitle(txtTitre.getText().trim());
                    p.setDescription(txtDescription.getText().trim());
                    p.setTarget_amount(Double.parseDouble(txtMontant.getText()));
                    p.setStatus(comboStatus.getValue());
                    p.setStart_date(Timestamp.valueOf(dateDebut.getValue().atStartOfDay()));
                    p.setEnd_date(Timestamp.valueOf(dateFin.getValue().atStartOfDay()));
                    p.setOwner_id(this.connectedOwner);

                    // 2. IA : Détection du secteur
                    System.out.println("IA : Analyse du secteur...");
                    String secteurDetecte = ImageAiService.detectProjectSector(p.getTitle(), p.getDescription());
                    p.setSecteur(secteurDetecte);

                    // 3. IA : Génération de l'image
                    System.out.println("IA : Génération de l'image via Flux...");
                    String imagePath = ImageAiService.generateAndSaveAiImage(p.getTitle(), p.getDescription());
                    p.setImageUrl(imagePath);

                    // 4. Sauvegarde Base de Données
                    ps.insertOne(p);

                    // 5. Retour à l'interface (UI Thread)
                    Platform.runLater(() -> {
                        afficherAlerte(Alert.AlertType.INFORMATION, "Succès",
                                "Projet créé avec succès !\nSecteur identifié : " + secteurDetecte);
                        annuler();
                    });

                } catch (Exception e) {
                    e.printStackTrace();
                    Platform.runLater(() -> {
                        afficherAlerte(Alert.AlertType.ERROR, "Erreur", "Échec de la création : " + e.getMessage());
                        btnEnregistrer.setDisable(false);
                        btnEnregistrer.setText("Enregistrer");
                    });
                }
            }).start();
        }
    }

    private boolean estValide() {
        StringBuilder erreurs = new StringBuilder();
        if (connectedOwner == null) erreurs.append("- Erreur : Utilisateur non connecté.\n");
        if (txtTitre.getText().trim().isEmpty()) erreurs.append("- Le titre est requis.\n");
        if (txtDescription.getText().trim().length() < 10) erreurs.append("- Description trop courte.\n");
        if (txtMontant.getText().isEmpty()) erreurs.append("- Montant requis.\n");
        if (dateDebut.getValue() == null || dateFin.getValue() == null) {
            erreurs.append("- Dates requises.\n");
        } else if (dateFin.getValue().isBefore(dateDebut.getValue())) {
            erreurs.append("- La date de fin doit être après le début.\n");
        }

        if (erreurs.length() > 0) {
            afficherAlerte(Alert.AlertType.WARNING, "Vérification", erreurs.toString());
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