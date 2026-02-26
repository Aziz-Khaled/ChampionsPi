package tn.esprit.Champions.gui.invest;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
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

    @FXML private HBox mainContainer;
    @FXML private FlowPane gridPane;
    @FXML private TextField txtSearch;
    @FXML private ComboBox<String> comboFilter;
    @FXML private Button btnRefresh;
    @FXML private Label lblTotalProjects, lblTotalVolume;

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

        // Animation d'entrée douce pour le container principal
        mainContainer.setOpacity(0);
        FadeTransition fadeIn = new FadeTransition(Duration.millis(800), mainContainer);
        fadeIn.setToValue(1.0);
        fadeIn.play();

        // Listeners pour filtrage automatique
        txtSearch.textProperty().addListener((obs, oldVal, newVal) -> filtrerDonnees());
        comboFilter.valueProperty().addListener((obs, oldVal, newVal) -> filtrerDonnees());

        // Bouton de rafraîchissement
        btnRefresh.setOnAction(e -> chargerDonnees());

        // Chargement différé pour laisser l'UI s'initialiser
        Platform.runLater(this::chargerDonnees);
    }

    private void chargerDonnees() {
        try {
            touteLaListe = cs.SelectAll();
            majStatistiques();
            afficherCredits(touteLaListe);
        } catch (SQLException e) {
            System.err.println("Erreur SQL lors du chargement : " + e.getMessage());
        }
    }

    private void majStatistiques() {
        if (touteLaListe != null) {
            if (lblTotalProjects != null) {
                lblTotalProjects.setText(String.valueOf(touteLaListe.size()));
            }
            if (lblTotalVolume != null) {
                double volume = touteLaListe.stream().mapToDouble(credit::getMontant).sum();
                lblTotalVolume.setText(String.format("%,.0f DT", volume));
            }
        }
    }

    private void filtrerDonnees() {
        String recherche = txtSearch.getText().trim().toLowerCase();
        String secteurFiltre = comboFilter.getValue();

        List<credit> filtree = touteLaListe.stream()
                .filter(c -> {
                    // Recherche textuelle
                    boolean matchesSearch = recherche.isEmpty() ||
                            (c.getDescription() != null && c.getDescription().toLowerCase().contains(recherche));

                    // Filtre par secteur
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
        int delayCounter = 0;

        for (credit c : liste) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/invest/CreditCard.fxml"));
                VBox card = loader.load();

                CreditCardController ctrl = loader.getController();
                ctrl.setCreditData(c);

                // Événement de clic pour voir les détails
                card.setOnMouseClicked(event -> ouvrirDetailsCredit(c));

                gridPane.getChildren().add(card);

                // --- ANIMATION EN CASCADE (Corrigée) ---
                animerApparitionCarte(card, delayCounter++);

            } catch (IOException e) {
                System.err.println("Erreur chargement carte FXML : " + e.getMessage());
            }
        }
    }

    /**
     * Gère l'animation de montée et d'opacité de chaque carte.
     */
    private void animerApparitionCarte(Node node, int index) {
        node.setOpacity(0);
        node.setTranslateY(40); // Départ 40 pixels plus bas

        // Correction : Utilisation de setToY() au lieu de setToValue()
        TranslateTransition tt = new TranslateTransition(Duration.millis(500), node);
        tt.setToY(0);
        tt.setInterpolator(Interpolator.EASE_OUT);

        FadeTransition ft = new FadeTransition(Duration.millis(500), node);
        ft.setToValue(1.0);

        // Groupe les deux animations
        ParallelTransition parallel = new ParallelTransition(tt, ft);

        // Applique un délai de 60ms multiplié par l'index de la carte (Cascade)
        parallel.setDelay(Duration.millis(index * 60));
        parallel.play();
    }

    private void ouvrirDetailsCredit(credit c) {
        try {
            // Animation de transition avant de changer de scène
            FadeTransition fadeOut = new FadeTransition(Duration.millis(300), mainContainer);
            fadeOut.setToValue(0.1);
            fadeOut.setOnFinished(e -> {
                try {
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/invest/DetailsCredit.fxml"));
                    Parent root = loader.load();

                    DetailsCreditController controller = loader.getController();
                    if (controller != null) {
                        controller.initData(c);
                    }

                    // Remplace la racine de la scène actuelle
                    mainContainer.getScene().setRoot(root);
                } catch (IOException ex) {
                    System.err.println("Impossible d'ouvrir DetailsCredit : " + ex.getMessage());
                    ex.printStackTrace();
                }
            });
            fadeOut.play();

        } catch (Exception e) {
            System.err.println("Erreur lors de la transition vers les détails : " + e.getMessage());
        }
    }
}