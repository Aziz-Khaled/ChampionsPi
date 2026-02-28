package tn.esprit.Champions.gui.invest;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import tn.esprit.Champions.models.Negociation;
import tn.esprit.Champions.models.credit;
import tn.esprit.Champions.models.projet;
import tn.esprit.Champions.models.wallet; // Import du modèle wallet
import tn.esprit.Champions.services.creditService;
import tn.esprit.Champions.services.negociationService;
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
    private final negociationService ns = new negociationService();

    private List<credit> touteLaListe = new ArrayList<>();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        comboFilter.setItems(FXCollections.observableArrayList(
                "Tous les secteurs", "Agriculture", "Technologie", "Énergie", "Santé", "Immobilier", "Autre"
        ));
        comboFilter.getSelectionModel().selectFirst();

        mainContainer.setOpacity(0);
        FadeTransition fadeIn = new FadeTransition(Duration.millis(800), mainContainer);
        fadeIn.setToValue(1.0);
        fadeIn.play();

        txtSearch.textProperty().addListener((obs, oldVal, newVal) -> filtrerDonnees());
        comboFilter.valueProperty().addListener((obs, oldVal, newVal) -> filtrerDonnees());
        btnRefresh.setOnAction(e -> chargerDonnees());

        Platform.runLater(() -> {
            chargerDonnees();
            verifierNotifications();
        });
    }

    private void chargerDonnees() {
        try {
            touteLaListe = cs.SelectAll();
            majStatistiques();
            afficherCredits(touteLaListe);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void verifierNotifications() {
        try {
            int idInvestisseurConnecte = 2; // ID de test
            List<Negociation> toutesLesNegos = ns.SelectAll();

            List<Negociation> offresAcceptees = toutesLesNegos.stream()
                    .filter(n -> n.getInvestor_id() == idInvestisseurConnecte)
                    .filter(n -> n.getStatus() != null &&
                            (n.getStatus().equalsIgnoreCase("ACCEPTED") || n.getStatus().equalsIgnoreCase("APPROUVEE")))
                    .collect(Collectors.toList());

            for (Negociation n : offresAcceptees) {
                credit cLinked = touteLaListe.stream()
                        .filter(c -> c.getId() == n.getCredit_id())
                        .findFirst()
                        .orElse(null);

                if (cLinked != null) {
                    projet pLinked = ps.SelectAll().stream()
                            .filter(p -> p.getId_project() == cLinked.getProject_id())
                            .findFirst()
                            .orElse(null);

                    String nomProjet = (pLinked != null) ? pLinked.getTitle() : "Investissement #" + cLinked.getId();
                    Platform.runLater(() -> showToastNotification(n, nomProjet, cLinked));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void showToastNotification(Negociation n, String nomProjet, credit cLinked) {
        Stage toastStage = new Stage();
        toastStage.initStyle(StageStyle.UNDECORATED);
        toastStage.setAlwaysOnTop(true);

        VBox root = new VBox(10);
        root.setPadding(new Insets(15));
        root.setStyle("-fx-background-color: #1e293b; -fx-border-color: #10b981; -fx-border-width: 2; -fx-background-radius: 12; -fx-border-radius: 12;");
        root.setPrefWidth(300);

        HBox header = new HBox();
        Label title = new Label("🎉 OFFRE ACCEPTÉE !");
        title.setStyle("-fx-text-fill: #10b981; -fx-font-weight: bold;");

        Button btnClose = new Button("✕");
        btnClose.setStyle("-fx-background-color: transparent; -fx-text-fill: white; -fx-cursor: hand;");
        btnClose.setOnAction(e -> toastStage.close());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        header.getChildren().addAll(title, spacer, btnClose);

        Label desc = new Label("Votre offre pour le projet \"" + nomProjet + "\" est validée.");
        desc.setStyle("-fx-text-fill: white; -fx-wrap-text: true;");

        // Bouton de paiement mis à jour pour ouvrir la transaction
        Button btnAction = new Button("💳 PAYER " + n.getMontant() + " TND");
        btnAction.setMaxWidth(Double.MAX_VALUE);
        btnAction.setStyle("-fx-background-color: #10b981; -fx-text-fill: #064e3b; -fx-font-weight: bold; -fx-cursor: hand;");

        btnAction.setOnAction(e -> {
            toastStage.close();
            ouvrirInterfaceTransaction(cLinked);
        });

        root.getChildren().addAll(header, desc, btnAction);

        Scene scene = new Scene(root);
        toastStage.setScene(scene);

        Rectangle2D bounds = Screen.getPrimary().getVisualBounds();
        toastStage.setX(bounds.getMaxX() - 320);
        toastStage.setY(bounds.getMaxY() - 160);
        toastStage.show();

        PauseTransition delay = new PauseTransition(Duration.seconds(10));
        delay.setOnFinished(e -> toastStage.close());
        delay.play();
    }

    /**
     * Ouvre l'interface de transaction statique pour la base d'intégration
     */
    private void ouvrirInterfaceTransaction(credit c) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/invest/ConfirmTransaction.fxml"));
            Parent root = loader.load();

            ConfirmTransactionController controller = loader.getController();

            // Simulation d'un wallet pour le test statique
            wallet mockWallet = new wallet();
            mockWallet.setRib("TN59 1234 5678 9012");
            mockWallet.setSolde(50000.0); // Solde de test suffisant

            controller.setTransactionData(c, mockWallet);

            Stage stage = new Stage();
            stage.initStyle(StageStyle.UNDECORATED); // Look moderne sans bordures
            stage.setScene(new Scene(root));
            stage.show();

        } catch (IOException e) {
            System.err.println("Erreur chargement ConfirmTransaction: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void majStatistiques() {
        if (touteLaListe != null && lblTotalProjects != null && lblTotalVolume != null) {
            lblTotalProjects.setText(String.valueOf(touteLaListe.size()));
            double volume = touteLaListe.stream().mapToDouble(credit::getMontant).sum();
            lblTotalVolume.setText(String.format("%,.0f DT", volume));
        }
    }

    private void filtrerDonnees() {
        String recherche = txtSearch.getText().trim().toLowerCase();
        String secteurFiltre = comboFilter.getValue();

        List<credit> filtree = touteLaListe.stream()
                .filter(c -> {
                    boolean matchesSearch = recherche.isEmpty() ||
                            (c.getDescription() != null && c.getDescription().toLowerCase().contains(recherche));

                    if (secteurFiltre == null || secteurFiltre.equals("Tous les secteurs")) return matchesSearch;

                    try {
                        projet p = ps.SelectAll().stream()
                                .filter(proj -> proj.getId_project() == c.getProject_id())
                                .findFirst().orElse(null);
                        return matchesSearch && p != null && p.getSecteur().equalsIgnoreCase(secteurFiltre);
                    } catch (SQLException e) { return false; }
                }).collect(Collectors.toList());

        afficherCredits(filtree);
    }

    private void afficherCredits(List<credit> liste) {
        gridPane.getChildren().clear();
        for (credit c : liste) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/invest/CreditCard.fxml"));
                VBox card = loader.load();

                CreditCardController ctrl = loader.getController();
                if (ctrl != null) {
                    ctrl.setCreditData(c);
                }

                card.setOnMouseClicked(event -> ouvrirDetailsCredit(c));
                gridPane.getChildren().add(card);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void ouvrirDetailsCredit(credit c) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/invest/DetailsCredit.fxml"));
            Parent root = loader.load();

            DetailsCreditController controller = loader.getController();
            if (controller != null) {
                controller.initData(c);
            }

            Stage stage = (Stage) mainContainer.getScene().getWindow();
            stage.getScene().setRoot(root);

        } catch (IOException ex) {
            System.err.println("Erreur lors de l'ouverture des détails : " + ex.getMessage());
        }
    }
}