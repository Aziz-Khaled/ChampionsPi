package tn.esprit.Champions.gui;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import tn.esprit.Champions.models.credit;
import tn.esprit.Champions.models.projet;
import tn.esprit.Champions.services.creditService;
import tn.esprit.Champions.services.projetService;

import java.net.URL;
import java.util.ResourceBundle;

public class ModifierCreditController implements Initializable {

    @FXML
    private ComboBox<projet> comboProjet;
    @FXML
    private ComboBox<String> comboDevise;
    @FXML
    private TextField txtMontant, txtTaux, txtDuree;
    @FXML
    private TextArea txtDescription;

    private final creditService cs = new creditService();
    private final projetService ps = new projetService();
    private credit selectedCredit;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        configurerComboBoxProjet(); // <--- AJOUTE ÇA ICI
        comboDevise.getItems().addAll("TND", "EUR", "USD");
        chargerProjets();
    }

    private void chargerProjets() {
        try {
            comboProjet.getItems().setAll(ps.SelectAll());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // MÉTHODE CLÉ : Reçoit les données de l'affichage
    public void initData(credit c) {
        this.selectedCredit = c;

        // Pré-remplir les champs
        txtMontant.setText(String.valueOf(c.getMontant()));
        txtTaux.setText(String.valueOf(c.getTaux()));
        txtDuree.setText(String.valueOf(c.getDuree()));
        txtDescription.setText(c.getDescription());
        comboDevise.setValue(c.getDevise());

        // Sélectionner le bon projet dans la combo
        for (projet p : comboProjet.getItems()) {
            if (p.getId_projet() == c.getProject_id()) {
                comboProjet.setValue(p);
                break;
            }
        }
    }

    @FXML
    private void modifier() {
        try {
            selectedCredit.setProject_id(comboProjet.getValue().getId_projet());
            selectedCredit.setMontant(Double.parseDouble(txtMontant.getText()));
            selectedCredit.setDevise(comboDevise.getValue());
            selectedCredit.setTaux(Double.parseDouble(txtTaux.getText()));
            selectedCredit.setDuree(Integer.parseInt(txtDuree.getText()));
            selectedCredit.setDescription(txtDescription.getText());

            cs.updateOne(selectedCredit); // Appel de la méthode UPDATE
            fermer();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void configurerComboBoxProjet() {
        // 1. Ce qui s'affiche dans la liste déroulante
        comboProjet.setCellFactory(lv -> new ListCell<projet>() {
            @Override
            protected void updateItem(projet p, boolean empty) {
                super.updateItem(p, empty);
                setText((empty || p == null) ? "" : p.getTitle());
            }
        });

        // 2. Ce qui s'affiche une fois l'élément sélectionné
        comboProjet.setButtonCell(new ListCell<projet>() {
            @Override
            protected void updateItem(projet p, boolean empty) {
                super.updateItem(p, empty);
                setText((empty || p == null) ? "" : p.getTitle());
            }
        });
    }

    @FXML
    private void fermer() {
        ((Stage) txtMontant.getScene().getWindow()).close();
    }
}