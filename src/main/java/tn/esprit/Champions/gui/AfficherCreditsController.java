package tn.esprit.Champions.gui;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import tn.esprit.Champions.models.credit;
import tn.esprit.Champions.models.CreditStatus;
import tn.esprit.Champions.services.creditService;

import java.net.URL;
import java.sql.SQLException;
import java.util.Optional;
import java.util.ResourceBundle;

public class AfficherCreditsController implements Initializable {

    @FXML private TableView<credit> tableCredits;
    @FXML private TableColumn<credit, Integer> colId;
    @FXML private TableColumn<credit, Double> colMontant;
    @FXML private TableColumn<credit, String> colDevise; // Ajouté pour correspondre au FXML
    @FXML private TableColumn<credit, Double> colTaux;
    @FXML private TableColumn<credit, CreditStatus> colStatus;
    @FXML private TableColumn<credit, String> colDescription;
    @FXML private TableColumn<credit, Void> colActions; // La colonne pour les boutons

    @FXML private TextField searchField;

    private creditService cs = new creditService();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // 1. Liaisons de données classiques
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colMontant.setCellValueFactory(new PropertyValueFactory<>("montant"));
        colDevise.setCellValueFactory(new PropertyValueFactory<>("devise"));
        colTaux.setCellValueFactory(new PropertyValueFactory<>("taux"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colDescription.setCellValueFactory(new PropertyValueFactory<>("description"));

        // 2. Configuration de la colonne ACTIONS (Boutons)
        configurerBoutonsActions();

        // 3. Charger les données
        chargerDonnees();
    }

    private void configurerBoutonsActions() {
        colActions.setCellFactory(param -> new TableCell<>() {
            private final Button btnEdit = new Button("✎"); // Icône plus fine
            private final Button btnDelete = new Button("✕"); // Une croix élégante ou "🗑"
            private final HBox container = new HBox(btnEdit, btnDelete);

            {
                container.setSpacing(12);
                container.setAlignment(Pos.CENTER);

                // --- STYLE BOUTON MODIFIER (Jaune/Or) ---
                btnEdit.setStyle(
                        "-fx-background-color: #FFB300;" + // Couleur ambre
                                "-fx-text-fill: white;" +
                                "-fx-background-radius: 15;" + // Rend le bouton rond
                                "-fx-min-width: 30px;" +
                                "-fx-min-height: 30px;" +
                                "-fx-font-weight: bold;" +
                                "-fx-cursor: hand;"
                );

                // --- STYLE BOUTON SUPPRIMER (Rouge Corail) ---
                btnDelete.setStyle(
                        "-fx-background-color: #FF5252;" + // Rouge vif moderne
                                "-fx-text-fill: white;" +
                                "-fx-background-radius: 15;" +
                                "-fx-min-width: 30px;" +
                                "-fx-min-height: 30px;" +
                                "-fx-font-weight: bold;" +
                                "-fx-cursor: hand;"
                );

                // Effets de survol (Hover) pour les rendre interactifs
                btnEdit.setOnMouseEntered(e -> btnEdit.setStyle(btnEdit.getStyle() + "-fx-background-color: #FFA000; -fx-scale-x: 1.1; -fx-scale-y: 1.1;"));
                btnEdit.setOnMouseExited(e -> btnEdit.setStyle(btnEdit.getStyle().replace("-fx-background-color: #FFA000; -fx-scale-x: 1.1; -fx-scale-y: 1.1;", "")));

                btnDelete.setOnMouseEntered(e -> btnDelete.setStyle(btnDelete.getStyle() + "-fx-background-color: #D32F2F; -fx-scale-x: 1.1; -fx-scale-y: 1.1;"));
                btnDelete.setOnMouseExited(e -> btnDelete.setStyle(btnDelete.getStyle().replace("-fx-background-color: #D32F2F; -fx-scale-x: 1.1; -fx-scale-y: 1.1;", "")));

                btnDelete.setOnAction(event -> handleDelete(getTableView().getItems().get(getIndex())));
                btnEdit.setOnAction(event -> handleEdit(getTableView().getItems().get(getIndex())));
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : container);
            }
        });
    }

    private void chargerDonnees() {
        try {
            ObservableList<credit> data = FXCollections.observableArrayList(cs.SelectAll());
            tableCredits.setItems(data);
        } catch (SQLException e) {
            System.err.println("Erreur SQL lors du chargement : " + e.getMessage());
        }
    }
    @FXML
    private void ouvrirAjout() {
        try {
            // Chargement de la nouvelle interface
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/AjouterCredit.fxml"));
            javafx.scene.Parent root = loader.load();

            // Création de la fenêtre (Stage)
            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL); // Bloque la fenêtre principale
            stage.setTitle("Nouvelle Demande");
            stage.setScene(new javafx.scene.Scene(root));

            stage.showAndWait(); // Attend la fermeture pour continuer

            chargerDonnees(); // Rafraîchit le tableau automatiquement après l'ajout

        } catch (java.io.IOException e) {
            System.err.println("Erreur chargement FXML : " + e.getMessage());
        }
    }

    private void handleDelete(credit c) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation de suppression");
        alert.setHeaderText("Supprimer le crédit REF: " + c.getId() + " ?");
        alert.setContentText("Êtes-vous sûr de vouloir supprimer ce crédit ?");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                cs.deleteOne(c); // Utilisation de deleteOne(credit) de ton service
                chargerDonnees(); // Rafraîchir la liste
            } catch (SQLException e) {
                System.err.println("Erreur lors de la suppression : " + e.getMessage());
            }
        }
    }

    private void handleEdit(credit c) {
        // Pour l'instant on affiche juste l'ID, tu pourras ouvrir ta popup ici
        System.out.println("Modifier le crédit : " + c.getId());
    }
}