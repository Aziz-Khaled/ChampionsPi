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
    @FXML private Button btnFormations, btnParticipations, btnCertificats;

    @FXML
    public void initialize() {
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
        // CORRIGÉ selon votre capture d'écran
        loadView("/certificat_interface.fxml");
        updateUI(btnCertificats);
    }

    private void loadView(String fxmlPath) {
        try {
            Parent view = FXMLLoader.load(getClass().getResource(fxmlPath));
            view.setOpacity(0);
            contentArea.getChildren().setAll(view);

            FadeTransition fade = new FadeTransition(Duration.millis(400), view);
            fade.setFromValue(0);
            fade.setToValue(1);
            fade.play();
        } catch (IOException e) {
            System.err.println("Fichier introuvable : " + fxmlPath);
            e.printStackTrace();
        }
    }

    private void updateUI(Button selected) {
        Button[] btns = {btnFormations, btnParticipations, btnCertificats};
        for (Button b : btns) {
            if (b == null) continue;
            b.setStyle(b == selected ?
                    "-fx-background-color: #2c3e50; -fx-text-fill: white;" :
                    "-fx-background-color: transparent; -fx-text-fill: #bdc3c7;");
        }
    }
}