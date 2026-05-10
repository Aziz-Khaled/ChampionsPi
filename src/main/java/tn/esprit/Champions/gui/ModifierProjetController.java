package tn.esprit.Champions.gui;

import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import tn.esprit.Champions.models.projet;
import tn.esprit.Champions.models.projetStatus;
import tn.esprit.Champions.services.projetService;

public class ModifierProjetController {

    @FXML private TextField txtTitre, txtMontant;
    @FXML private TextArea txtDescription;
    @FXML private ComboBox<projetStatus> comboStatus;

    private projet projetSelectionne;
    private final projetService ps = new projetService();

    @FXML
    public void initialize() {
        comboStatus.getItems().setAll(projetStatus.values());
    }

    // METHODE CLE : Pour pré-remplir les champs depuis la liste
    public void initData(projet p) {
        this.projetSelectionne = p;
        txtTitre.setText(p.getTitle());
        txtMontant.setText(String.valueOf(p.getTarget_amount()));
        txtDescription.setText(p.getDescription());
        comboStatus.setValue(p.getStatus());
    }

    @FXML
    private void enregistrer() {
        try {
            projetSelectionne.setTitle(txtTitre.getText());
            projetSelectionne.setTarget_amount(Double.parseDouble(txtMontant.getText()));
            projetSelectionne.setDescription(txtDescription.getText());
            projetSelectionne.setStatus(comboStatus.getValue());

            ps.updateOne(projetSelectionne);
            annuler(); // Fermer la fenêtre
        } catch (Exception e) {
            System.err.println("Erreur de mise à jour");
        }
    }

    @FXML
    private void annuler() {
        ((Stage) txtTitre.getScene().getWindow()).close();
    }
}