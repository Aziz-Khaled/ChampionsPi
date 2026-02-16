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
import java.net.URL;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

public class StudentDashboardController {

    @FXML private FlowPane coursesGrid;
    @FXML private TextField searchField;

    private final FormationService fs = new FormationService();
    private List<formations> allFormations;

    @FXML
    public void initialize() {
        try {
            allFormations = fs.SelectAll();
            displayCourses(allFormations);

            // Logique de recherche en temps réel
            searchField.textProperty().addListener((observable, oldValue, newValue) -> {
                filterCourses(newValue);
            });
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void filterCourses(String keyword) {
        List<formations> filtered = allFormations.stream()
                .filter(f -> f.getTitre().toLowerCase().contains(keyword.toLowerCase()) ||
                        (f.getDomaine() != null && f.getDomaine().toLowerCase().contains(keyword.toLowerCase())))
                .collect(Collectors.toList());
        displayCourses(filtered);
    }

    private void displayCourses(List<formations> list) {
        coursesGrid.getChildren().clear();
        for (formations f : list) {
            coursesGrid.getChildren().add(createEnhancedCard(f));
        }
    }

    private VBox createEnhancedCard(formations f) {
        VBox card = new VBox(12);
        card.getStyleClass().add("formation-card");
        card.setPrefWidth(300);
        card.setPadding(new Insets(20));

        // Badge Domaine
        Label badge = new Label(f.getDomaine() != null ? f.getDomaine().toUpperCase() : "GENERAL");
        badge.getStyleClass().add("badge-domaine");

        // Titre
        Label title = new Label(f.getTitre());
        title.getStyleClass().add("formation-title");
        title.setWrapText(true);
        title.setMinHeight(50);

        // Zone Prix et Note
        HBox footer = new HBox();
        footer.setAlignment(Pos.CENTER_LEFT);

        Label price = new Label(f.getPrix() + " DT");
        price.getStyleClass().add("price-tag");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label rating = new Label("⭐ 4.8");
        rating.setStyle("-fx-text-fill: #f59e0b; -fx-font-weight: bold;");

        footer.getChildren().addAll(price, spacer, rating);

        // Bouton
        Button btnDetails = new Button("Voir Détails →");
        btnDetails.getStyleClass().add("button-primary");
        btnDetails.setMaxWidth(Double.MAX_VALUE);
        btnDetails.setOnAction(event -> loadPage(event, "/formation_details.fxml", f));

        card.getChildren().addAll(badge, title, footer, new Separator(), btnDetails);
        return card;
    }

    @FXML
    private void handleGoToCertifs(ActionEvent event) {
        loadPage(event, "/student_certificates.fxml", null);
    }

    private void loadPage(ActionEvent event, String fxmlPath, formations f) {
        try {
            URL resource = getClass().getResource(fxmlPath);
            if (resource == null) return;

            FXMLLoader loader = new FXMLLoader(resource);
            Parent root = loader.load();

            if (f != null && loader.getController() instanceof FormationDetailsController) {
                ((FormationDetailsController) loader.getController()).setFormation(f);
            }

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}