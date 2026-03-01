package tn.esprit.Champions.gui;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import tn.esprit.Champions.models.Utilisateur;

import java.io.IOException;

public class MainLayoutController {

    @FXML
    private StackPane contentArea;
    @FXML
    private Label userNameLabel;


    @FXML private Label homeLink;
    @FXML private Label catalogueLink;
    @FXML private Label panierLink;
    @FXML private Label vendeurLink;

    public void initialize() {
        // default: hide all sections
        homeLink.setVisible(false);
        catalogueLink.setVisible(false);
        panierLink.setVisible(false);
        vendeurLink.setVisible(false);
    }
    public void setUser(Utilisateur user) {
        userNameLabel.setText(user.getNom());

        switch (user.getRole()) {
            case COMMERCANT:
                // COMMERCANT can only see the vendeur section
                vendeurLink.setVisible(true);
                break;

            case CLIENT, INVESTISSEUR:
                // CLIENT & INVESTISSEUR see marketplace sections
                homeLink.setVisible(true);
                catalogueLink.setVisible(true);
                panierLink.setVisible(true);
                break;

        }
    }

    @FXML
    // Afficher la vue du catalogue client
    private void showCatalogue() {
        loadView("client/ClientDashboard.fxml");
    }

    @FXML
    // Afficher la vue du panier client
    private void showCart() {
        loadView("client/CartView.fxml");
    }

    @FXML
    // Afficher le tableau de bord du fournisseur (ou autre selon le rôle)
    private void showDashboard() {
        // Here we could check user role, for now just show Fournisseur as requested
        loadView("fournisseur/FournisseurDashboard.fxml");
    }

    @FXML
    // Gérer la déconnexion de l'utilisateur
    private void handleLogout() {
        System.out.println("Logging out...");
        System.exit(0);
    }

    // Charger une vue FXML et l'afficher dans la zone de contenu principale
    private void loadView(String fxmlPath) {
        try {
            // Path is now relative to src/main/resources/views/
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/" + fxmlPath));
            Parent view = loader.load();
            contentArea.getChildren().setAll(view);
        } catch (IOException e) {
            System.err.println("Could not load FXML: /views/" + fxmlPath);
            e.printStackTrace();
        }
    }
}
