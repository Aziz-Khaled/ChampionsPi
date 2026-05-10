package tn.esprit.Champions.gui.invest;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import tn.esprit.Champions.models.credit;
import tn.esprit.Champions.models.projet;
import tn.esprit.Champions.models.statutWallet;
import tn.esprit.Champions.models.wallet;
import tn.esprit.Champions.services.RiskAnalysisService;
import tn.esprit.Champions.services.projetService;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;

public class DetailsCreditController {

    @FXML
    private VBox detailsRoot;
    @FXML
    private Label lblTitle, lblSecteurBadge, lblWalletSolde, lblDescription;
    @FXML
    private Label lblMontant, lblTaux, lblDuree, lblProfit;
    @FXML
    private Label lblProjetDescription, lblProjetStatut;
    @FXML
    private ImageView imgLarge;
    @FXML
    private VBox boxAnalyseIA, boxConditions, boxProjet;

    private credit currentCredit;
    private wallet userWallet;
    private final RiskAnalysisService expertIA = new RiskAnalysisService();
    private final projetService ps = new projetService();

    @FXML
    public void initialize() {
        detailsRoot.setOpacity(0);
    }

    public void initData(credit c) {
        if (c == null)
            return;
        this.currentCredit = c;
        remplirInterface(c);
        jouerAnimationsEntree();
        lancerExpertiseIA();
    }

    private void remplirInterface(credit c) {
        lblMontant.setText(String.format("%.2f TND", c.getMontant()));
        lblTaux.setText(String.format("%.1f %%", c.getTaux()));
        lblDuree.setText(c.getDuree() + " Mois");

        double profitTotal = (c.getMontant() * c.getTaux()) / 100;
        lblProfit.setText(String.format("+ %.2f TND", profitTotal));

        try {
            // Simulation Wallet (Base statique)
            userWallet = new wallet();
            userWallet.setIdWallet(1);
            userWallet.setRib("TN59 0001 0002 0003");
            userWallet.setSolde(25000.0);
            userWallet.setStatut(statutWallet.actif);

            lblWalletSolde.setText(String.format("Solde: %.2f TND", userWallet.getSolde()));

            projet p = ps.findById(c.getProject_id());
            if (p != null) {
                lblTitle.setText(p.getTitle());
                lblSecteurBadge.setText(p.getSecteur().toUpperCase());
                lblProjetDescription.setText(p.getDescription());
                lblProjetStatut.setText("STATUT : " + p.getSecteur());
                chargerImageDynamique(p.getImage_url());
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void chargerImageDynamique(String path) {
        if (path == null || path.isEmpty())
            return;
        try {
            if (path.startsWith("http")) {
                imgLarge.setImage(new Image(path, true));
            } else {
                // Correction du chemin pour les ressources internes
                String resourcePath = path.startsWith("/") ? path : "/" + path;
                var res = getClass().getResource(resourcePath);
                if (res != null) {
                    imgLarge.setImage(new Image(res.toExternalForm()));
                } else {
                    File file = new File("src/main/resources" + path);
                    if (file.exists()) {
                        imgLarge.setImage(new Image(file.toURI().toString()));
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Erreur image détails: " + e.getMessage());
        }
    }

    private void jouerAnimationsEntree() {
        FadeTransition fadeIn = new FadeTransition(Duration.millis(1000), detailsRoot);
        fadeIn.setToValue(1.0);
        fadeIn.play();

        animerSlideUp(imgLarge, 0);
        animerSlideUp(lblTitle, 100);
        animerSlideUp(boxAnalyseIA, 200);
        animerSlideUp(boxConditions, 300);
        animerSlideUp(boxProjet, 400);
    }

    private void animerSlideUp(Node node, int delayMs) {
        if (node == null)
            return;
        node.setTranslateY(30);
        node.setOpacity(0);

        Timeline timeline = new Timeline(
                new KeyFrame(Duration.millis(delayMs),
                        new KeyValue(node.translateYProperty(), 30),
                        new KeyValue(node.opacityProperty(), 0)),
                new KeyFrame(Duration.millis(delayMs + 600),
                        new KeyValue(node.translateYProperty(), 0, Interpolator.EASE_OUT),
                        new KeyValue(node.opacityProperty(), 1)));
        timeline.play();
    }

    private void lancerExpertiseIA() {
        lblDescription.setText("L'Expert IA analyse votre profil investisseur... 🔍");
        new Thread(() -> {
            String resultIA = expertIA.getAiAnalysis(currentCredit, userWallet);
            Platform.runLater(() -> {
                lblDescription.setOpacity(0);
                lblDescription.setText(resultIA);
                FadeTransition ft = new FadeTransition(Duration.millis(800), lblDescription);
                ft.setToValue(1.0);
                ft.play();
            });
        }).start();
    }

    @FXML
    private void investirCredit() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/invest/ConfirmTransaction.fxml"));
            Parent root = loader.load();
            ConfirmTransactionController controller = loader.getController();
            controller.setTransactionData(currentCredit, userWallet);

            Stage stage = new Stage();
            stage.initStyle(StageStyle.UNDECORATED);
            stage.setTitle("Confirmation");
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            afficherAlerte("Erreur", "Impossible d'ouvrir l'interface de paiement.", Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void handleRetourMarketplace(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/invest/Marketplace.fxml"));
            detailsRoot.getScene().setRoot(root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void negocierCredit() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/invest/Negociation.fxml"));
            Parent root = loader.load();
            NegociationController controller = loader.getController();
            controller.initData(currentCredit);
            detailsRoot.getScene().setRoot(root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void afficherAlerte(String titre, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(titre);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}