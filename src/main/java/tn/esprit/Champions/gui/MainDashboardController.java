package tn.esprit.Champions.gui;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Priority;
import tn.esprit.Champions.services.creditService;
import tn.esprit.Champions.services.projetService;

import java.io.IOException;
import java.util.List;

public class MainDashboardController {

    @FXML private VBox mainContent;

    // Boutons de la barre latérale
    @FXML private Button btnDashboard;
    @FXML private Button btnCredits;
    @FXML private Button btnProjets;
    @FXML private Button btnNegociations;

    // Éléments de la barre supérieure et stats
    @FXML private Label lblWelcome;
    @FXML private Label lblRole;
    @FXML private Label statTotalCredits;
    @FXML private Label statTotalNegos;
    @FXML private Label statTotalProjets;

    // Services pour la base de données
    private final creditService cs = new creditService();
    private final projetService ps = new projetService();

    private Node dashboardHomeView;

    @FXML
    public void initialize() {
        // 1. Sauvegarder la vue initiale (les cartes de stats)
        if (!mainContent.getChildren().isEmpty()) {
            dashboardHomeView = mainContent.getChildren().get(0);
        }

        // 2. Configurer les actions des boutons
        btnDashboard.setOnAction(event -> {
            if (dashboardHomeView != null) {
                mainContent.getChildren().setAll(dashboardHomeView);
                rafraichirStatistiques(); // Mise à jour des chiffres réels au retour
            }
            mettreAJourStyleBouton(btnDashboard);
        });

        btnProjets.setOnAction(event -> {
            chargerVue("/AfficherProjets.fxml");
            mettreAJourStyleBouton(btnProjets);
        });

        btnCredits.setOnAction(event -> {
            chargerVue("/AfficherCredits.fxml");
            mettreAJourStyleBouton(btnCredits);
        });

        btnNegociations.setOnAction(event -> {
            // chargerVue("/AfficherNegociations.fxml");
            mettreAJourStyleBouton(btnNegociations);
        });

        // 3. Initialiser les données utilisateur et les statistiques réelles
        lblWelcome.setText("Bienvenue, Sarra Gharbi 👋");
        lblRole.setText("Rôle : Emprunteur");

        rafraichirStatistiques();
        mettreAJourStyleBouton(btnDashboard); // Dashboard actif par défaut
    }

    /**
     * Récupère les données réelles depuis la base de données
     */
    private void rafraichirStatistiques() {
        try {
            int totalCredits = cs.getTotalCredits();
            int totalProjets = ps.getTotalProjets();

            statTotalCredits.setText(String.valueOf(totalCredits));
            statTotalProjets.setText(String.valueOf(totalProjets));
            // statTotalNegos.setText("0"); // À implémenter avec ServiceNegociation

        } catch (Exception e) {
            System.err.println("Erreur lors de la mise à jour des stats : " + e.getMessage());
        }
    }

    private void chargerVue(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent view = loader.load();

            // Remplacer le contenu
            mainContent.getChildren().setAll(view);

            // Forcer l'extension pour remplir l'espace
            VBox.setVgrow(view, Priority.ALWAYS);

        } catch (IOException e) {
            System.err.println("Erreur de chargement de la vue : " + fxmlPath);
            e.printStackTrace();
        }
    }

    private void mettreAJourStyleBouton(Button boutonActif) {
        List<Button> tousLesBoutons = List.of(btnDashboard, btnProjets, btnCredits, btnNegociations);

        for (Button btn : tousLesBoutons) {
            if (btn == boutonActif) {
                btn.setStyle("-fx-background-color: #34495e; -fx-text-fill: white; -fx-alignment: CENTER_LEFT; -fx-cursor: hand; -fx-background-radius: 5;");
            } else {
                btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #ecf0f1; -fx-alignment: CENTER_LEFT; -fx-cursor: hand;");
            }
        }
    }
}