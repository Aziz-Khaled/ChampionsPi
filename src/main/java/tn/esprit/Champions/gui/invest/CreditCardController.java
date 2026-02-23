package tn.esprit.Champions.gui.invest;

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
    @FXML private Label lblTitle, lblMontant, lblTaux, lblRisque, lblSecteurBadge, lblPercentage;
    @FXML private ProgressBar progressFinancement;

    private final projetService ps = new projetService();
    private credit currentCredit;

    public void setCreditData(credit c) {
        if (c == null) return;
        this.currentCredit = c;

        try {
            projet p = ps.findById(c.getProject_id());

            if (p != null) {
                lblTitle.setText(p.getTitle());
                if (lblSecteurBadge != null) lblSecteurBadge.setText(p.getSecteur());

                // --- GESTION DES IMAGES IA & LOCALES ---
                chargerImageDynamique(p.getImageUrl());
            }

            // Formatage monétaire
            lblMontant.setText(String.format("%.0f %s", c.getMontant(), c.getDevise()));
            lblTaux.setText(c.getTaux() + "%");

            // Style dynamique du Risque/Status
            configurerBadgeRisque(c);

            // Animation de la barre de progression
            animerProgression(0.65);

        } catch (Exception e) {
            System.err.println("Erreur chargement CreditCard: " + e.getMessage());
        }
    }

    /**
     * Charge l'image intelligemment selon qu'il s'agisse d'une URL web
     * ou d'un fichier généré localement par l'IA.
     */
    private void chargerImageDynamique(String path) {
        if (path == null || path.isEmpty()) {
            chargerImageParDefaut();
            return;
        }

        try {
            if (path.startsWith("http")) {
                // Cas : URL Web (Anciennes images ou web)
                imgProject.setImage(new Image(path, true));
            } else {
                // Cas : Image générée par Gemini/Flux stockée dans /uploads/
                // On pointe vers src/main/resources + le chemin stocké (/uploads/...)
                File file = new File("src/main/resources" + path);

                if (file.exists()) {
                    // toURI().toString() est obligatoire pour transformer le chemin disque en URL JavaFX valide
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
        try {
            // Chargement depuis le classpath (ressources internes)
            URL res = getClass().getResource("/images/default_project.png");
            if (res != null) {
                imgProject.setImage(new Image(res.toExternalForm()));
            }
        } catch (Exception e) {
            System.err.println("Image par défaut introuvable dans les ressources.");
        }
    }

    private void configurerBadgeRisque(credit c) {
        String status = c.getStatus() != null ? c.getStatus().name() : "OPEN";
        lblRisque.setText(status);

        if (c.getTaux() > 10) {
            lblRisque.setStyle("-fx-background-color: #FFEBEE; -fx-text-fill: #C62828; -fx-padding: 4 10; -fx-background-radius: 10; -fx-font-weight: bold;");
        } else {
            lblRisque.setStyle("-fx-background-color: #E8F5E9; -fx-text-fill: #2E7D32; -fx-padding: 4 10; -fx-background-radius: 10; -fx-font-weight: bold;");
        }
    }

    private void animerProgression(double valeurCible) {
        progressFinancement.setProgress(0);
        Timeline timeline = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(progressFinancement.progressProperty(), 0)),
                new KeyFrame(Duration.seconds(1.5), new KeyValue(progressFinancement.progressProperty(), valeurCible))
        );
        timeline.play();
        if(lblPercentage != null) lblPercentage.setText((int)(valeurCible * 100) + "%");
    }

    @FXML
    private void onHoverEnter() {
        cardRoot.setStyle("-fx-background-color: white; " +
                "-fx-background-radius: 25; " +
                "-fx-effect: dropshadow(three-pass-box, rgba(39, 174, 96, 0.25), 30, 0, 0, 15); " +
                "-fx-translate-y: -10; " +
                "-fx-cursor: hand;");
    }

    @FXML
    private void onHoverExit() {
        cardRoot.setStyle("-fx-background-color: white; " +
                "-fx-background-radius: 25; " +
                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 20, 0, 0, 10); " +
                "-fx-translate-y: 0; " +
                "-fx-cursor: hand;");
    }

    @FXML
    private void handleInvest() {
        System.out.println("Navigation vers les détails : " + currentCredit.getId());
    }
}