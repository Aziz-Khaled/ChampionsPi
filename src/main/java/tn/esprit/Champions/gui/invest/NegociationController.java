package tn.esprit.Champions.gui.invest;

import javafx.animation.FadeTransition;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.Duration;
import tn.esprit.Champions.models.Negociation;
import tn.esprit.Champions.models.credit;
import tn.esprit.Champions.services.negociationService;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Optional;

public class NegociationController {

    @FXML private HBox negocRoot;
    @FXML private Label lblMontantOrigine, lblTauxOrigine, lblDureeOrigine, lblStatutNegoc, lblGainEstime;

    private credit currentCredit;
    private final negociationService ns = new negociationService();

    @FXML
    public void initialize() {
        // Animation de fluidité à l'ouverture
        negocRoot.setOpacity(0);
        FadeTransition ft = new FadeTransition(Duration.millis(600), negocRoot);
        ft.setToValue(1.0);
        ft.play();
    }

    public void initData(credit c) {
        this.currentCredit = c;
        if (c != null) {
            updateUI(c.getMontant(), c.getTaux(), c.getDuree());
        }
    }

    private void updateUI(double m, double t, int d) {
        lblMontantOrigine.setText(String.format("%.2f TND", m));
        lblTauxOrigine.setText(String.format("%.2f %%", t));
        lblDureeOrigine.setText(d + " Mois");

        // Calcul du profit : (Montant * Taux/100) * (Durée en années)
        double profit = m * (t / 100.0) * (d / 12.0);
        lblGainEstime.setText(String.format("%.2f TND", profit));
    }

    @FXML
    private void proposerOffre(ActionEvent event) {
        Dialog<double[]> dialog = new Dialog<>();
        dialog.setTitle("Ajustement des Termes");
        dialog.setHeaderText("Modifiez votre proposition financière");

        ButtonType proposerBtnType = new ButtonType("Valider l'offre", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(proposerBtnType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(15); grid.setVgap(15);
        grid.setPadding(new Insets(20));

        // Nettoyage des valeurs pour les champs de texte
        TextField txtM = new TextField(lblMontantOrigine.getText().replaceAll("[^0-9,.]", "").replace(",", "."));
        TextField txtT = new TextField(lblTauxOrigine.getText().replaceAll("[^0-9,.]", "").replace(",", "."));
        TextField txtD = new TextField(lblDureeOrigine.getText().replaceAll("[^0-9]", ""));

        grid.add(new Label("Montant (TND):"), 0, 0);
        grid.add(txtM, 1, 0);
        grid.add(new Label("Taux d'intérêt (%):"), 0, 1);
        grid.add(txtT, 1, 1);
        grid.add(new Label("Durée (Mois):"), 0, 2);
        grid.add(txtD, 1, 2);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == proposerBtnType) {
                try {
                    // CORRECTION DU BUG : Remplacer virgule par point
                    return new double[]{
                            Double.parseDouble(txtM.getText().replace(",", ".")),
                            Double.parseDouble(txtT.getText().replace(",", ".")),
                            Double.parseDouble(txtD.getText().replace(",", "."))
                    };
                } catch (NumberFormatException e) {
                    return null;
                }
            }
            return null;
        });

        Optional<double[]> result = dialog.showAndWait();

        result.ifPresent(data -> {
            try {
                double m = data[0];
                double t = data[1];
                int d = (int) data[2];

                Negociation n = new Negociation();
                n.setCredit_id(currentCredit.getId());
                n.setInvestor_id(2); // ID de test
                n.setMontant(m);
                n.setTaux_propose(t);

                ns.insertOne(n);

                updateUI(m, t, d);
                lblStatutNegoc.setText("OFFRE ENVOYÉE");

            } catch (SQLException e) {
                System.err.println("Erreur SQL : " + e.getMessage());
            }
        });
    }

    @FXML
    private void retourDetails(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/invest/DetailsCredit.fxml"));
            Parent root = loader.load();
            negocRoot.getScene().setRoot(root);
        } catch (IOException e) { e.printStackTrace(); }
    }
}