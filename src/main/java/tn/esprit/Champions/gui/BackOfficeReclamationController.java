package tn.esprit.Champions.gui;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import tn.esprit.Champions.models.Reclamation;
import tn.esprit.Champions.models.StatutReclamation;
import tn.esprit.Champions.services.ReclamationService;

import java.sql.SQLException;

public class BackOfficeReclamationController {

    @FXML private TableView<Reclamation> tableReclamations;

    // CORRECTION : colUser doit être <Reclamation, String> car on affiche "nomUtilisateur" (Sarra)
    @FXML private TableColumn<Reclamation, String> colUser;

    // CORRECTION : colFormation est déjà String, c'est parfait pour "titreFormation"
    @FXML private TableColumn<Reclamation, String> colFormation;

    @FXML private TableColumn<Reclamation, String> colSujet;
    @FXML private TableColumn<Reclamation, String> colDescription;
    @FXML private TableColumn<Reclamation, StatutReclamation> colStatut;
    @FXML private ComboBox<StatutReclamation> comboStatut;

    private final ReclamationService rs = new ReclamationService();
    private ObservableList<Reclamation> reclamationList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        // Liaison des colonnes aux attributs String du modèle
        // Ces noms doivent correspondre exactement aux getters dans Reclamation.java
        colUser.setCellValueFactory(new PropertyValueFactory<>("nomUtilisateur"));
        colFormation.setCellValueFactory(new PropertyValueFactory<>("titreFormation"));

        colSujet.setCellValueFactory(new PropertyValueFactory<>("sujet"));
        colDescription.setCellValueFactory(new PropertyValueFactory<>("description"));
        colStatut.setCellValueFactory(new PropertyValueFactory<>("statut"));

        // Gestion du style visuel des statuts
        setupStatusColumnStyle();

        // Remplissage du ComboBox avec les valeurs de l'Enum
        comboStatut.getItems().setAll(StatutReclamation.values());

        loadData();
    }

    private void setupStatusColumnStyle() {
        colStatut.setCellFactory(column -> new TableCell<Reclamation, StatutReclamation>() {
            @Override
            protected void updateItem(StatutReclamation item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item.toString());
                    // Application des couleurs selon le statut
                    switch (item) {
                        case TRAITEE: setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;"); break;
                        case EN_ATTENTE: setStyle("-fx-text-fill: #f39c12; -fx-font-weight: bold;"); break;
                        case REJETEE: setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;"); break;
                        default: setStyle(""); break;
                    }
                }
            }
        });
    }

    private void loadData() {
        try {
            reclamationList.clear();
            // Appel de la méthode selectAll() qui contient maintenant les JOIN SQL
            reclamationList.addAll(rs.selectAll());
            tableReclamations.setItems(reclamationList);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleUpdateStatut() {
        Reclamation selected = tableReclamations.getSelectionModel().getSelectedItem();
        StatutReclamation nouveauStatut = comboStatut.getValue();

        if (selected != null && nouveauStatut != null) {
            try {
                // Utilisation de l'ID réel pour la mise à jour en base
                rs.updateStatut(selected.getIdRec(), nouveauStatut);
                loadData();
                showAlert("Succès", "Le statut a été mis à jour.");
            } catch (SQLException e) {
                e.printStackTrace();
            }
        } else {
            showAlert("Attention", "Veuillez sélectionner une ligne et un statut.");
        }
    }

    @FXML
    private void handleDelete() {
        Reclamation selected = tableReclamations.getSelectionModel().getSelectedItem();
        if (selected != null) {
            try {
                rs.deleteOne(selected.getIdRec());
                loadData();
                showAlert("Supprimé", "La réclamation a été supprimée.");
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}