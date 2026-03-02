package tn.esprit.Champions.gui;

import io.github.cdimascio.dotenv.Dotenv;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.json.JSONArray;
import org.json.JSONObject;
import tn.esprit.Champions.models.Utilisateur;
import tn.esprit.Champions.utils.UserSession;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class MainDashboardController {

    @FXML private VBox mainContent;
    @FXML private FlowPane newsContainer;
    @FXML private Label lblTicker, lblWelcome;
    @FXML private Button btnDashboard, btnCredits, btnProjets;

    // Sauvegarde de la vue d'accueil pour permettre le retour en arrière
    private List<Node> initialDashboardNodes;

    // CONFIGURATION SÉCURISÉE VIA DOTENV
    private static final Dotenv dotenv = Dotenv.load();
    private final String API_KEY = dotenv.get("NEWS_API_KEY");
    private final String NEWS_URL = "https://newsapi.org/v2/everything?q=fintech+investment&language=fr&sortBy=publishedAt&apiKey=" + API_KEY;

    @FXML
    public void initialize() {
        Utilisateur currentUser = UserSession.getLoggedInUser();

        if (currentUser != null) {
            lblWelcome.setText("Bienvenue, " + currentUser.getNom() + " " + currentUser.getPrenom() + " 👋");
        } else {
            lblWelcome.setText("Bienvenue 👋");
        }

        // Sauvegarde immédiate des éléments de l'accueil (Stats + News Container)
        Platform.runLater(() -> {
            if (initialDashboardNodes == null) {
                initialDashboardNodes = new ArrayList<>(mainContent.getChildren());
            }
        });

        lancerTicker();
        chargerNewsAPI();
        mettreAJourStyleBouton(btnDashboard);
    }

    // --- NAVIGATION ---

    @FXML
    private void showHome() {
        if (initialDashboardNodes != null) {
            // On restaure les éléments sauvegardés
            mainContent.getChildren().setAll(initialDashboardNodes);

            // On réassigne la référence du newsContainer car il a été réinjecté
            newsContainer = (FlowPane) mainContent.lookup("#newsContainer");

            appliquerTransition(mainContent);
        }
        chargerNewsAPI();
        mettreAJourStyleBouton(btnDashboard);
    }

    @FXML
    private void showProjets() {
        chargerModule("/AfficherProjets.fxml", btnProjets);
    }

    @FXML
    private void showCredits() {
        chargerModule("/AfficherCredits.fxml", btnCredits);
    }

    private void chargerModule(String fxmlPath, Button source) {
        try {
            URL res = getClass().getResource(fxmlPath);
            if (res == null) {
                System.err.println("❌ Fichier FXML introuvable : " + fxmlPath);
                return;
            }
            Parent view = FXMLLoader.load(res);
            mainContent.getChildren().setAll(view);
            appliquerTransition(view);
            mettreAJourStyleBouton(source);
        } catch (IOException e) {
            System.err.println("❌ Erreur lors du chargement du module : " + e.getMessage());
        }
    }

    // --- LOGIQUE API NEWS ---

    private void chargerNewsAPI() {
        if (API_KEY == null || API_KEY.isEmpty()) {
            System.err.println("⚠️ NEWS_API_KEY manquante dans le fichier .env");
            return;
        }

        Thread thread = new Thread(() -> {
            try {
                String response = callAPI(NEWS_URL);
                JSONObject jsonResponse = new JSONObject(response);
                JSONArray articles = jsonResponse.getJSONArray("articles");

                Platform.runLater(() -> {
                    if (newsContainer != null) {
                        newsContainer.getChildren().clear();
                        // Affichage de 10 articles max
                        for (int i = 0; i < Math.min(articles.length(), 10); i++) {
                            JSONObject art = articles.getJSONObject(i);
                            String articleUrl = art.optString("url", "");

                            newsContainer.getChildren().add(createNewsCard(
                                    art.getString("title"),
                                    art.optString("urlToImage", "https://via.placeholder.com/300x150"),
                                    art.getJSONObject("source").getString("name"),
                                    articleUrl
                            ));
                        }
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    if (newsContainer != null) newsContainer.getChildren().add(new Label("Erreur de connexion aux news"));
                });
            }
        });
        thread.setDaemon(true);
        thread.start();
    }

    private String callAPI(String urlString) throws Exception {
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        try (BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            StringBuilder result = new StringBuilder();
            String line;
            while ((line = rd.readLine()) != null) result.append(line);
            return result.toString();
        }
    }

    private VBox createNewsCard(String title, String imageUrl, String source, String articleUrl) {
        VBox card = new VBox(10);
        card.setPrefSize(300, 250);
        card.setCursor(Cursor.HAND);
        card.setStyle("-fx-background-color: #1e293b; -fx-background-radius: 15; -fx-padding: 15; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.4), 10, 0, 0, 5);");

        ImageView img = new ImageView();
        try {
            img.setImage(new Image(imageUrl, 270, 130, false, true));
        } catch (Exception e) {
            img.setImage(new Image("https://via.placeholder.com/300x150"));
        }
        img.setFitWidth(270);
        img.setFitHeight(130);

        Label lblTitle = new Label(title);
        lblTitle.setWrapText(true);
        lblTitle.setMaxHeight(60);
        lblTitle.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 13px;");

        Label lblSource = new Label("🔹 " + source);
        lblSource.setStyle("-fx-text-fill: #38bdf8; -fx-font-size: 11px;");

        card.getChildren().addAll(img, lblTitle, lblSource);

        // Action au clic : Ouverture du navigateur
        card.setOnMouseClicked(e -> {
            if (!articleUrl.isEmpty()) {
                try {
                    java.awt.Desktop.getDesktop().browse(new java.net.URI(articleUrl));
                } catch (Exception ex) { ex.printStackTrace(); }
            }
        });

        // Effets visuels au survol
        card.setOnMouseEntered(e -> card.setStyle(card.getStyle() + "-fx-border-color: #38bdf8; -fx-border-width: 1; -fx-border-radius: 15;"));
        card.setOnMouseExited(e -> card.setStyle(card.getStyle().replace("-fx-border-color: #38bdf8; -fx-border-width: 1; -fx-border-radius: 15;", "")));

        return card;
    }

    // --- ANIMATIONS & STYLE ---

    private void lancerTicker() {
        TranslateTransition tt = new TranslateTransition(Duration.seconds(18), lblTicker);
        tt.setFromX(1000); tt.setToX(-1200);
        tt.setCycleCount(Timeline.INDEFINITE);
        tt.setInterpolator(Interpolator.LINEAR);
        tt.play();
    }

    private void appliquerTransition(Node n) {
        FadeTransition ft = new FadeTransition(Duration.millis(500), n);
        ft.setFromValue(0); ft.setToValue(1); ft.play();
    }

    private void mettreAJourStyleBouton(Button actif) {
        List<Button> btns = List.of(btnDashboard, btnProjets, btnCredits);
        btns.forEach(b -> b.setStyle("-fx-background-color: transparent; -fx-text-fill: #94a3b8; -fx-font-weight: normal;"));
        if (actif != null) {
            actif.setStyle("-fx-background-color: rgba(56, 189, 248, 0.1); -fx-text-fill: #38bdf8; -fx-border-color: #38bdf8; -fx-border-width: 0 0 0 4; -fx-font-weight: bold;");
        }
    }
    @FXML
    private void handleLogout(ActionEvent event) {
        try {
            // 1. Charger le nouveau fichier FXML
            // Assurez-vous que le chemin commence par / et correspond à votre structure
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/DashboardWalletClient.fxml"));
            Parent root = loader.load();

            // 2. Récupérer la scène actuelle à partir du bouton cliqué
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();

            // 3. Remplacer le contenu de la fenêtre
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.show();

            System.out.println("Redirection vers le Dashboard Wallet réussie.");

        } catch (IOException e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Impossible de charger le tableau de bord : " + e.getMessage()).show();
        }
    }
}