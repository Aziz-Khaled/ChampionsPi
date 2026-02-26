package tn.esprit.Champions.gui;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.Stage;
import tn.esprit.Champions.models.projet;
import tn.esprit.Champions.models.credit;
import tn.esprit.Champions.models.CreditStatus;
import tn.esprit.Champions.models.Utilisateur; // <--- Import de ton modèle Utilisateur
import tn.esprit.Champions.services.projetService;
import tn.esprit.Champions.services.creditService;

import java.net.URL;
import java.util.ResourceBundle;

public class AjouterCreditController implements Initializable {

    @FXML private ComboBox<projet> comboProjet;
    @FXML private ComboBox<String> comboDevise;
    @FXML private TextField txtMontant, txtTaux, txtDuree;
    @FXML private TextArea txtDescription;

    private final projetService ps = new projetService();
    private final creditService cs = new creditService();

    // Changement : on utilise l'objet Utilisateur au lieu d'un int
    private Utilisateur connectedUser;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        configurerComboBoxProjet();
        configurerComboBoxDevise();
        chargerDonneesProjets();
        appliquerControlesSaisieTempsReel();
    }

    // Changement : le tuteur veut passer l'objet utilisateur complet
    public void setConnectedUser(Utilisateur user) {
        this.connectedUser = user;
    }

    // --- CONFIGURATION DES UI ---

    private void configurerComboBoxProjet() {
        comboProjet.setCellFactory(lv -> new ListCell<projet>() {
            @Override
            protected void updateItem(projet p, boolean empty) {
                super.updateItem(p, empty);
                setText((empty || p == null) ? "" : p.getTitle());
            }
        });
        comboProjet.setButtonCell(new ListCell<projet>() {
            @Override
            protected void updateItem(projet p, boolean empty) {
                super.updateItem(p, empty);
                setText((empty || p == null) ? "" : p.getTitle());
            }
        });
    }

    private void configurerComboBoxDevise() {
        comboDevise.getItems().setAll("TND", "EUR", "USD");
        comboDevise.getSelectionModel().selectFirst();
    }

    private void chargerDonneesProjets() {
        try {
            comboProjet.getItems().setAll(ps.SelectAll());
        } catch (Exception e) {
            System.err.println("Erreur chargement projets : " + e.getMessage());
        }
    }

    private void appliquerControlesSaisieTempsReel() {
        txtMontant.textProperty().addListener((obs, old, newVal) -> {
            if (!newVal.matches("\\d*(\\.\\d*)?")) txtMontant.setText(old);
        });
        txtTaux.textProperty().addListener((obs, old, newVal) -> {
            if (!newVal.matches("\\d*(\\.\\d*)?")) txtTaux.setText(old);
        });
        txtDuree.textProperty().addListener((obs, old, newVal) -> {
            if (!newVal.matches("\\d*")) txtDuree.setText(old);
        });
    }

    // --- LOGIQUE D'ENREGISTREMENT ---

    @FXML
    private void enregistrer() {
        if (estSaisieValide()) {
            try {
                projet pSelected = comboProjet.getValue();
                credit nouveauCredit = new credit();

                // On associe les objets (POO)
                nouveauCredit.setProject_id(pSelected.getId_project());
                nouveauCredit.setBorrower_id(this.connectedUser);

                nouveauCredit.setMontant(Double.parseDouble(txtMontant.getText()));
                nouveauCredit.setDevise(comboDevise.getValue());
                nouveauCredit.setTaux(Double.parseDouble(txtTaux.getText()));
                nouveauCredit.setDuree(Integer.parseInt(txtDuree.getText()));
                nouveauCredit.setDescription(txtDescription.getText().trim());
                nouveauCredit.setStatus(CreditStatus.OPEN);

                // Ton service s'occupera d'extraire les IDs des objets pour la DB
                cs.insertOne(nouveauCredit);

                afficherAlerte(Alert.AlertType.INFORMATION, "Succès", "Le crédit a été ajouté avec succès !");
                annuler();

            } catch (Exception e) {
                afficherAlerte(Alert.AlertType.ERROR, "Erreur Système", "Impossible d'enregistrer : " + e.getMessage());
            }
        }
    }

    private boolean estSaisieValide() {
        StringBuilder erreurs = new StringBuilder();

        // 0. Vérification de l'utilisateur (Sécurité)
        if (connectedUser == null) {
            erreurs.append("- Aucun utilisateur connecté détecté.\n");
        }

        // 1. Vérification Projet
        if (comboProjet.getValue() == null) {
            erreurs.append("- Veuillez sélectionner un projet cible.\n");
        }

        // 2. Vérification Montant
        if (txtMontant.getText().trim().isEmpty()) {
            erreurs.append("- Le montant est obligatoire.\n");
        } else {
            double m = Double.parseDouble(txtMontant.getText());
            if (m <= 0) {
                erreurs.append("- Le montant doit être strictement positif.\n");
            } else if (comboProjet.getValue() != null && m > comboProjet.getValue().getTarget_amount()) {
                erreurs.append("- Le montant demandé dépasse le budget du projet (")
                        .append(comboProjet.getValue().getTarget_amount()).append(").\n");
            }
        }

        // 3. Vérification Taux
        if (txtTaux.getText().trim().isEmpty()) {
            erreurs.append("- Le taux d'intérêt est obligatoire.\n");
        } else {
            double t = Double.parseDouble(txtTaux.getText());
            if (t < 0 || t > 30) erreurs.append("- Le taux doit être entre 0% et 30%.\n");
        }

        // 4. Vérification Durée
        if (txtDuree.getText().trim().isEmpty()) {
            erreurs.append("- La durée est obligatoire.\n");
        } else {
            int d = Integer.parseInt(txtDuree.getText());
            if (d < 1 || d > 360) erreurs.append("- La durée doit être entre 1 et 360 mois.\n");
        }

        if (erreurs.length() > 0) {
            afficherAlerte(Alert.AlertType.WARNING, "Erreur de saisie", erreurs.toString());
            return false;
        }
        return true;
    }

    @FXML
    private void annuler() {
        Stage stage = (Stage) txtMontant.getScene().getWindow();
        stage.close();
    }

    private void afficherAlerte(Alert.AlertType type, String titre, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(titre);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}