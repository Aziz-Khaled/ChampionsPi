package tn.esprit.Champions.gui.invest;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import tn.esprit.Champions.models.credit;
import tn.esprit.Champions.services.creditService;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class MarketplaceController implements Initializable {
    @FXML private FlowPane gridPane;
    private final creditService cs = new creditService();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        Platform.runLater(this::chargerDonnees);
    }

    private void chargerDonnees() {
        if (gridPane == null) return;
        gridPane.getChildren().clear();
        try {
            List<credit> liste = cs.SelectAll();
            if (liste == null || liste.isEmpty()) {
                // Test forcé si DB vide
                for(int i=0; i<3; i++) { creerCarte(null); }
            } else {
                for (credit c : liste) { creerCarte(c); }
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void creerCarte(credit c) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/invest/CreditCard.fxml"));
            VBox card = loader.load();
            CreditCardController ctrl = loader.getController();

            if (c != null) {
                ctrl.setCreditData(c);
            } else {
                ctrl.setData("Projet Test", "Secteur", 5000, 5.0, "OPEN");
            }

            gridPane.getChildren().add(card);
        } catch (Exception e) { e.printStackTrace(); }
    }
}