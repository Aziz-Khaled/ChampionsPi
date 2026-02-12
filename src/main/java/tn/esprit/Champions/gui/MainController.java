package tn.esprit.Champions.gui;

import javafx.animation.FadeTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.layout.StackPane;
import javafx.scene.control.Button;
import javafx.util.Duration;
import java.io.IOException;

public class MainController {

    @FXML private StackPane contentArea;
    @FXML private Button btnFormations;
    @FXML private Button btnParticipations;

    @FXML
    public void initialize() {
        // Charger les formations par défaut au démarrage
        showFormations();
    }

    @FXML
    public void showFormations() {
        loadView("/interface.fxml");
        updateButtonStyle(btnFormations, btnParticipations);
    }

    @FXML
    public void showParticipations() {
        loadView("/participation_interface.fxml");
        updateButtonStyle(btnParticipations, btnFormations);
    }

    private void loadView(String fxmlPath) {
        try {
            // 1. Charger le nouveau fichier FXML
            Parent view = FXMLLoader.load(getClass().getResource(fxmlPath));

            // 2. Préparer l'animation de fondu
            view.setOpacity(0); // On commence invisible
            contentArea.getChildren().clear();
            contentArea.getChildren().add(view);

            // 3. Lancer l'animation (durée: 400 millisecondes)
            FadeTransition fade = new FadeTransition(Duration.millis(400), view);
            fade.setFromValue(0);
            fade.setToValue(1);
            fade.play();

        } catch (IOException e) {
            System.err.println("Erreur de chargement : " + e.getMessage());
        }
    }

    private void updateButtonStyle(Button selected, Button other) {
        // Style pour le bouton actif (Bleu sombre/Cyan)
        selected.setStyle("-fx-text-fill: white; -fx-background-color: #2c3e50; -fx-background-radius: 8; -fx-font-weight: bold; -fx-alignment: CENTER_LEFT; -fx-pref-width: 210; -fx-padding: 10 15;");
        // Style pour le bouton inactif
        other.setStyle("-fx-text-fill: #bdc3c7; -fx-background-color: transparent; -fx-alignment: CENTER_LEFT; -fx-pref-width: 210; -fx-padding: 10 15;");
    }
}