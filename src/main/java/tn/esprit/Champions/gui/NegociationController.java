package tn.esprit.Champions.gui;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
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
        if (containerOffres != null) {
            containerOffres.setSpacing(15);
            containerOffres.setPadding(new Insets(10));
        }
    }

    public void setCreditSelectionne(credit c, String titreProjet) {
        this.creditSelectionne = c;
        if (lblTitreCredit != null) lblTitreCredit.setText(titreProjet.toUpperCase());
        if (lblDetailsCredit != null) {
            lblDetailsCredit.setText("Besoin : " + c.getMontant() + " " + c.getDevise() +
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
                msg.setStyle("-fx-text-fill: #94a3b8; -fx-font-style: italic;");
                containerOffres.getChildren().add(msg);
            } else {
                for (Negociation n : offresFiltrees) {
                    containerOffres.getChildren().add(creerCarteOffre(n));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Crée une carte d'offre complète avec toutes les données financières
     */
    private VBox creerCarteOffre(Negociation n) {
        VBox card = new VBox(10);
        card.setStyle("-fx-background-color: #1e293b; -fx-padding: 20; -fx-background-radius: 12; " +
                "-fx-border-color: #334155; -fx-border-width: 1;");

        // 1. Ligne Investisseur
        Utilisateur invDetails = recupererUtilisateurSansErreur(n.getInvestor_id());
        String nomAffichage = (invDetails != null) ? invDetails.getNom() + " " + invDetails.getPrenom() : "ID #" + n.getInvestor_id();
        Label lblInv = new Label("INVESTISSEUR : " + nomAffichage);
        lblInv.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14;");

        // 2. Ligne Détails Financiers (Montant & Taux)
        HBox finances = new HBox(20);
        Label lblMontant = new Label("💰 Capital : " + n.getMontant() + " TND");
        lblMontant.setStyle("-fx-text-fill: #38bdf8; -fx-font-weight: bold;");
        Label lblTaux = new Label("📈 Taux proposé : " + n.getTaux_propose() + "%");
        lblTaux.setStyle("-fx-text-fill: #38bdf8; -fx-font-weight: bold;");
        finances.getChildren().addAll(lblMontant, lblTaux);

        // 3. Ligne Détails Temporels & Rendement
        HBox rendement = new HBox(20);
        Label lblDuree = new Label("⏱️ Durée : " + creditSelectionne.getDuree() + " mois");
        lblDuree.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 12;");

        // Calcul du montant total à rembourser (Capital + Intérêts)
        double totalRemboursement = n.getMontant() + (n.getMontant() * (n.getTaux_propose() / 100));
        Label lblTotal = new Label("🏁 Retour total estimé : " + String.format("%.2f", totalRemboursement) + " TND");
        lblTotal.setStyle("-fx-text-fill: #10b981; -fx-font-size: 12; -fx-font-weight: bold;");
        rendement.getChildren().addAll(lblDuree, lblTotal);

        // 4. Ligne Boutons
        HBox actions = new HBox(15);
        actions.setAlignment(Pos.CENTER_RIGHT);

        Button btnRejeter = new Button("Rejeter");
        btnRejeter.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-background-radius: 6; -fx-padding: 8 15;");
        btnRejeter.setOnAction(e -> handleRejet(n));

        Button btnAccepter = new Button("Accepter");
        btnAccepter.setStyle("-fx-background-color: #22c55e; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-background-radius: 6; -fx-padding: 8 15;");
        btnAccepter.setOnAction(e -> handleAcceptation(n));

        actions.getChildren().addAll(btnRejeter, btnAccepter);

        card.getChildren().addAll(lblInv, finances, rendement, actions);
        return card;
    }

    private void handleAcceptation(Negociation n) {
        try {
            creditSelectionne.setStatus(CreditStatus.APPROVED);
            cs.updateOne(creditSelectionne);
            ns.accepterNegociation(n.getId_negociation());

            Utilisateur investisseur = recupererUtilisateurSansErreur(n.getInvestor_id());

            if (investisseur != null) {
                // Déploiement du contrat et envoi d'email
                scs.deployAndNotify(
                        creditSelectionne.getId(),
                        n.getMontant(),
                        lblTitreCredit.getText(),
                        investisseur,
                        n.getTaux_propose(),
                        creditSelectionne.getDuree()
                );
            } else {
                new Alert(Alert.AlertType.ERROR, "Erreur : Profil investisseur introuvable.").show();
            }

            chargerOffresRecues();
        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Erreur lors de l'acceptation : " + e.getMessage()).show();
        }
    }

    private Utilisateur recupererUtilisateurSansErreur(int id) {
        String query = "SELECT id_user, nom, prenom, email, mot_de_passe, telephone, piece_identite, user_image, statut, role FROM utilisateur WHERE id_user = ?";
        try (PreparedStatement ps = DbConnection.getInstance().getCnx().prepareStatement(query)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return new Utilisateur(
                        rs.getInt("id_user"),
                        rs.getString("nom"),
                        rs.getString("prenom"),
                        rs.getString("email"),
                        rs.getString("mot_de_passe"),
                        rs.getString("telephone"),
                        rs.getString("piece_identite"),
                        rs.getString("user_image"),
                        null,
                        null
                );
            }
        } catch (SQLException e) {
            System.err.println("Erreur SQL : " + e.getMessage());
        }
        return null;
    }

    private void handleRejet(Negociation n) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation de rejet");
        alert.setHeaderText("Supprimer cette offre d'investissement");
        alert.setContentText("Voulez-vous vraiment rejeter l'offre de " + n.getMontant() + " TND ?");

        alert.getDialogPane().setStyle("-fx-background-color: #1e293b;");
        alert.getDialogPane().lookupAll(".label").forEach(node -> node.setStyle("-fx-text-fill: white;"));

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    ns.deleteOne(n);
                    chargerOffresRecues();
                } catch (SQLException e) {
                    e.printStackTrace();
                    new Alert(Alert.AlertType.ERROR, "Erreur lors du rejet : " + e.getMessage()).show();
                }
            }
        });
    }
}