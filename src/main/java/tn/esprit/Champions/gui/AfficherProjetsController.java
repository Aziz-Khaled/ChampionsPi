package tn.esprit.Champions.gui;

import javafx.animation.FadeTransition;
import javafx.animation.TranslateTransition;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;
import tn.esprit.Champions.models.Utilisateur;
import tn.esprit.Champions.models.projet;
import tn.esprit.Champions.models.projetStatus;
import tn.esprit.Champions.services.projetService;

import java.io.IOException;
import java.net.URL;
import java.sql.Timestamp;
import java.util.ResourceBundle;

public class AfficherProjetsController implements Initializable {

    // Conteneurs pour les animations
    @FXML private HBox headerContainer, statsContainer;

    @FXML private TableView<projet> tableProjets;
    @FXML private TableColumn<projet, String> colTitre;
    @FXML private TableColumn<projet, String> colDescription;
    @FXML private TableColumn<projet, Float> colMontant;
    @FXML private TableColumn<projet, projetStatus> colStatus;
    @FXML private TableColumn<projet, Timestamp> colDateDebut;
    @FXML private TableColumn<projet, Timestamp> colDateFin;

    @FXML private Button btnEdit, btnDelete;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> statusFilterCombo;
    @FXML private Label statLabel;

    private final projetService ps = new projetService();
    private final ObservableList<projet> masterData = FXCollections.observableArrayList();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        configurerColonnes();
        chargerDonnees();
        configurerRechercheDynamique();
        appliquerAnimations();

        // Gestion de l'activation des boutons
        tableProjets.getSelectionModel().selectedItemProperty().addListener((obs, old, selection) -> {
            boolean selectionExiste = (selection != null);
            btnEdit.setDisable(!selectionExiste);
            btnDelete.setDisable(!selectionExiste);
        });
    }

    private void appliquerAnimations() {
        animateNode(headerContainer, 0);
        animateNode(statsContainer, 150);
        animateNode(tableProjets, 300);
    }

    private void animateNode(Node node, double delayMs) {
        node.setOpacity(0);
        FadeTransition ft = new FadeTransition(Duration.millis(800), node);
        ft.setFromValue(0); ft.setToValue(1);
        ft.setDelay(Duration.millis(delayMs));

        TranslateTransition tt = new TranslateTransition(Duration.millis(800), node);
        tt.setFromY(20); tt.setToY(0);
        tt.setDelay(Duration.millis(delayMs));

        ft.play(); tt.play();
    }

    private void configurerColonnes() {
        colTitre.setCellValueFactory(new PropertyValueFactory<>("title"));
        colDescription.setCellValueFactory(new PropertyValueFactory<>("description"));
        colMontant.setCellValueFactory(new PropertyValueFactory<>("target_amount"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colDateDebut.setCellValueFactory(new PropertyValueFactory<>("start_date"));
        colDateFin.setCellValueFactory(new PropertyValueFactory<>("end_date"));

        // ✅ Design des Badges de Statut (Style Dark Marketplace)
        colStatus.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(projetStatus item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    Label badge = new Label(item.toString());
                    badge.setPrefWidth(100);
                    badge.setAlignment(Pos.CENTER);

                    String styleBase = "-fx-padding: 6 12; -fx-background-radius: 20; -fx-font-weight: bold; -fx-font-size: 11px;";

                    if (item == projetStatus.DRAFT) {
                        badge.setStyle(styleBase + "-fx-background-color: rgba(52, 152, 219, 0.2); -fx-text-fill: #3498db; -fx-border-color: rgba(52, 152, 219, 0.3); -fx-border-radius: 20;");
                    } else if (item == projetStatus.ACTIVE) {
                        badge.setStyle(styleBase + "-fx-background-color: rgba(16, 185, 129, 0.2); -fx-text-fill: #10b981; -fx-border-color: rgba(16, 185, 129, 0.3); -fx-border-radius: 20;");
                    } else {
                        badge.setStyle(styleBase + "-fx-background-color: rgba(148, 163, 184, 0.2); -fx-text-fill: #94a3b8; -fx-border-color: rgba(148, 163, 184, 0.3); -fx-border-radius: 20;");
                    }

                    HBox container = new HBox(badge);
                    container.setAlignment(Pos.CENTER);
                    setGraphic(container);
                }
            }
        });
    }

    private void configurerRechercheDynamique() {
        ObservableList<String> options = FXCollections.observableArrayList("Tous");
        for (projetStatus s : projetStatus.values()) {
            options.add(s.toString());
        }
        statusFilterCombo.setItems(options);
        statusFilterCombo.setValue("Tous");

        FilteredList<projet> filteredData = new FilteredList<>(masterData, p -> true);

        Runnable applyFilters = () -> {
            String textFilter = searchField.getText() == null ? "" : searchField.getText().toLowerCase();
            String selectedStatus = statusFilterCombo.getValue();

            filteredData.setPredicate(p -> {
                boolean matchesText = textFilter.isEmpty() ||
                        p.getTitle().toLowerCase().contains(textFilter) ||
                        (p.getDescription() != null && p.getDescription().toLowerCase().contains(textFilter)) ||
                        String.valueOf(p.getTarget_amount()).contains(textFilter);

                boolean matchesStatus = selectedStatus.equals("Tous") ||
                        p.getStatus().toString().equals(selectedStatus);

                return matchesText && matchesStatus;
            });
            updateStatLabel(filteredData.size());
        };

        searchField.textProperty().addListener((obs, old, newVal) -> applyFilters.run());
        statusFilterCombo.valueProperty().addListener((obs, old, newVal) -> applyFilters.run());

        SortedList<projet> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(tableProjets.comparatorProperty());
        tableProjets.setItems(sortedData);
    }

    private void chargerDonnees() {
        try {
            masterData.setAll(ps.SelectAll());
            updateStatLabel(masterData.size());
        } catch (Exception e) {
            afficherErreur("Erreur chargement : " + e.getMessage());
        }
    }

    private void updateStatLabel(int count) {
        if (statLabel != null) {
            statLabel.setText(count + (count > 1 ? " Projets" : " Projet"));
        }
    }

    @FXML
    private void handleEditSelection() {
        projet selected = tableProjets.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ModifierProjet.fxml"));
            Parent root = loader.load();
            ModifierProjetController controller = loader.getController();
            controller.initData(selected);

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            Scene scene = new Scene(root);
            root.setStyle("-fx-background-color: #0f172a;");
            stage.setScene(scene);
            stage.showAndWait();

            chargerDonnees();
        } catch (IOException e) {
            afficherErreur("Erreur d'ouverture de l'interface.");
        }
    }

    @FXML
    private void handleDeleteSelection() {
        projet selected = tableProjets.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Voulez-vous supprimer le projet : " + selected.getTitle() + " ?", ButtonType.OK, ButtonType.CANCEL);
        alert.getDialogPane().setStyle("-fx-background-color: #1e293b;");
        alert.getDialogPane().lookupAll(".label").forEach(node -> node.setStyle("-fx-text-fill: white;"));

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    ps.deleteOne(selected);
                    chargerDonnees();
                } catch (Exception e) {
                    afficherErreur("Erreur suppression : " + e.getMessage());
                }
            }
        });
    }

    @FXML
    private void ouvrirAjout() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/AjouterProjet.fxml"));
            Parent root = loader.load();

            AjouterProjetController controller = loader.getController();
            Utilisateur userConnecte = new Utilisateur();
            userConnecte.setId_user(2); // Simulation user
            controller.setConnectedOwner(userConnecte);

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            Scene scene = new Scene(root);
            root.setStyle("-fx-background-color: #0f172a;");
            stage.setScene(scene);
            stage.showAndWait();
            chargerDonnees();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void afficherErreur(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.getDialogPane().setStyle("-fx-background-color: #1e293b;");
        alert.setContentText(message);
        alert.showAndWait();
    }
}