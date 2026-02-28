package tn.esprit.Champions.gui;

import javafx.animation.FadeTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.layout.StackPane;
import javafx.scene.control.Button;
import javafx.util.Duration;
import java.io.IOException;

public class MainController {

    @FXML private StackPane contentArea;
    @FXML private Button btnFormations, btnParticipations, btnCertificats, btnReclamations;

    @FXML
    public void initialize() {
        // Charge la vue par défaut au démarrage
        showFormations();
    }

    @FXML
    public void showFormations() {
        loadView("/interface.fxml");
        updateUI(btnFormations);
    }

    @FXML
    public void showParticipations() {
        loadView("/participation_interface.fxml");
        updateUI(btnParticipations);
    }

    @FXML
    public void showCertificats() {
        loadView("/certificat_interface.fxml");
        updateUI(btnCertificats);
    }

    @FXML
    public void showReclamations() {
        // Assure-toi que le nom du fichier FXML est exact (BackOfficeReclamations.fxml)
        loadView("/BackOfficeReclamations.fxml");
        updateUI(btnReclamations);
    }

    private void loadView(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent view = loader.load();

            // Animation de transition fluide
            view.setOpacity(0);
            contentArea.getChildren().setAll(view);

            FadeTransition fade = new FadeTransition(Duration.millis(400), view);
            fade.setFromValue(0);
            fade.setToValue(1);
            fade.play();
        } catch (IOException e) {
            System.err.println("Erreur de chargement : " + fxmlPath);
            e.printStackTrace();
        } catch (NullPointerException e) {
            System.err.println("Chemin FXML introuvable : " + fxmlPath);
        }
    }

    private void updateUI(Button selected) {
        Button[] btns = {btnFormations, btnParticipations, btnCertificats, btnReclamations};

        String selectedStyle = "-fx-background-color: #2c3e50; -fx-text-fill: white; -fx-font-size: 16; -fx-cursor: hand; -fx-alignment: CENTER_LEFT; -fx-pref-width: 210; -fx-background-radius: 5;";
        String defaultStyle = "-fx-background-color: transparent; -fx-text-fill: #bdc3c7; -fx-font-size: 16; -fx-cursor: hand; -fx-alignment: CENTER_LEFT; -fx-pref-width: 210;";

        for (Button b : btns) {
            if (b != null) {
                b.setStyle(b == selected ? selectedStyle : defaultStyle);
            }
        }
    }
}