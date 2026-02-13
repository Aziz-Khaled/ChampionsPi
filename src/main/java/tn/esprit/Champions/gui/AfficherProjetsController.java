package tn.esprit.Champions.gui;

import javafx.fxml.FXML;
import javafx.stage.Stage;
import javafx.stage.Modality;
import javafx.scene.Scene;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
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
    @FXML private TableColumn<projet, Float> colMontant; // Adapté en Float selon ton modèle
    @FXML private TableColumn<projet, projetStatus> colStatus;
    @FXML private TableColumn<projet, Timestamp> colDateDebut;
    @FXML private TableColumn<projet, Timestamp> colDateFin;

    @FXML private Button btnEdit;
    @FXML private Button btnDelete;
    @FXML private TextField searchField;

    private projetService ps = new projetService();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        configurerColonnes();
        chargerDonnees();
        configurerSelectionTableau();
    }

    private void configurerColonnes() {
        colTitre.setCellValueFactory(new PropertyValueFactory<>("title"));
        colDescription.setCellValueFactory(new PropertyValueFactory<>("description"));
        colMontant.setCellValueFactory(new PropertyValueFactory<>("target_amount"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colDateDebut.setCellValueFactory(new PropertyValueFactory<>("start_date"));
        colDateFin.setCellValueFactory(new PropertyValueFactory<>("end_date"));
    }

    private void configurerSelectionTableau() {
        tableProjets.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            boolean selectionExiste = (newSelection != null);
            btnEdit.setDisable(!selectionExiste);
            btnDelete.setDisable(!selectionExiste);
        });
    }

    private void chargerDonnees() {
        try {
            tableProjets.getItems().setAll(ps.SelectAll());
        } catch (Exception e) {
            System.err.println("Erreur chargement projets : " + e.getMessage());
        }
    }

    @FXML
    private void handleEditSelection() {
        projet p = tableProjets.getSelectionModel().getSelectedItem();
        if (p != null) {
            try {
                // 1. Charger le FXML de modification
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/ModifierProjet.fxml"));
                Parent root = loader.load();

                // 2. Accéder au contrôleur de la fenêtre de modification pour lui envoyer l'objet
                ModifierProjetController controller = loader.getController();
                controller.initData(p);

                // 3. Configurer et afficher la fenêtre modale
                Stage stage = new Stage();
                stage.setTitle("CHAMPIONS | Modifier le Projet : " + p.getTitle());
                stage.initModality(Modality.APPLICATION_MODAL);
                stage.setScene(new Scene(root));
                stage.setResizable(false);

                stage.showAndWait(); // Attend la fermeture de la fenêtre pour continuer

                // 4. Rafraîchir le tableau après la modification
                chargerDonnees();

            } catch (IOException e) {
                afficherErreur("Impossible d'ouvrir l'interface de modification : " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    @FXML
    private void handleDeleteSelection() {
        projet p = tableProjets.getSelectionModel().getSelectedItem();
        if (p != null) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Supprimer le projet : " + p.getTitle() + " ?", ButtonType.YES, ButtonType.NO);
            alert.setHeaderText("Confirmation de suppression");
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
    }

    @FXML
    private void ouvrirAjout() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/AjouterProjet.fxml"));
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setTitle("CHAMPIONS | Nouveau Projet");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));
            stage.setResizable(false);

            stage.showAndWait();
            chargerDonnees();

        } catch (IOException e) {
            afficherErreur("Fichier AjouterProjet.fxml introuvable.");
            e.printStackTrace();
        }
    }

    private void afficherErreur(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erreur");
        alert.setContentText(message);
        alert.showAndWait();
    }
}