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
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.shape.Rectangle;
import javafx.stage.Modality;
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

            // Recherche en temps réel
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
        VBox card = new VBox(15);
        card.getStyleClass().add("formation-card");
        card.setPrefWidth(300);
        card.setPadding(new Insets(0, 0, 20, 0)); // Padding 0 en haut pour l'image

        // --- 1. GESTION DE L'IMAGE ---
        ImageView imageView = new ImageView();
        try {
            // Charge l'image depuis src/main/resources/images/
            String imagePath = "/images/" + f.getImagePath();
            Image img = new Image(getClass().getResourceAsStream(imagePath));
            imageView.setImage(img);
        } catch (Exception e) {
            // Image par défaut si le chemin en BDD est vide ou incorrect
            imageView.setImage(new Image("https://images.unsplash.com/photo-1516321318423-f06f85e504b3?w=300"));
        }

        imageView.setFitWidth(300);
        imageView.setFitHeight(170);
        imageView.setPreserveRatio(false);

        // Arrondir les coins supérieurs de l'image (Style GomyCode)
        Rectangle clip = new Rectangle(300, 170);
        clip.setArcWidth(30);
        clip.setArcHeight(30);
        imageView.setClip(clip);

        // --- 2. CONTENU TEXTUEL ---
        VBox textContent = new VBox(10);
        textContent.setPadding(new Insets(0, 20, 0, 20));

        Label badge = new Label(f.getDomaine() != null ? f.getDomaine().toUpperCase() : "GENERAL");
        badge.getStyleClass().add("badge-domaine");

        Label title = new Label(f.getTitre());
        title.getStyleClass().add("formation-title");
        title.setWrapText(true);
        title.setMinHeight(50);

        // Footer : Prix et Rating
        HBox footer = new HBox();
        footer.setAlignment(Pos.CENTER_LEFT);
        Label price = new Label(f.getPrix() + " DT");
        price.getStyleClass().add("price-tag");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label rating = new Label("⭐ 4.8");
        rating.setStyle("-fx-text-fill: #f59e0b; -fx-font-weight: bold;");
        footer.getChildren().addAll(price, spacer, rating);

        // Bouton Détails
        Button btnDetails = new Button("Voir Détails →");
        btnDetails.getStyleClass().add("button-primary");
        btnDetails.setMaxWidth(Double.MAX_VALUE);
        btnDetails.setOnAction(event -> loadPage(event, "/formation_details.fxml", f));

        textContent.getChildren().addAll(badge, title, footer, new Separator(), btnDetails);

        // Assemblage final : Image puis Texte
        card.getChildren().addAll(imageView, textContent);
        return card;
    }

    @FXML
    private void handleGoToCertifs(ActionEvent event) {
        loadPage(event, "/student_certificates.fxml", null);
    }

    @FXML
    private void handleOpenReclamations(ActionEvent event) {
        try {
            URL resource = getClass().getResource("/ReclamationForm.fxml");
            if (resource == null) {
                System.err.println("Erreur : Fichier ReclamationForm.fxml introuvable !");
                return;
            }
            FXMLLoader loader = new FXMLLoader(resource);
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle("Déposer une réclamation");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleGoToMyReclamations(ActionEvent event) {
        loadPage(event, "/mes_reclamations.fxml", null);
    }

    private void loadPage(ActionEvent event, String fxmlPath, formations f) {
        try {
            // 1. On vérifie d'abord si le chemin commence par un '/'
            if (!fxmlPath.startsWith("/")) {
                fxmlPath = "/" + fxmlPath;
            }

            URL resource = getClass().getResource(fxmlPath);
            if (resource == null) {
                System.err.println("Erreur : Chemin FXML non trouvé : " + fxmlPath);
                return;
            }

            FXMLLoader loader = new FXMLLoader(resource);
            Parent root = loader.load();

            // --- ÉTAPE CRUCIALE : TRANSMISSION DES DONNÉES ---
            Object controller = loader.getController();

            // On vérifie si on va vers la page de détails et si on a une formation
            if (f != null && controller instanceof FormationDetailsController) {
                ((FormationDetailsController) controller).setFormation(f);
                System.out.println("Données envoyées au contrôleur : " + f.getTitre());
            }

            // Changement de scène
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();

        } catch (IOException e) {
            System.err.println("Erreur lors du chargement de la page : " + fxmlPath);
            e.printStackTrace();
        }

    }
    @FXML
    private void handleBackToWallet(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/DashboardWalletClient.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.getScene().setRoot(root);
        } catch (IOException e) {
            System.err.println("Erreur lors du retour au dashboard : " + e.getMessage());
        }
    }
}