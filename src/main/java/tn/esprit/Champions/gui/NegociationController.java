package tn.esprit.Champions.gui;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import tn.esprit.Champions.models.*;
import tn.esprit.Champions.services.negociationService;
import tn.esprit.Champions.services.creditService;
import tn.esprit.Champions.services.SmartContractService;
import tn.esprit.Champions.utils.DbConnection;

import java.net.URL;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
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
        Utilisateur invDetails = recupererUtilisateurSansErreur(n.getInvestor_id());
        String nomAffichage = (invDetails != null) ? invDetails.getNom() + " " + invDetails.getPrenom() : "ID #" + n.getInvestor_id();

        Label inv = new Label("INVESTISSEUR : " + nomAffichage);
        inv.setStyle("-fx-text-fill: #ecf0f1; -fx-font-weight: bold;");
        Label prop = new Label(n.getMontant() + " TND à " + n.getTaux_propose() + "%");
        prop.setStyle("-fx-text-fill: #3498db;");
        info.getChildren().addAll(inv, prop);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button btnRejeter = new Button("Rejeter");
        btnRejeter.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-background-radius: 8;");
        btnRejeter.setOnAction(e -> handleRejet(n));

        Button btnAccepter = new Button("Accepter");
        btnAccepter.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-background-radius: 8;");
        btnAccepter.setOnAction(e -> handleAcceptation(n));

        card.getChildren().addAll(info, spacer, btnRejeter, btnAccepter);
        return card;
    }

    private void handleAcceptation(Negociation n) {
        try {
            creditSelectionne.setStatus(CreditStatus.APPROVED);
            cs.updateOne(creditSelectionne);
            ns.accepterNegociation(n.getId_negociation());

            Utilisateur investisseur = recupererUtilisateurSansErreur(n.getInvestor_id());

            if (investisseur != null) {
                scs.deployAndShowContract(
                        creditSelectionne.getId(),
                        n.getMontant(),
                        lblTitreCredit.getText(),
                        investisseur,
                        n.getTaux_propose(),
                        creditSelectionne.getDuree()
                );
            } else {
                new Alert(Alert.AlertType.ERROR, "Erreur fatale : Profil investisseur introuvable.").show();
            }

            chargerOffresRecues();
        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Erreur : " + e.getMessage()).show();
        }
    }

    // ✅ MÉTHODE CORRIGÉE POUR CORRESPONDRE AU MODÈLE (8 ARGUMENTS)
    private Utilisateur recupererUtilisateurSansErreur(int id) {
        // Note : On ne récupère pas l'email ici car il n'est pas dans ton modèle Utilisateur
        String query = "SELECT id_user, nom, prenom, mot_de_passe, telephone, piece_identite, user_image FROM utilisateur WHERE id_user = ?";
        try (PreparedStatement ps = DbConnection.getInstance().getCnx().prepareStatement(query)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                // ✅ On appelle le constructeur à 8 arguments :
                // (int id_user, String nom, String prenom, String mot_de_passe, String telephone,
                //  String piece_identite, String user_image, Role role)
                return new Utilisateur(
                        rs.getInt("id_user"),
                        rs.getString("nom"),
                        rs.getString("prenom"),
                        rs.getString("mot_de_passe"),
                        rs.getString("telephone"),
                        rs.getString("piece_identite"),
                        rs.getString("user_image"),
                        null // On met null pour le Role pour éviter les erreurs d'Enum
                );
            }
        } catch (SQLException e) {
            System.err.println("Erreur SQL lors de la récupération : " + e.getMessage());
        }
        return null;
    }

    private void handleRejet(Negociation n) {
        try {
            ns.refuserNegociation(n.getId_negociation());
            new Alert(Alert.AlertType.WARNING, "Offre refusée.").showAndWait();
            chargerOffresRecues();
        } catch (SQLException e) { e.printStackTrace(); }
    }
}