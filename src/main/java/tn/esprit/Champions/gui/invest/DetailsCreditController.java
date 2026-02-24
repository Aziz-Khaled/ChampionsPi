package tn.esprit.Champions.gui.invest;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import tn.esprit.Champions.models.*;
import tn.esprit.Champions.services.RiskAnalysisService;
import tn.esprit.Champions.services.projetService;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;

public class DetailsCreditController {

    @FXML private VBox detailsRoot;
    @FXML private Label lblTitle, lblSecteurBadge, lblWalletSolde;
    @FXML private Label lblDescription;
    @FXML private Label lblMontant, lblTaux, lblDuree, lblProfit;
    @FXML private Label lblProjetDescription, lblProjetStatut;
    @FXML private ImageView imgLarge;

    // Conteneurs pour les animations (à vérifier dans votre FXML si vous voulez animer des blocs précis)
    @FXML private VBox boxAnalyseIA, boxConditions, boxProjet;

    private credit currentCredit;
    private wallet userWallet;
    private final RiskAnalysisService expertIA = new RiskAnalysisService();
    private final projetService ps = new projetService();

    @FXML
    public void initialize() {
        // Cacher les éléments au départ pour l'animation
        detailsRoot.setOpacity(0);
    }

    public void initData(credit c) {
        if (c == null) return;
        this.currentCredit = c;

        // 1. Remplissage des données
        remplirInterface(c);

        // 2. Lancement des animations de l'interface
        jouerAnimationsEntree();

        // 3. Expertise IA en arrière-plan
        lancerExpertiseIA();
    }

    private void remplirInterface(credit c) {
        lblMontant.setText(String.format("%.2f TND", c.getMontant()));
        lblTaux.setText(String.format("%.1f %%", c.getTaux()));
        lblDuree.setText(c.getDuree() + " Mois");

        double profitTotal = (c.getMontant() * c.getTaux()) / 100;
        lblProfit.setText(String.format("+ %.2f TND", profitTotal));

        try {
            // Simulation Wallet (A remplacer par votre service de session plus tard)
            userWallet = new wallet(1, 101, typeWallet.FIAT, "Principal", 25000.0, statutWallet.actif);
            lblWalletSolde.setText(String.format("Solde: %.2f TND", userWallet.getSolde()));

            projet p = ps.findById(c.getProject_id());
            if (p != null) {
                lblTitle.setText(p.getTitle());
                lblSecteurBadge.setText(p.getSecteur().toUpperCase());
                lblProjetDescription.setText(p.getDescription());
                lblProjetStatut.setText("STATUT : " + p.getSecteur()); // Ou p.getStatut()

                chargerImageDynamique(p.getImageUrl());
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void chargerImageDynamique(String path) {
        if (path == null || path.isEmpty()) return;
        try {
            if (path.startsWith("http")) {
                imgLarge.setImage(new Image(path, true));
            } else {
                File file = new File("src/main/resources" + path);
                if (file.exists()) {
                    imgLarge.setImage(new Image(file.toURI().toString()));
                }
            }
        } catch (Exception e) {
            System.err.println("Erreur image détails: " + e.getMessage());
        }
    }

    private void jouerAnimationsEntree() {
        // 1. Fade In global de la page
        FadeTransition fadeIn = new FadeTransition(Duration.millis(1000), detailsRoot);
        fadeIn.setToValue(1.0);
        fadeIn.play();

        // 2. Animation "Slide Up" pour les composants
        // On fait monter l'image et les blocs de 20 pixels en douceur
        animerSlideUp(imgLarge, 0);
        animerSlideUp(lblTitle, 100);

        // Si vous avez mis des fx:id sur vos VBox dans le FXML :
        if (boxAnalyseIA != null) animerSlideUp(boxAnalyseIA, 200);
        if (boxConditions != null) animerSlideUp(boxConditions, 300);
        if (boxProjet != null) animerSlideUp(boxProjet, 400);
    }

    private void animerSlideUp(Node node, int delayMs) {
        if (node == null) return;
        node.setTranslateY(30);
        node.setOpacity(0);

        Timeline timeline = new Timeline(
                new KeyFrame(Duration.millis(delayMs),
                        new KeyValue(node.translateYProperty(), 30),
                        new KeyValue(node.opacityProperty(), 0)
                ),
                new KeyFrame(Duration.millis(delayMs + 600),
                        new KeyValue(node.translateYProperty(), 0, Interpolator.EASE_OUT),
                        new KeyValue(node.opacityProperty(), 1)
                )
        );
        timeline.play();
    }

    private void lancerExpertiseIA() {
        lblDescription.setText("L'Expert IA analyse votre profil investisseur... 🔍");
        new Thread(() -> {
            String resultIA = expertIA.getAiAnalysis(currentCredit, userWallet);
            Platform.runLater(() -> {
                // Petit effet de fondu pour l'apparition du texte de l'IA
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
        if (userWallet.getSolde() < currentCredit.getMontant()) {
            afficherAlerte("Solde Insuffisant", "Votre solde actuel est trop bas.", Alert.AlertType.ERROR);
        } else {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Confirmer l'investissement ?", ButtonType.YES, ButtonType.NO);
            // Application du style sombre à l'alerte si possible ou simple confirmation
            alert.showAndWait().ifPresent(response -> {
                if (response == ButtonType.YES) {
                    System.out.println("Investissement validé !");
                    // Ajouter ici l'appel service pour soustraire le montant
                }
            });
        }
    }

    private void afficherAlerte(String titre, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(titre);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @FXML
    private void retourMarketplace() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/invest/Marketplace.fxml"));
            lblTitle.getScene().setRoot(root);
        } catch (IOException e) { e.printStackTrace(); }
    }

    @FXML
    private void negocierCredit() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/invest/Negociation.fxml"));
            Parent root = loader.load();
            NegociationController controller = loader.getController();
            controller.initData(currentCredit);
            lblTitle.getScene().setRoot(root);
        } catch (IOException e) { e.printStackTrace(); }
    }
}