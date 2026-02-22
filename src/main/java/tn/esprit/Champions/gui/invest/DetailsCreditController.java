package tn.esprit.Champions.gui.invest;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import tn.esprit.Champions.models.*;
import tn.esprit.Champions.services.RiskAnalysisService;
import tn.esprit.Champions.services.projetService;

import java.io.IOException;
import java.sql.SQLException;

public class DetailsCreditController {

    @FXML private Label lblTitle, lblSecteurBadge, lblWalletSolde;
    @FXML private Label lblDescription;
    @FXML private Label lblMontant, lblTaux, lblDuree, lblProfit;
    @FXML private Label lblProjetDescription, lblProjetStatut;
    @FXML private ImageView imgLarge;

    private credit currentCredit;
    private wallet userWallet;
    private final RiskAnalysisService expertIA = new RiskAnalysisService();
    private final projetService ps = new projetService();

    public void initData(credit c) {
        if (c == null) return;
        this.currentCredit = c;

        // 1. Remplissage Crédit (Sécurisé)
        if (lblMontant != null) lblMontant.setText(String.format("%.2f TND", c.getMontant()));
        if (lblTaux != null) lblTaux.setText(String.format("%.1f %%", c.getTaux()));
        if (lblDuree != null) lblDuree.setText(c.getDuree() + " Mois");

        double profitTotal = (c.getMontant() * c.getTaux()) / 100;
        if (lblProfit != null) lblProfit.setText(String.format("+ %.2f TND", profitTotal));

        // 2. Remplissage Projet et Wallet
        try {
            userWallet = new wallet(1, 101, typeWallet.FIAT, "Principal", 25000.0, statutWallet.actif);
            if (lblWalletSolde != null) lblWalletSolde.setText(String.format("Solde: %.2f TND", userWallet.getSolde()));

            projet p = ps.findById(c.getProject_id());
            if (p != null) {
                if (lblTitle != null) lblTitle.setText(p.getTitle());
                if (lblSecteurBadge != null) lblSecteurBadge.setText(p.getSecteur().toUpperCase());
                if (lblProjetDescription != null) lblProjetDescription.setText(p.getDescription());
                if (lblProjetStatut != null) lblProjetStatut.setText(p.getSecteur());

                if (imgLarge != null && p.getImageUrl() != null && !p.getImageUrl().isEmpty()) {
                    imgLarge.setImage(new Image(p.getImageUrl(), true));
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }

        lancerExpertiseIA();
    }

    private void lancerExpertiseIA() {
        if (lblDescription != null) lblDescription.setText("L'Expert Mistral analyse le dossier... ⌛");
        new Thread(() -> {
            String resultIA = expertIA.getAiAnalysis(currentCredit, userWallet);
            Platform.runLater(() -> {
                if (lblDescription != null) lblDescription.setText(resultIA);
            });
        }).start();
    }

    @FXML
    private void negocierCredit() {
        try {
            // 1. Charger le fichier FXML de la négociation
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/invest/Negociation.fxml"));
            Parent root = loader.load();

            // 2. Récupérer le contrôleur de la page de négociation
            NegociationController controller = loader.getController();

            // 3. Lui passer le crédit actuel pour qu'il sache de quoi on discute
            controller.initData(currentCredit);

            // 4. Afficher la nouvelle interface dans la même fenêtre
            lblTitle.getScene().setRoot(root);

        } catch (IOException e) {
            System.err.println("Erreur lors de l'ouverture de la négociation : " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void investirCredit() {
        if (userWallet.getSolde() < currentCredit.getMontant()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Solde Insuffisant");
            alert.setContentText("Votre solde actuel ne permet pas cet investissement.");
            alert.showAndWait();
        } else {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Confirmer l'investissement de " + lblMontant.getText() + " ?", ButtonType.YES, ButtonType.NO);
            alert.showAndWait().ifPresent(response -> {
                if (response == ButtonType.YES) {
                    System.out.println("Action : Investissement enregistré !");
                }
            });
        }
    }

    @FXML
    private void retourMarketplace() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/invest/Marketplace.fxml"));
            lblTitle.getScene().setRoot(root);
        } catch (IOException e) { e.printStackTrace(); }
    }
}