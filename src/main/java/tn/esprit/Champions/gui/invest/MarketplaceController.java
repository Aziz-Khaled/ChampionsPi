package tn.esprit.Champions.gui.invest;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import tn.esprit.Champions.models.credit;
import tn.esprit.Champions.models.projet;
import tn.esprit.Champions.services.creditService;
import tn.esprit.Champions.services.projetService;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class MarketplaceController implements Initializable {

    // On change VBox par HBox car la racine du nouveau FXML est une HBox (Sidebar + Content)
    @FXML private HBox mainContainer;
    @FXML private FlowPane gridPane;
    @FXML private TextField txtSearch;
    @FXML private ComboBox<String> comboFilter;
    @FXML private Button btnRefresh;

    // Nouveaux éléments pour les statistiques (Optionnel, si tu as gardé les IDs dans le FXML)
    @FXML private Label lblTotalProjects;
    @FXML private Label lblTotalVolume;

    private final creditService cs = new creditService();
    private final projetService ps = new projetService();
    private List<credit> touteLaListe = new ArrayList<>();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Initialisation du ComboBox
        comboFilter.setItems(FXCollections.observableArrayList(
                "Tous les secteurs", "Agriculture", "Technologie", "Énergie", "Santé", "Immobilier", "Autre"
        ));
        comboFilter.getSelectionModel().selectFirst();

        // Listeners pour recherche en temps réel
        txtSearch.textProperty().addListener((obs, oldVal, newVal) -> filtrerDonnees());
        comboFilter.valueProperty().addListener((obs, oldVal, newVal) -> filtrerDonnees());

        btnRefresh.setOnAction(e -> chargerDonnees());

        // Chargement initial
        Platform.runLater(this::chargerDonnees);
    }

    private void chargerDonnees() {
        try {
            touteLaListe = cs.SelectAll();
            afficherCredits(touteLaListe);
            majStatistiques(); // Met à jour les petits compteurs en haut
        } catch (SQLException e) {
            System.err.println("Erreur SQL : " + e.getMessage());
        }
    }

    private void majStatistiques() {
        if (touteLaListe != null) {
            // Met à jour le nombre réel de projets chargés depuis la DB
            if (lblTotalProjects != null) {
                lblTotalProjects.setText(String.valueOf(touteLaListe.size()));
            }

            // Calcule et affiche le volume réel
            if (lblTotalVolume != null) {
                double volume = touteLaListe.stream().mapToDouble(credit::getMontant).sum();
                lblTotalVolume.setText(String.format("%,.0f TND", volume));
            }
        }
    }

    private void filtrerDonnees() {
        String recherche = txtSearch.getText().trim().toLowerCase();
        String secteurFiltre = comboFilter.getValue();

        List<credit> filtree = touteLaListe.stream()
                .filter(c -> {
                    // Vérifie si la description ou le titre du projet correspond à la recherche
                    boolean matchesSearch = recherche.isEmpty() ||
                            (c.getDescription() != null && c.getDescription().toLowerCase().contains(recherche));

                    if (secteurFiltre == null || secteurFiltre.equals("Tous les secteurs")) return matchesSearch;

                    try {
                        projet p = ps.findById(c.getProject_id());
                        return matchesSearch && p != null && p.getSecteur().equalsIgnoreCase(secteurFiltre);
                    } catch (SQLException e) { return false; }
                }).collect(Collectors.toList());

        afficherCredits(filtree);
    }

    private void afficherCredits(List<credit> liste) {
        gridPane.getChildren().clear();
        for (credit c : liste) {
            try {
                // Chargement de la carte stylisée
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/invest/CreditCard.fxml"));
                VBox card = loader.load();

                CreditCardController ctrl = loader.getController();
                ctrl.setCreditData(c);

                // Navigation vers les détails au clic
                card.setOnMouseClicked(event -> ouvrirDetailsCredit(c));

                gridPane.getChildren().add(card);
            } catch (IOException e) {
                System.err.println("Erreur chargement carte : " + e.getMessage());
            }
        }
    }

    private void ouvrirDetailsCredit(credit c) {
        try {
            // VÉRIFIE BIEN LE CHEMIN ICI : Doit pointer vers DetailsCredit.fxml
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/invest/DetailsCredit.fxml"));
            Parent root = loader.load();

            // C'est ici que l'erreur se produisait à la ligne 128
            // Vérifie que dans DetailsCredit.fxml, l'attribut fx:controller
            // est bien "tn.esprit.Champions.gui.invest.DetailsCreditController"
            DetailsCreditController controller = loader.getController();

            if (controller != null) {
                controller.initData(c);
            }

            mainContainer.getScene().setRoot(root);

        } catch (IOException e) {
            System.err.println("Erreur de chargement FXML : " + e.getMessage());
        } catch (ClassCastException e) {
            System.err.println("ERREUR CRITIQUE : Le fichier FXML chargé n'utilise pas DetailsCreditController !");
            e.printStackTrace();
        }
    }
}