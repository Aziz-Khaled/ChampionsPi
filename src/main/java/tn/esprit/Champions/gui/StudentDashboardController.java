package tn.esprit.Champions.gui;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import tn.esprit.Champions.models.formations;
import tn.esprit.Champions.services.FormationService;

import java.io.IOException;
import java.util.List;

public class StudentDashboardController {

    @FXML private FlowPane coursesGrid;
    private final FormationService fs = new FormationService();

    @FXML
    public void initialize() {
        loadStudentCourses();
    }

    private void loadStudentCourses() {
        try {
            // Récupération des données depuis la base
            List<formations> list = fs.SelectAll();
            coursesGrid.getChildren().clear();

            for (formations f : list) {
                coursesGrid.getChildren().add(createStyledCard(f));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private VBox createStyledCard(formations f) {
        // Conteneur principal de la carte
        VBox card = new VBox(15);
        card.setPrefWidth(280);
        card.setPadding(new Insets(20));
        card.setStyle("-fx-background-color: white; " +
                "-fx-background-radius: 15; " +
                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 5); " +
                "-fx-cursor: hand;");

        // Catégorie / Domaine
        Label cat = new Label(f.getDomaine() != null ? f.getDomaine().toUpperCase() : "FORMATION");
        cat.setStyle("-fx-text-fill: #FE4A49; -fx-font-size: 10; -fx-font-weight: bold;");

        // Titre de la formation (Utilisation de getTitre())
        Label title = new Label(f.getTitre());
        title.setWrapText(true);
        title.setStyle("-fx-font-size: 18; -fx-font-weight: bold; -fx-text-fill: #2D3436;");
        title.setPrefHeight(60);
        title.setAlignment(Pos.TOP_LEFT);

        // Barre de progression factice pour le style
        ProgressBar pb = new ProgressBar(0.2);
        pb.setPrefWidth(240);
        pb.setStyle("-fx-accent: #FE4A49;");

        // Footer avec Prix et Bouton Détails
        HBox footer = new HBox();
        footer.setAlignment(Pos.CENTER_LEFT);

        Label price = new Label(f.getPrix() + " DT");
        price.setStyle("-fx-text-fill: #2D3436; -fx-font-weight: bold;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button btnDetails = new Button("Détails");
        btnDetails.setStyle("-fx-background-color: #FE4A49; -fx-text-fill: white; " +
                "-fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 8 15;");

        // --- ACTION : CLIC SUR DÉTAILS ---
        btnDetails.setOnAction(event -> handleViewDetails(event, f));

        footer.getChildren().addAll(price, spacer, btnDetails);
        card.getChildren().addAll(cat, title, pb, footer);

        return card;
    }

    private void handleViewDetails(ActionEvent event, formations f) {
        try {
            // Chargement de l'interface de détails (Chemin simplifié car à la racine de resources)
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/formation_details.fxml"));
            Parent root = loader.load();

            // Transmission de la formation sélectionnée au contrôleur suivant
            FormationDetailsController controller = loader.getController();
            controller.setFormation(f);

            // Changement de scène
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();

        } catch (IOException e) {
            System.err.println("Erreur lors du chargement des détails : " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleGoToCertifs(ActionEvent event) {
        try {
            // Chemin simplifié pour correspondre à ton dossier resources
            Parent root = FXMLLoader.load(getClass().getResource("/student_certificates.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (IOException e) {
            System.err.println("Erreur redirection certificats : " + e.getMessage());
        }
    }
}