package tn.esprit.Champions.gui;

import javafx.beans.property.SimpleStringProperty;
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
import tn.esprit.Champions.models.Utilisateur;
import tn.esprit.Champions.models.credit;
import tn.esprit.Champions.models.CreditStatus;
import tn.esprit.Champions.models.projet;
import tn.esprit.Champions.services.creditService;
import tn.esprit.Champions.services.projetService;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

public class AfficherCreditsController implements Initializable {

    @FXML private TableView<credit> tableCredits;
    @FXML private TableColumn<credit, String> colProjet;
    @FXML private TableColumn<credit, Double> colMontant;
    @FXML private TableColumn<credit, String> colDevise;
    @FXML private TableColumn<credit, Double> colTaux;
    @FXML private TableColumn<credit, Integer> colDuree;
    @FXML private TableColumn<credit, CreditStatus> colStatus;
    @FXML private TableColumn<credit, String> colDescription;
    @FXML private TableColumn<credit, Void> colActions;

    @FXML private Button btnEdit, btnDelete;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> statusFilterCombo;
    @FXML private Label statLabel;

    private final creditService cs = new creditService();
    private final projetService ps = new projetService();

    private final ObservableList<credit> masterData = FXCollections.observableArrayList();

    // ✅ Cache pour stocker les noms des projets et permettre une recherche instantanée
    private final Map<Integer, String> projetCache = new HashMap<>();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        configurerColonnes();
        chargerDonnees();
        configurerRechercheDynamique();

        tableCredits.getSelectionModel().selectedItemProperty().addListener((obs, old, selection) -> {
            boolean selectionExiste = (selection != null);
            btnEdit.setDisable(!selectionExiste);
            btnDelete.setDisable(!selectionExiste);
        });
    }

    private void configurerColonnes() {
        // Affichage du titre du projet (utilise le cache pour la performance)
        colProjet.setCellValueFactory(cellData -> {
            int id = cellData.getValue().getProject_id();
            return new SimpleStringProperty(projetCache.getOrDefault(id, "Inconnu"));
        });

        colMontant.setCellValueFactory(new PropertyValueFactory<>("montant"));
        colDevise.setCellValueFactory(new PropertyValueFactory<>("devise"));
        colTaux.setCellValueFactory(new PropertyValueFactory<>("taux"));
        colDuree.setCellValueFactory(new PropertyValueFactory<>("duree"));
        colDescription.setCellValueFactory(new PropertyValueFactory<>("description"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        colStatus.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(CreditStatus item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) setGraphic(null);
                else {
                    Label badge = new Label(item.toString());
                    badge.setPrefWidth(90);
                    badge.setAlignment(Pos.CENTER);
                    String styleBase = "-fx-padding: 4 10; -fx-background-radius: 15; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 11px;";

                    if (item == CreditStatus.OPEN) badge.setStyle(styleBase + "-fx-background-color: #f39c12;");
                    else if (item == CreditStatus.APPROVED) badge.setStyle(styleBase + "-fx-background-color: #27ae60;");
                    else if (item == CreditStatus.REJECTED) badge.setStyle(styleBase + "-fx-background-color: #e74c3c;");
                    else badge.setStyle(styleBase + "-fx-background-color: #95a5a6;");
                    setGraphic(badge);
                }
            }
        });

        colActions.setCellFactory(param -> new TableCell<>() {
            private final Button btnOffers = new Button("Offres");
            {
                btnOffers.setStyle("-fx-background-color: transparent; -fx-border-color: #0fbcf9; -fx-border-radius: 15; -fx-text-fill: #0fbcf9; -fx-font-weight: bold; -fx-cursor: hand;");
                btnOffers.setOnAction(event -> ouvrirNegociationEmprunteur(getTableView().getItems().get(getIndex())));
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) setGraphic(null);
                else {
                    HBox c = new HBox(btnOffers); c.setAlignment(Pos.CENTER); setGraphic(c);
                }
            }
        });
    }

    private void configurerRechercheDynamique() {
        ObservableList<String> options = FXCollections.observableArrayList("Tous");
        for (CreditStatus s : CreditStatus.values()) options.add(s.toString());
        statusFilterCombo.setItems(options);
        statusFilterCombo.setValue("Tous");

        FilteredList<credit> filteredData = new FilteredList<>(masterData, c -> true);

        Runnable applyFilters = () -> {
            String text = searchField.getText() == null ? "" : searchField.getText().toLowerCase();
            String status = statusFilterCombo.getValue();

            filteredData.setPredicate(c -> {
                // ✅ RECHERCHE PAR NOM DE PROJET (via le cache)
                String nomProjet = projetCache.getOrDefault(c.getProject_id(), "").toLowerCase();

                boolean matchesText = text.isEmpty() ||
                        nomProjet.contains(text) || // Recherche dans le nom du projet
                        c.getDescription().toLowerCase().contains(text) ||
                        c.getDevise().toLowerCase().contains(text) ||
                        String.valueOf(c.getMontant()).contains(text);

                boolean matchesStatus = status.equals("Tous") || c.getStatus().toString().equals(status);

                return matchesText && matchesStatus;
            });
            updateStatLabel(filteredData.size());
        };

        searchField.textProperty().addListener((obs, old, val) -> applyFilters.run());
        statusFilterCombo.valueProperty().addListener((obs, old, val) -> applyFilters.run());

        SortedList<credit> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(tableCredits.comparatorProperty());
        tableCredits.setItems(sortedData);
    }

    private void chargerDonnees() {
        try {
            // 1. Charger les projets pour le cache (Recherche par nom)
            projetCache.clear();
            for (projet p : ps.SelectAll()) {
                projetCache.put(p.getId_project(), p.getTitle());
            }

            // 2. Charger les crédits
            masterData.setAll(cs.SelectAll());
            updateStatLabel(masterData.size());
        } catch (Exception e) {
            afficherErreur("Erreur chargement : " + e.getMessage());
        }
    }

    private void updateStatLabel(int count) {
        if (statLabel != null) {
            statLabel.setText(count + (count > 1 ? " Crédits trouvés" : " Crédit trouvé"));
        }
    }

    @FXML
    private void ouvrirAjout() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/AjouterCredit.fxml"));
            Parent root = loader.load();
            AjouterCreditController controller = loader.getController();
            Utilisateur tempUser = new Utilisateur();
            tempUser.setId_user(1);
            controller.setConnectedUser(tempUser);

            Stage stage = new Stage();
            stage.setTitle("Nouvelle Demande");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));
            stage.showAndWait();
            chargerDonnees();
        } catch (IOException e) { afficherErreur("Erreur FXML : " + e.getMessage()); }
    }

    @FXML
    private void handleEditSelection() {
        credit selected = tableCredits.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ModifierCredit.fxml"));
            Parent root = loader.load();
            ModifierCreditController controller = loader.getController();
            controller.initData(selected);
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));
            stage.showAndWait();
            chargerDonnees();
        } catch (IOException e) { afficherErreur("Erreur FXML : " + e.getMessage()); }
    }

    @FXML
    private void handleDeleteSelection() {
        credit selected = tableCredits.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Supprimer ce crédit ?", ButtonType.YES, ButtonType.NO);
        alert.showAndWait().ifPresent(res -> {
            if (res == ButtonType.YES) {
                try { cs.deleteOne(selected); chargerDonnees(); }
                catch (Exception e) { afficherErreur("Erreur suppression."); }
            }
        });
    }

    private void ouvrirNegociationEmprunteur(credit c) {
        if (c == null) return;

        try {
            // 1. Extraire le titre du projet depuis ton cache existant
            String titreDuProjet = projetCache.getOrDefault(c.getProject_id(), "Projet #" + c.getProject_id());

            // 2. Charger le FXML
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/GestionNegociation.fxml"));
            Parent root = loader.load();

            // 3. Récupérer le contrôleur de la fenêtre de négociation
            NegociationController controller = loader.getController();

            // 4. ✅ Appel de la méthode corrigée (credit + titre)
            controller.setCreditSelectionne(c, titreDuProjet);

            // 5. Affichage en mode Pop-up (Modale)
            Stage stage = new Stage();
            stage.setTitle("Offres pour : " + titreDuProjet);
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
            afficherErreur("Erreur lors de l'ouverture : " + e.getMessage());
        }
    }

    private void afficherErreur(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setContentText(message);
        alert.showAndWait();
    }
}