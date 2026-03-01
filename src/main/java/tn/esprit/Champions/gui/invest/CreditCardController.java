package tn.esprit.Champions.gui.invest;

import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import tn.esprit.Champions.models.credit;
import tn.esprit.Champions.models.projet;
import tn.esprit.Champions.services.projetService;

import java.io.File;
import java.net.URL;

public class CreditCardController {

    @FXML private VBox cardRoot;
    @FXML private ImageView imgProject;
    @FXML private Label lblTitle, lblMontant, lblTaux, lblRisque, lblProgressionText;
    @FXML private ProgressBar progressFinancement;

    private final projetService ps = new projetService();
    private credit currentCredit;

    // Constantes de style pour éviter la répétition (Style Premium Dark)
    private static final String STYLE_RISQUE_HAUT = "-fx-background-color: rgba(239, 68, 68, 0.1); -fx-text-fill: #ef4444; -fx-padding: 8 15; -fx-background-radius: 20; -fx-font-size: 10; -fx-font-weight: bold; -fx-border-color: rgba(239, 68, 68, 0.3); -fx-border-radius: 20;";
    private static final String STYLE_RISQUE_BAS = "-fx-background-color: rgba(16, 185, 129, 0.1); -fx-text-fill: #10b981; -fx-padding: 8 15; -fx-background-radius: 20; -fx-font-size: 10; -fx-font-weight: bold; -fx-border-color: rgba(16, 185, 129, 0.3); -fx-border-radius: 20;";
    private static final String STYLE_RISQUE_MOYEN = "-fx-background-color: rgba(245, 158, 11, 0.1); -fx-text-fill: #fbbf24; -fx-padding: 8 15; -fx-background-radius: 20; -fx-font-size: 10; -fx-font-weight: bold; -fx-border-color: rgba(245, 158, 11, 0.3); -fx-border-radius: 20;";

    public void initialize() {
        // Ajout des effets de survol via code pour plus de fluidité
        setupHoverEffects();

        // Petit effet d'apparition (Fade In) au chargement
        cardRoot.setOpacity(0);
        FadeTransition ft = new FadeTransition(Duration.millis(800), cardRoot);
        ft.setToValue(1.0);
        ft.play();
    }

    public void setCreditData(credit c) {
        if (c == null) return;
        this.currentCredit = c;

        try {
            projet p = ps.findById(c.getProject_id());
            if (p != null) {
                lblTitle.setText(p.getTitle());
                chargerImageDynamique(p.getImageUrl());
            }

            lblMontant.setText(String.format("%.0f %s", c.getMontant(), c.getDevise() != null ? c.getDevise() : "DT"));
            lblTaux.setText(c.getTaux() + "%");

            configurerBadgeRisque(c);

            // On simule une progression aléatoire ou réelle si tu as le champ
            double progression = 0.65;
            animerProgression(progression);

        } catch (Exception e) {
            System.err.println("Erreur chargement CreditCard: " + e.getMessage());
        }
    }

    private void chargerImageDynamique(String path) {
        if (path == null || path.isEmpty()) {
            chargerImageParDefaut();
            return;
        }
        try {
            if (path.startsWith("http")) {
                imgProject.setImage(new Image(path, true));
            } else {
                File file = new File("src/main/resources" + path);
                if (file.exists()) {
                    imgProject.setImage(new Image(file.toURI().toString()));
                } else {
                    chargerImageParDefaut();
                }
            }
        } catch (Exception e) {
            chargerImageParDefaut();
        }
    }

    private void chargerImageParDefaut() {
        URL res = getClass().getResource("/images/default_project.png");
        if (res != null) imgProject.setImage(new Image(res.toExternalForm()));
    }

    private void configurerBadgeRisque(credit c) {
        String status = c.getStatus() != null ? c.getStatus().name() : "MODÉRÉ";
        lblRisque.setText("RISQUE : " + status);

        // Logique de couleur selon le taux (Exemple Fintech)
        if (c.getTaux() > 15) {
            lblRisque.setStyle(STYLE_RISQUE_HAUT);
        } else if (c.getTaux() > 8) {
            lblRisque.setStyle(STYLE_RISQUE_MOYEN);
        } else {
            lblRisque.setStyle(STYLE_RISQUE_BAS);
        }
    }

    private void animerProgression(double valeurCible) {
        progressFinancement.setProgress(0);
        Timeline timeline = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(progressFinancement.progressProperty(), 0)),
                new KeyFrame(Duration.seconds(1.2), new KeyValue(progressFinancement.progressProperty(), valeurCible))
        );
        timeline.play();

        if(lblProgressionText != null) {
            lblProgressionText.setText((int)(valeurCible * 100) + "%");
        }
    }

    private void setupHoverEffects() {
        cardRoot.setOnMouseEntered(e -> {
            Timeline timeline = new Timeline(
                    new KeyFrame(Duration.millis(200),
                            new KeyValue(cardRoot.translateYProperty(), -10),
                            new KeyValue(cardRoot.scaleXProperty(), 1.02),
                            new KeyValue(cardRoot.scaleYProperty(), 1.02)
                    )
            );
            timeline.play();
            cardRoot.setStyle(cardRoot.getStyle() + "-fx-effect: dropshadow(three-pass-box, rgba(56, 189, 248, 0.2), 30, 0, 0, 15);");
        });

        cardRoot.setOnMouseExited(e -> {
            Timeline timeline = new Timeline(
                    new KeyFrame(Duration.millis(200),
                            new KeyValue(cardRoot.translateYProperty(), 0),
                            new KeyValue(cardRoot.scaleXProperty(), 1.0),
                            new KeyValue(cardRoot.scaleYProperty(), 1.0)
                    )
            );
            timeline.play();
            cardRoot.setStyle(cardRoot.getStyle() + "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.4), 20, 0, 0, 15);");
        });
    }

    @FXML
    private void handleInvest() {
        System.out.println("Ouverture des détails pour le projet : " + lblTitle.getText());
    }
}