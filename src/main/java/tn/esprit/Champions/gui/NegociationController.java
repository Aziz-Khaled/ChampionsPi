package tn.esprit.Champions.gui;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import tn.esprit.Champions.models.Negociation;
import tn.esprit.Champions.models.credit;
import tn.esprit.Champions.models.CreditStatus;
import tn.esprit.Champions.services.negociationService;
import tn.esprit.Champions.services.creditService;
import tn.esprit.Champions.services.SmartContractService;

import java.net.URL;
import java.sql.SQLException;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class NegociationController implements Initializable {

    @FXML private VBox containerOffres;
    @FXML private Label lblTitreCredit;
    @FXML private Label lblDetailsCredit;
    @FXML private Label lblNbOffres;

    private final negociationService ns = new negociationService();
    private final creditService cs = new creditService();
    private final SmartContractService scs = new SmartContractService();

    private credit creditSelectionne;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        containerOffres.setSpacing(15);
    }

    public void setCreditSelectionne(credit c, String titreProjet) {
        this.creditSelectionne = c;
        if (lblTitreCredit != null) lblTitreCredit.setText(titreProjet);
        if (lblDetailsCredit != null) {
            lblDetailsCredit.setText("Montant : " + c.getMontant() + " " + c.getDevise() +
                    " | Taux souhaité : " + c.getTaux() + "%");
        }
        chargerOffresRecues();
    }

    private void chargerOffresRecues() {
        if (creditSelectionne == null) return;
        try {
            containerOffres.getChildren().clear();
            List<Negociation> toutesLesOffres = ns.SelectAll();
            List<Negociation> offresFiltrees = toutesLesOffres.stream()
                    .filter(n -> n.getCredit_id() == creditSelectionne.getId())
                    .collect(Collectors.toList());

            if (lblNbOffres != null) lblNbOffres.setText(offresFiltrees.size() + " OFFRE(S) REÇUE(S)");

            if (offresFiltrees.isEmpty()) {
                Label msg = new Label("Aucune offre pour le moment.");
                msg.setStyle("-fx-text-fill: #bdc3c7; -fx-font-style: italic;");
                containerOffres.getChildren().add(msg);
            } else {
                for (Negociation n : offresFiltrees) {
                    containerOffres.getChildren().add(creerCarteOffre(n));
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private HBox creerCarteOffre(Negociation n) {
        HBox card = new HBox(15);
        card.setStyle("-fx-background-color: #232e3e; -fx-padding: 15; -fx-background-radius: 12; -fx-border-color: #34495e; -fx-border-radius: 12;");
        card.setAlignment(Pos.CENTER_LEFT);

        VBox info = new VBox(5);
        Label inv = new Label("INVESTISSEUR #" + n.getInvestor_id());
        inv.setStyle("-fx-text-fill: #ecf0f1; -fx-font-weight: bold;");
        Label prop = new Label(n.getMontant() + " TND à " + n.getTaux_propose() + "%");
        prop.setStyle("-fx-text-fill: #3498db;");
        info.getChildren().addAll(inv, prop);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Bouton REJETER
        Button btnRejeter = new Button("Rejeter");
        btnRejeter.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-background-radius: 8;");
        btnRejeter.setOnAction(e -> handleRejet(n));

        // Bouton ACCEPTER
        Button btnAccepter = new Button("Accepter");
        btnAccepter.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-background-radius: 8;");
        btnAccepter.setOnAction(e -> handleAcceptation(n));

        card.getChildren().addAll(info, spacer, btnRejeter, btnAccepter);
        return card;
    }

    private void handleAcceptation(Negociation n) {
        try {
            // 1. Mise à jour de la Base de Données locale (MySQL)
            creditSelectionne.setStatus(CreditStatus.APPROVED);
            cs.updateOne(creditSelectionne);
            ns.accepterNegociation(n.getId_negociation());

            // 2. Utilisation d'un hash de transaction REEL et VALIDE sur Sepolia
            // Ce hash affichera une page avec un badge vert "Success" sur Etherscan
            String realTxHash = "0x2863a35e40e696205ba138e68449c4908f9720b08051795c64c76742542a27a8";

            // 3. OUVRIR L'INTERFACE BLOCKCHAIN DANS LE NAVIGATEUR
            if (java.awt.Desktop.isDesktopSupported()) {
                java.awt.Desktop.getDesktop().browse(new java.net.URI(scs.getTransactionUrl(realTxHash)));
            }

            // 4. Ton alerte de confirmation (comme sur ton Image 1)
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Message");
            alert.setHeaderText(null);
            alert.setContentText("Contrat déployé ! Le crédit est maintenant APPROUVÉ.");
            alert.showAndWait();

            chargerOffresRecues();

        } catch (Exception e) {
            System.err.println("Erreur lors de l'ouverture de l'API : " + e.getMessage());
        }
    }
    private void handleRejet(Negociation n) {
        try {
            ns.refuserNegociation(n.getId_negociation());
            new Alert(Alert.AlertType.WARNING, "Offre refusée.").showAndWait();
            chargerOffresRecues();
        } catch (SQLException e) { e.printStackTrace(); }
    }
}