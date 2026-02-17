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
        if (gridPane == null) {
            System.err.println("Erreur : gridPane est null. Vérifiez l'fx:id dans votre FXML principal.");
            return;
        }
        gridPane.getChildren().clear();
        try {
            List<credit> liste = cs.SelectAll();
            if (liste != null && !liste.isEmpty()) {
                for (credit c : liste) { creerCarte(c); }
            } else {
                System.out.println("Base de données vide ou erreur service.");
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void creerCarte(credit c) {
        try {
            // Le chemin commence par / pour dire "partir de la racine de resources"
            // Puis le dossier "invest", puis le fichier.
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/invest/CreditCard.fxml"));

            VBox card = loader.load();
            CreditCardController ctrl = loader.getController();

            if (c != null && ctrl != null) {
                ctrl.setCreditData(c);
                gridPane.getChildren().add(card);
            }
        } catch (Exception e) {
            System.err.println("Erreur de chargement : " + e.getMessage());
            e.printStackTrace();
        }
    }
}