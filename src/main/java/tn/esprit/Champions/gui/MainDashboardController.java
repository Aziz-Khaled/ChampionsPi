package tn.esprit.Champions.gui;

import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Priority;
import javafx.util.Duration;
import tn.esprit.Champions.services.creditService;
import tn.esprit.Champions.services.projetService;

import java.io.IOException;
import java.util.List;

public class MainDashboardController {

    @FXML private VBox mainContent;
    @FXML private Button btnDashboard, btnCredits, btnProjets, btnNegociations;
    @FXML private Label lblWelcome, lblRole, statTotalCredits, statTotalProjets;

    private final creditService cs = new creditService();
    private final projetService ps = new projetService();
    private Node dashboardHomeView;

    @FXML
    public void initialize() {
        // Sauvegarde de l'affichage des stats
        if (!mainContent.getChildren().isEmpty()) {
            dashboardHomeView = mainContent.getChildren().get(0);
        }

        setupMenuActions();
        rafraichirStatistiques();
        mettreAJourStyleBouton(btnDashboard); // Dashboard actif au début
    }

    private void setupMenuActions() {
        btnDashboard.setOnAction(e -> {
            mainContent.getChildren().setAll(dashboardHomeView);
            appliquerTransition(dashboardHomeView);
            rafraichirStatistiques();
            mettreAJourStyleBouton(btnDashboard);
        });

        btnProjets.setOnAction(e -> chargerModule("/AfficherProjets.fxml", btnProjets));
        btnCredits.setOnAction(e -> chargerModule("/AfficherCredits.fxml", btnCredits));
    }

    private void chargerModule(String fxmlPath, Button source) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent view = loader.load();
            VBox.setVgrow(view, Priority.ALWAYS);

            mainContent.getChildren().setAll(view);
            appliquerTransition(view);
            mettreAJourStyleBouton(source);
        } catch (IOException e) {
            System.err.println("Erreur chargement : " + fxmlPath);
        }
    }

    private void appliquerTransition(Node n) {
        FadeTransition ft = new FadeTransition(Duration.millis(400), n);
        ft.setFromValue(0.3);
        ft.setToValue(1.0);
        ft.play();
    }

    private void rafraichirStatistiques() {
        Platform.runLater(() -> {
            try {
                statTotalCredits.setText(String.valueOf(cs.getTotalCredits()));
                statTotalProjets.setText(String.valueOf(ps.getTotalProjets()));
                lblWelcome.setText("Bienvenue, Sarra Gharbi 👋");
            } catch (Exception e) { e.printStackTrace(); }
        });
    }

    private void mettreAJourStyleBouton(Button actif) {
        List<Button> btns = List.of(btnDashboard, btnProjets, btnCredits, btnNegociations);
        for (Button b : btns) {
            if (b == actif) {
                b.setStyle("-fx-background-color: #34495e; -fx-text-fill: #3498db; -fx-font-weight: bold; -fx-alignment: CENTER_LEFT; -fx-padding: 0 0 0 20; -fx-border-color: #3498db; -fx-border-width: 0 0 0 5;");
            } else {
                b.setStyle("-fx-background-color: transparent; -fx-text-fill: #bdc3c7; -fx-alignment: CENTER_LEFT; -fx-padding: 0 0 0 20; -fx-border-width: 0;");
            }
        }
    }
}