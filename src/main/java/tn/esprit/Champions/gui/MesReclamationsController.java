package tn.esprit.Champions.gui;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import tn.esprit.Champions.models.Reclamation;
import tn.esprit.Champions.models.StatutReclamation;
import tn.esprit.Champions.services.ReclamationService;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

public class MesReclamationsController {

    @FXML private TableView<Reclamation> tableMesRec;
    @FXML private TableColumn<Reclamation, String> colSujet;
    @FXML private TableColumn<Reclamation, String> colDate;

    // Le type doit être StatutReclamation ici
    @FXML private TableColumn<Reclamation, StatutReclamation> colStatut;

    private final ReclamationService rs = new ReclamationService();

    @FXML
    public void initialize() {
        // Configuration des colonnes
        colSujet.setCellValueFactory(new PropertyValueFactory<>("sujet"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("dateEnvoi"));
        colStatut.setCellValueFactory(new PropertyValueFactory<>("statut"));

        // Personnalisation de l'affichage du statut (Couleurs)
        setupStatusColumn();

        // Chargement des données
        loadUserReclamations();
    }

    private void loadUserReclamations() {
        try {
            List<Reclamation> toutesLesRecs = rs.selectAll();

            // Filtrage pour l'utilisateur ID 1
            List<Reclamation> mesRecs = toutesLesRecs.stream()
                    .filter(r -> r.getIdUtilisateur() == 1)
                    .collect(Collectors.toList());

            ObservableList<Reclamation> data = FXCollections.observableArrayList(mesRecs);
            tableMesRec.setItems(data);

        } catch (SQLException e) {
            System.err.println("Erreur lors du chargement des réclamations : " + e.getMessage());
        }
    }

    private void setupStatusColumn() {
        // Spécification explicite des types <Reclamation, StatutReclamation>
        colStatut.setCellFactory(column -> new TableCell<Reclamation, StatutReclamation>() {
            @Override
            protected void updateItem(StatutReclamation item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    // Conversion de l'Enum en String pour l'affichage
                    setText(item.name());

                    // Design des badges de statut en utilisant l'Enum
                    switch (item) {
                        case EN_ATTENTE:
                            setStyle("-fx-text-fill: #f39c12; -fx-font-weight: bold;");
                            break;
                        case TRAITEE:
                            setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
                            break;
                        default:
                            setStyle("-fx-text-fill: #2980b9; -fx-font-weight: bold;");
                            break;
                    }
                }
            }
        });
    }

    @FXML
    private void refreshTable() {
        loadUserReclamations();
    }

    @FXML
    private void handleBack(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/student_dashboard.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            System.err.println("Erreur retour dashboard : " + e.getMessage());
        }
    }
}