package tn.esprit.Champions.gui;

import javafx.fxml.FXML;
import javafx.stage.Stage;
import javafx.stage.Modality;
import javafx.scene.Scene;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import tn.esprit.Champions.models.projet;
import tn.esprit.Champions.models.projetStatus;
import tn.esprit.Champions.services.projetService;

import java.io.IOException;
import java.net.URL;
import java.sql.Timestamp;
import java.util.ResourceBundle;

public class AfficherProjetsController implements Initializable {

    @FXML private TableView<projet> tableProjets;
    @FXML private TableColumn<projet, Integer> colId;
    @FXML private TableColumn<projet, String> colTitre;
    @FXML private TableColumn<projet, String> colDescription;
    @FXML private TableColumn<projet, Double> colMontant;
    @FXML private TableColumn<projet, projetStatus> colStatus;
    @FXML private TableColumn<projet, Timestamp> colDateDebut;
    @FXML private TableColumn<projet, Void> colActions;
    @FXML private TextField searchField;

    private projetService ps = new projetService();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        configurerColonnes();
        configurerBoutonsActions();
        chargerDonnees();
    }

    private void configurerColonnes() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id_project"));
        colTitre.setCellValueFactory(new PropertyValueFactory<>("title"));
        colDescription.setCellValueFactory(new PropertyValueFactory<>("description"));
        colMontant.setCellValueFactory(new PropertyValueFactory<>("target_amount"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colDateDebut.setCellValueFactory(new PropertyValueFactory<>("start_date"));
    }

    private void chargerDonnees() {
        try {
            tableProjets.getItems().setAll(ps.SelectAll());
        } catch (Exception e) {
            System.err.println("Erreur chargement projets : " + e.getMessage());
        }
    }

    private void configurerBoutonsActions() {
        colActions.setCellFactory(param -> new TableCell<>() {
            private final Button btnEdit = new Button("✎");
            private final Button btnDelete = new Button("✕");
            private final HBox container = new HBox(btnEdit, btnDelete);

            {
                container.setSpacing(12);
                container.setAlignment(Pos.CENTER);

                // Style Modifier (Jaune)
                btnEdit.setStyle("-fx-background-color: #FFB300; -fx-text-fill: white; -fx-background-radius: 15; -fx-min-width: 30px; -fx-cursor: hand;");

                // Style Supprimer (Rouge)
                btnDelete.setStyle("-fx-background-color: #FF5252; -fx-text-fill: white; -fx-background-radius: 15; -fx-min-width: 30px; -fx-cursor: hand;");

                btnDelete.setOnAction(event -> {
                    projet p = getTableView().getItems().get(getIndex());
                    handleDelete(p);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : container);
            }
        });
    }

    private void handleDelete(projet p) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Supprimer le projet : " + p.getTitle() + " ?", ButtonType.YES, ButtonType.NO);
        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.YES) {
                try {
                    ps.deleteOne(p);
                    chargerDonnees();
                } catch (Exception e) {
                    System.err.println("Erreur suppression : " + e.getMessage());
                }
            }
        });
    }

    @FXML
    private void ouvrirAjout() {
        try {
            // 1. Charger le fichier FXML de l'interface d'ajout
            // Assure-toi que le chemin correspond à l'emplacement de ton fichier dans resources
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/AjouterProjet.fxml"));
            Parent root = loader.load();

            // 2. Créer une nouvelle fenêtre (Stage)
            Stage stage = new Stage();
            stage.setTitle("CHAMPIONS | Nouveau Projet");

            // 3. Rendre la fenêtre modale (bloque la fenêtre principale tant qu'elle est ouverte)
            stage.initModality(Modality.APPLICATION_MODAL);

            // 4. Configurer la scène et l'afficher
            stage.setScene(new Scene(root));
            stage.setResizable(false); // Optionnel : empêche de redimensionner la popup

            // Utiliser showAndWait pour rafraîchir le tableau automatiquement après la fermeture
            stage.showAndWait();

            // 5. Rafraîchir les données du tableau après l'ajout
            chargerDonnees();

        } catch (IOException e) {
            System.err.println("Erreur lors de l'ouverture de l'interface d'ajout : " + e.getMessage());
            e.printStackTrace();

            // Afficher une alerte à l'utilisateur en cas d'erreur de chargement
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erreur de chargement");
            alert.setHeaderText("Impossible d'ouvrir le formulaire");
            alert.setContentText("Le fichier AjouterProjet.fxml est introuvable ou corrompu.");
            alert.showAndWait();
        }
    }
}