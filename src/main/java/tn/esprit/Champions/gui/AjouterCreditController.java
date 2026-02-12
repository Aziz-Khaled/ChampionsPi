package tn.esprit.Champions.gui;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.Stage;
import tn.esprit.Champions.models.CreditStatus;
import tn.esprit.Champions.models.credit;
import tn.esprit.Champions.services.creditService;
import java.net.URL;
import java.sql.SQLException;
import java.util.ResourceBundle;

public class AjouterCreditController implements Initializable {

    @FXML private TextField txtMontant, txtTaux;
    @FXML private TextArea txtDescription;
    @FXML private ComboBox<String> comboDevise;
    @FXML private ComboBox<String> comboProjet; // On affiche les noms des projets

    private creditService cs = new creditService();
    // private projetService ps = new projetService(); // Service pour charger les projets

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        comboDevise.getItems().addAll("TND", "EUR", "USD");
        comboDevise.setValue("TND");

        // TEST : On ajoute manuellement des projets pour vérifier la logique
        // Dans le futur, tu feras : comboProjet.getItems().addAll(ps.getNomsProjets());
        comboProjet.getItems().addAll("Projet Solaire A", "Expansion Boutique B");
    }

    @FXML
    private void enregistrer() {
        try {
            if (comboProjet.getValue() == null) {
                afficherErreur("Erreur", "Veuillez sélectionner un projet valide.");
                return;
            }

            credit c = new credit();
            c.setMontant(Double.parseDouble(txtMontant.getText()));
            c.setTaux(Double.parseDouble(txtTaux.getText()));
            c.setDevise(comboDevise.getValue());
            c.setDescription(txtDescription.getText());

            // LOGIQUE AUTOMATIQUE
            c.setStatus(CreditStatus.PENDING); // Toujours en attente à la création
            c.setDuree(12); // Valeur par défaut ou ajouter un champ si nécessaire

            // RÉSOLUTION CLÉ ÉTRANGÈRE :
            // Pour l'instant on force des IDs qui existent en BDD pour tester
            c.setProject_id(1);  // Assure-toi que l'ID 1 existe en table projet
            c.setBorrower_id(1); // Assure-toi que l'ID 1 existe en table user/borrower
            // c.setInvestisseur_id(null); // Optionnel si ta BDD accepte le null

            cs.insertOne(c);

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setContentText("Demande envoyée avec succès !");
            alert.showAndWait();
            annuler();

        } catch (Exception e) {
            afficherErreur("Erreur SQL", "Impossible d'ajouter : " + e.getMessage());
        }
    }

    @FXML private void annuler() {
        ((Stage) txtMontant.getScene().getWindow()).close();
    }

    private void afficherErreur(String titre, String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle(titre);
        a.setContentText(msg);
        a.showAndWait();
    }
}