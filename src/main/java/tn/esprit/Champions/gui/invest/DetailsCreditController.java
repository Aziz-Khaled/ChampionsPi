package tn.esprit.Champions.gui.invest;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.util.Duration;
import tn.esprit.Champions.models.credit;
import tn.esprit.Champions.models.projet;
import tn.esprit.Champions.services.projetService;

import java.io.IOException;
import java.sql.SQLException;

public class DetailsCreditController {

    @FXML private Label lblTitle, lblDescription, lblSecteur, lblMontant, lblTaux, lblScoreRisque;
    @FXML private ImageView imgLarge;

    private final projetService ps = new projetService();

    /**
     * Initialise les données avec une petite animation d'entrée
     */
    public void initData(credit c) {
        if (c == null) return;

        try {
            projet p = ps.findById(c.getProject_id());
            if (p != null) {
                lblTitle.setText(p.getTitle());
                lblDescription.setText(p.getDescription() != null ? p.getDescription() : "Aucune description disponible pour ce projet.");
                lblSecteur.setText(p.getSecteur());

                if (p.getImageUrl() != null && !p.getImageUrl().isEmpty()) {
                    // Chargement asynchrone pour éviter de figer l'UI
                    Image image = new Image(p.getImageUrl(), true);
                    imgLarge.setImage(image);
                }
            }

            lblMontant.setText(String.format("%.2f %s", c.getMontant(), c.getDevise()));
            lblTaux.setText(c.getTaux() + " %");

            // Réinitialiser le score IA
            lblScoreRisque.setText("Analyse IA disponible");
            lblScoreRisque.setStyle("-fx-text-fill: #b2bec3; -fx-font-weight: bold;");

        } catch (SQLException e) {
            System.err.println("Erreur SQL lors de l'initialisation des détails : " + e.getMessage());
        }
    }

    /**
     * Simule une analyse IA avec animation de texte
     */
    @FXML
    private void calculerRisqueIA() {
        lblScoreRisque.setText("🔄 Analyse des données en cours...");
        lblScoreRisque.setStyle("-fx-text-fill: #2980b9; -fx-font-weight: bold;");

        // Petite pause pour simuler un calcul complexe
        PauseTransition pause = new PauseTransition(Duration.seconds(1.5));
        pause.setOnFinished(event -> {
            double score = 75.0 + (Math.random() * 20.0);
            lblScoreRisque.setText(String.format("Score de fiabilité : %.2f%%", score));

            // Changement de couleur selon le résultat
            if (score > 85) {
                lblScoreRisque.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: 900; -fx-font-size: 22px;");
            } else {
                lblScoreRisque.setStyle("-fx-text-fill: #f39c12; -fx-font-weight: 900; -fx-font-size: 22px;");
            }
        });
        pause.play();
    }

    /**
     * Retour à la Marketplace avec gestion d'erreur
     */
    @FXML
    private void retourMarketplace() {
        try {
            // Assure-toi que le chemin vers Marketplace.fxml est exact
            Parent root = FXMLLoader.load(getClass().getResource("/invest/Marketplace.fxml"));

            // Animation de transition (Fade Out)
            FadeTransition ft = new FadeTransition(Duration.millis(300), lblTitle.getScene().getRoot());
            ft.setFromValue(1.0);
            ft.setToValue(0.0);
            ft.setOnFinished(e -> lblTitle.getScene().setRoot(root));
            ft.play();

        } catch (IOException e) {
            System.err.println("Erreur lors du retour à la Marketplace : " + e.getMessage());
        }
    }
}