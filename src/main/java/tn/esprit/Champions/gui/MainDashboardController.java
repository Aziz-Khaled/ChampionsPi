package tn.esprit.Champions.gui;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Priority;
import java.io.IOException;

public class MainDashboardController {

    @FXML private VBox mainContent;
    @FXML private Button btnCredits;
    @FXML private Button btnProjets;
    @FXML private Button btnNegociations;

    @FXML
    public void initialize() {
        // Gestion du clic pour les Projets
        btnProjets.setOnAction(event -> chargerVue("/AfficherProjets.fxml"));

        // Gestion du clic pour les Crédits
        btnCredits.setOnAction(event -> chargerVue("/AfficherCredits.fxml"));
    }

    private void chargerVue(String fxmlPath) {
        try {
            // 1. Charger le fichier FXML
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent view = loader.load();

            // 2. Nettoyer le centre du Dashboard (la zone grise/blanche)
            mainContent.getChildren().clear();

            // 3. Ajouter la nouvelle interface
            mainContent.getChildren().add(view);

            // 4. Forcer l'interface à prendre toute la place
            VBox.setVgrow(view, Priority.ALWAYS);

            System.out.println("Vue chargée : " + fxmlPath);
        } catch (IOException e) {
            System.err.println("Erreur de chargement : " + fxmlPath);
            e.printStackTrace();
        }
    }
}