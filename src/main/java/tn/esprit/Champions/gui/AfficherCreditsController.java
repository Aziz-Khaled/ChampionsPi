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

        // ✅ Design des Badges de Statut (Style Dark Mode)
        colStatus.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(CreditStatus item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) setGraphic(null);
                else {
                    Label badge = new Label(item.toString());
                    badge.setPrefWidth(100);
                    badge.setAlignment(Pos.CENTER);

                    // Style de base : Background translucide pour l'effet "Glassmorphism"
                    String styleBase = "-fx-padding: 6 12; -fx-background-radius: 20; -fx-font-weight: bold; -fx-font-size: 11px;";

                    if (item == CreditStatus.OPEN)
                        badge.setStyle(styleBase + "-fx-background-color: rgba(243, 156, 18, 0.2); -fx-text-fill: #f39c12; -fx-border-color: rgba(243, 156, 18, 0.3); -fx-border-radius: 20;");
                    else if (item == CreditStatus.APPROVED)
                        badge.setStyle(styleBase + "-fx-background-color: rgba(16, 185, 129, 0.2); -fx-text-fill: #10b981; -fx-border-color: rgba(16, 185, 129, 0.3); -fx-border-radius: 20;");
                    else if (item == CreditStatus.REJECTED)
                        badge.setStyle(styleBase + "-fx-background-color: rgba(231, 76, 60, 0.2); -fx-text-fill: #e74c3c; -fx-border-color: rgba(231, 76, 60, 0.3); -fx-border-radius: 20;");
                    else
                        badge.setStyle(styleBase + "-fx-background-color: rgba(148, 163, 184, 0.2); -fx-text-fill: #94a3b8; -fx-border-color: rgba(148, 163, 184, 0.3); -fx-border-radius: 20;");

                    setGraphic(badge);
                }
            }
        });

        // ✅ Design du bouton "Offres" (Style Cyan Marketplace)
        colActions.setCellFactory(param -> new TableCell<>() {
            private final Button btnOffers = new Button("Offres");
            {
                btnOffers.setStyle("-fx-background-color: rgba(56, 189, 248, 0.1); -fx-border-color: #38bdf8; -fx-border-radius: 12; -fx-background-radius: 12; -fx-text-fill: #38bdf8; -fx-font-weight: 900; -fx-cursor: hand; -fx-padding: 5 15;");
                btnOffers.setOnAction(event -> ouvrirNegociationEmprunteur(getTableView().getItems().get(getIndex())));

                // Effet Hover simple en code
                btnOffers.setOnMouseEntered(e -> btnOffers.setStyle("-fx-background-color: #38bdf8; -fx-text-fill: #020617; -fx-background-radius: 12; -fx-font-weight: 900;"));
                btnOffers.setOnMouseExited(e -> btnOffers.setStyle("-fx-background-color: rgba(56, 189, 248, 0.1); -fx-border-color: #38bdf8; -fx-border-radius: 12; -fx-background-radius: 12; -fx-text-fill: #38bdf8; -fx-font-weight: 900;"));
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
                String nomProjet = projetCache.getOrDefault(c.getProject_id(), "").toLowerCase();

                boolean matchesText = text.isEmpty() ||
                        nomProjet.contains(text) ||
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
            projetCache.clear();
            for (projet p : ps.SelectAll()) {
                projetCache.put(p.getId_project(), p.getTitle());
            }

            masterData.setAll(cs.SelectAll());
            updateStatLabel(masterData.size());
        } catch (Exception e) {
            afficherErreur("Erreur chargement : " + e.getMessage());
        }
    }

    private void updateStatLabel(int count) {
        if (statLabel != null) {
            statLabel.setText(count + (count > 1 ? " Crédits trouvés" : " Crédit trouvé"));
            // Style dynamique pour le label de stats
            statLabel.setStyle("-fx-background-color: rgba(56, 189, 248, 0.1); -fx-text-fill: #38bdf8; -fx-padding: 8 20; -fx-background-radius: 20; -fx-font-weight: bold;");
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
            Scene scene = new Scene(root);
            // Optionnel : Forcer le fond sombre sur la modale si nécessaire
            root.setStyle("-fx-background-color: #0f172a;");
            stage.setScene(scene);
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
            Scene scene = new Scene(root);
            root.setStyle("-fx-background-color: #0f172a;");
            stage.setScene(scene);
            stage.showAndWait();
            chargerDonnees();
        } catch (IOException e) { afficherErreur("Erreur FXML : " + e.getMessage()); }
    }

    @FXML
    private void handleDeleteSelection() {
        credit selected = tableCredits.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        // Custom Alert Style pour rester dans le thème
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Supprimer ce crédit ?", ButtonType.YES, ButtonType.NO);
        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.setStyle("-fx-background-color: #1e293b;");
        dialogPane.lookupAll(".label").forEach(node -> node.setStyle("-fx-text-fill: white;"));

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
            String titreDuProjet = projetCache.getOrDefault(c.getProject_id(), "Projet #" + c.getProject_id());
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/GestionNegociation.fxml"));
            Parent root = loader.load();
            NegociationController controller = loader.getController();
            controller.setCreditSelectionne(c, titreDuProjet);

            Stage stage = new Stage();
            stage.setTitle("Offres pour : " + titreDuProjet);
            stage.initModality(Modality.APPLICATION_MODAL);
            Scene scene = new Scene(root);
            root.setStyle("-fx-background-color: #0f172a;");
            stage.setScene(scene);
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
            afficherErreur("Erreur lors de l'ouverture : " + e.getMessage());
        }
    }

    private void afficherErreur(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.getDialogPane().setStyle("-fx-background-color: #1e293b;");
        alert.setContentText(message);
        alert.showAndWait();
    }
}