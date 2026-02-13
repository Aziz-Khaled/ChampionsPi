package tn.esprit.Champions.gui;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import tn.esprit.Champions.models.Negociation;
import tn.esprit.Champions.models.credit; // Assure-toi que l'import correspond à ton modèle
import tn.esprit.Champions.services.negociationService;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class NegociationController implements Initializable {

    @FXML private VBox containerOffres;

    private final negociationService ns = new negociationService();

    // Cette variable stockera le crédit sélectionné envoyé par l'autre interface
    private credit creditSelectionne;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // On ne charge pas les offres ici car creditSelectionne est encore nul
    }

    /**
     * Cette méthode est appelée par AfficherCreditsController
     * Elle reçoit le crédit et déclenche l'affichage des offres
     */
    public void setCreditSelectionne(credit c) {
        this.creditSelectionne = c;
        System.out.println("Chargement des offres pour le crédit #" + c.getId());
        chargerOffresRecues();
    }

    private void chargerOffresRecues() {
        if (creditSelectionne == null) return;

        try {
            containerOffres.getChildren().clear(); // On vide le container avant de charger

            // Récupérer toutes les négociations
            List<Negociation> toutesLesOffres = ns.SelectAll();

            // Filtrer pour ne garder que celles qui concernent MON crédit
            List<Negociation> offresFiltrees = toutesLesOffres.stream()
                    .filter(n -> n.getCredit_id() == creditSelectionne.getId())
                    .collect(Collectors.toList());

            if (offresFiltrees.isEmpty()) {
                containerOffres.getChildren().add(new Label("Aucune offre reçue pour ce crédit."));
            } else {
                for (Negociation n : offresFiltrees) {
                    containerOffres.getChildren().add(creerCarteOffre(n));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private HBox creerCarteOffre(Negociation n) {
        HBox card = new HBox(20);
        card.setStyle("-fx-background-color: white; -fx-padding: 15; -fx-background-radius: 10; " +
                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 5, 0, 0, 2);");
        card.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        VBox info = new VBox(5);
        Label inv = new Label("Investisseur #" + n.getInvestor_id());
        inv.setStyle("-fx-font-weight: bold; -fx-font-size: 14;");
        Label taux = new Label("Propose un taux de : " + n.getTaux_propose() + "%");
        info.getChildren().addAll(inv, taux);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button btnAccepter = new Button("Accepter");
        btnAccepter.setStyle("-fx-background-color: #2ecc71; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");
        btnAccepter.setOnAction(e -> valider(n));

        Button btnNegocier = new Button("Contre-proposer");
        btnNegocier.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");
        // Option : lier vers ton interface de slider ici

        card.getChildren().addAll(info, spacer, btnNegocier, btnAccepter);
        return card;
    }

    private void valider(Negociation n) {
        System.out.println("Crédit validé avec l'investisseur " + n.getInvestor_id());
        // Logique de validation : ns.accepterNegociation(n.getId_negociation());
    }
}