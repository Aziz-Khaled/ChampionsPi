package tn.esprit.Champions.gui;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import tn.esprit.Champions.models.projet;
import tn.esprit.Champions.models.projetStatus;
import tn.esprit.Champions.services.projetService;

import java.io.IOException;
import java.net.URL;
import java.sql.Timestamp;
import java.util.ResourceBundle;

public class AfficherProjetsController implements Initializable {

    @FXML private TableView<projet> tableProjets;
    @FXML private TableColumn<projet, String> colTitre;
    @FXML private TableColumn<projet, String> colDescription;
    @FXML private TableColumn<projet, Float> colMontant;
    @FXML private TableColumn<projet, projetStatus> colStatus;
    @FXML private TableColumn<projet, Timestamp> colDateDebut;
    @FXML private TableColumn<projet, Timestamp> colDateFin;

    @FXML private Button btnEdit;
    @FXML private Button btnDelete;
    @FXML private TextField searchField;

    // ✅ AJOUTÉ : Déclaration du ComboBox pour le FXML
    @FXML private ComboBox<String> statusFilterCombo;

    @FXML private Label statLabel;

    private final projetService ps = new projetService();
    private final ObservableList<projet> masterData = FXCollections.observableArrayList();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        configurerColonnes();
        chargerDonnees();
        configurerRechercheDynamique(); // ✅ Inclut maintenant le remplissage du Combo

        tableProjets.getSelectionModel().selectedItemProperty().addListener((obs, old, selection) -> {
            boolean selectionExiste = (selection != null);
            btnEdit.setDisable(!selectionExiste);
            btnDelete.setDisable(!selectionExiste);
        });
    }

    private void configurerColonnes() {
        colTitre.setCellValueFactory(new PropertyValueFactory<>("title"));
        colDescription.setCellValueFactory(new PropertyValueFactory<>("description"));
        colMontant.setCellValueFactory(new PropertyValueFactory<>("target_amount"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colDateDebut.setCellValueFactory(new PropertyValueFactory<>("start_date"));
        colDateFin.setCellValueFactory(new PropertyValueFactory<>("end_date"));

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
                    String styleBase = "-fx-padding: 4 10; -fx-background-radius: 15; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 11px;";

                    if (item == projetStatus.DRAFT) {
                        badge.setStyle(styleBase + "-fx-background-color: #3498db;");
                    } else if (item == projetStatus.ACTIVE) {
                        badge.setStyle(styleBase + "-fx-background-color: #27ae60;");
                    } else {
                        badge.setStyle(styleBase + "-fx-background-color: #95a5a6;");
                    }

                    HBox container = new HBox(badge);
                    container.setAlignment(Pos.CENTER);
                    setGraphic(container);
                }
            }
        });
    }

    private void configurerRechercheDynamique() {
        // ✅ 1. Remplir le ComboBox avec les valeurs de l'Enum
        ObservableList<String> options = FXCollections.observableArrayList("Tous");
        for (projetStatus s : projetStatus.values()) {
            options.add(s.toString());
        }
        statusFilterCombo.setItems(options);
        statusFilterCombo.setValue("Tous");

        // 2. Créer la liste filtrable
        FilteredList<projet> filteredData = new FilteredList<>(masterData, p -> true);

        // ✅ 3. Créer une méthode de filtrage commune pour le texte ET le combo
        Runnable applyFilters = () -> {
            String textFilter = searchField.getText() == null ? "" : searchField.getText().toLowerCase();
            String selectedStatus = statusFilterCombo.getValue();

            filteredData.setPredicate(p -> {
                // Filtre Texte
                boolean matchesText = textFilter.isEmpty() ||
                        p.getTitle().toLowerCase().contains(textFilter) ||
                        (p.getDescription() != null && p.getDescription().toLowerCase().contains(textFilter)) ||
                        String.valueOf(p.getTarget_amount()).contains(textFilter);

                // Filtre Statut
                boolean matchesStatus = selectedStatus.equals("Tous") ||
                        p.getStatus().toString().equals(selectedStatus);

                return matchesText && matchesStatus;
            });
            updateStatLabel(filteredData.size());
        };

        // 4. Écouter les changements sur les deux composants
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
            afficherErreur("Erreur lors du chargement des projets : " + e.getMessage());
        }
    }

    private void updateStatLabel(int count) {
        if (statLabel != null) {
            statLabel.setText(count + (count > 1 ? " Projets trouvés" : " Projet trouvé"));
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
            stage.setTitle("Modifier : " + selected.getTitle());
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));
            stage.showAndWait();

            chargerDonnees();
        } catch (IOException e) {
            afficherErreur("Erreur d'ouverture de l'interface de modification.");
        }
    }

    @FXML
    private void handleDeleteSelection() {
        projet selected = tableProjets.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Suppression");
        alert.setHeaderText("Confirmation");
        alert.setContentText("Voulez-vous vraiment supprimer le projet : " + selected.getTitle() + " ?");

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.YES) {
                try {
                    ps.deleteOne(selected);
                    chargerDonnees();
                } catch (Exception e) {
                    afficherErreur("Impossible de supprimer le projet.");
                }
            }
        });
    }

    @FXML
    private void ouvrirAjout() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/AjouterProjet.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle("Nouveau Projet");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));
            stage.showAndWait();
            chargerDonnees();
        } catch (IOException e) {
            afficherErreur("Erreur d'ouverture du formulaire d'ajout.");
        }
    }

    private void afficherErreur(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}