package tn.esprit.Champions.gui;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.Stage;
import tn.esprit.Champions.models.projet;
import tn.esprit.Champions.models.credit;
import tn.esprit.Champions.models.CreditStatus;
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

    private int connectedUserId;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        configurerComboBoxProjet();
        configurerComboBoxDevise();
        chargerDonneesProjets();
    }

    public void setConnectedUserId(int id) {
        this.connectedUserId = id;
    }

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
        comboDevise.getItems().addAll("TND", "EUR", "USD");
        comboDevise.getSelectionModel().selectFirst();
    }

    private void chargerDonneesProjets() {
        try {
            comboProjet.getItems().setAll(ps.SelectAll());
        } catch (Exception e) {
            System.err.println("Erreur chargement projets : " + e.getMessage());
        }
    }

    @FXML
    private void enregistrer() {
        projet pSelected = comboProjet.getValue();
        String deviseSelected = comboDevise.getValue();

        // Validation stricte
        if (pSelected == null || txtMontant.getText().trim().isEmpty() || deviseSelected == null
                || txtTaux.getText().trim().isEmpty() || txtDuree.getText().trim().isEmpty()) {
            afficherAlerte("Champs manquants", "Veuillez remplir toutes les informations du formulaire.");
            return;
        }

        try {
            credit nouveauCredit = new credit();

            // 1. Liaison des IDs
            nouveauCredit.setProject_id(pSelected.getId_project());
            nouveauCredit.setBorrower_id(this.connectedUserId);

            // 2. Données saisies (Parsing)
            nouveauCredit.setMontant(Double.parseDouble(txtMontant.getText()));
            nouveauCredit.setDevise(deviseSelected);
            nouveauCredit.setTaux(Double.parseDouble(txtTaux.getText()));
            nouveauCredit.setDuree(Integer.parseInt(txtDuree.getText()));
            nouveauCredit.setDescription(txtDescription.getText());

            // 3. Statut initial (doit correspondre à ton Enum CreditStatus)
            nouveauCredit.setStatus(CreditStatus.OPEN);

            // Appel au service (ta nouvelle méthode insertOne avec 9 paramètres)
            cs.insertOne(nouveauCredit);

            annuler();

        } catch (NumberFormatException e) {
            afficherAlerte("Erreur de saisie", "Le montant, le taux et la durée doivent être des nombres valides.");
        } catch (Exception e) {
            e.printStackTrace();
            // Affiche l'erreur réelle pour faciliter le débogage
            afficherAlerte("Erreur", "Impossible d'enregistrer : " + e.getMessage());
        }
    }

    @FXML
    private void annuler() {
        Stage stage = (Stage) txtMontant.getScene().getWindow();
        stage.close();
    }

    private void afficherAlerte(String titre, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(titre);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}