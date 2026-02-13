package tn.esprit.Champions.gui;

import javafx.beans.property.SimpleStringProperty;
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
import tn.esprit.Champions.models.credit;
import tn.esprit.Champions.models.CreditStatus;
import tn.esprit.Champions.models.projet;
import tn.esprit.Champions.services.creditService;
import tn.esprit.Champions.services.projetService;

import java.io.IOException;
import java.net.URL;
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

    @FXML private Button btnEdit;
    @FXML private Button btnDelete;
    @FXML private TextField searchField;

    private final creditService cs = new creditService();
    private final projetService ps = new projetService();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        configurerColonnes();
        chargerDonnees();

        tableCredits.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            boolean selectionExiste = (newSelection != null);
            btnEdit.setDisable(!selectionExiste);
            btnDelete.setDisable(!selectionExiste);
        });
    }

    private void configurerColonnes() {
        colProjet.setCellValueFactory(cellData -> {
            int idProjet = cellData.getValue().getProject_id();
            try {
                projet p = ps.findById(idProjet);
                return new SimpleStringProperty(p != null ? p.getTitle() : "Inconnu");
            } catch (Exception e) {
                return new SimpleStringProperty("Erreur");
            }
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
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    Label badge = new Label(item.toString());
                    badge.setPrefWidth(85);
                    badge.setAlignment(Pos.CENTER);
                    String styleBase = "-fx-padding: 3 10; -fx-background-radius: 12; -fx-font-weight: bold; -fx-text-fill: white; -fx-font-size: 11px;";

                    switch (item) {
                        case OPEN -> badge.setStyle(styleBase + "-fx-background-color: #f39c12;");
                        case APPROVED -> badge.setStyle(styleBase + "-fx-background-color: #27ae60;");
                        case REJECTED -> badge.setStyle(styleBase + "-fx-background-color: #e74c3c;");
                        default -> badge.setStyle(styleBase + "-fx-background-color: #95a5a6;");
                    }
                    setGraphic(badge);
                    setAlignment(Pos.CENTER);
                }
            }
        });

        colActions.setCellFactory(param -> new TableCell<>() {
            private final Button btnOffers = new Button("Offres");
            {
                btnOffers.setStyle("-fx-background-color: transparent; -fx-border-color: #0fbcf9; -fx-border-radius: 15; -fx-text-fill: #0fbcf9; -fx-font-weight: bold; -fx-font-size: 10px; -fx-cursor: hand;");
                btnOffers.setOnMouseEntered(e -> btnOffers.setStyle("-fx-background-color: #0fbcf9; -fx-text-fill: white; -fx-background-radius: 15; -fx-font-weight: bold; -fx-font-size: 10px;"));
                btnOffers.setOnMouseExited(e -> btnOffers.setStyle("-fx-background-color: transparent; -fx-border-color: #0fbcf9; -fx-border-radius: 15; -fx-text-fill: #0fbcf9; -fx-font-weight: bold; -fx-font-size: 10px;"));
                btnOffers.setOnAction(event -> ouvrirNegociationEmprunteur(getTableView().getItems().get(getIndex())));
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) setGraphic(null);
                else {
                    HBox container = new HBox(btnOffers);
                    container.setAlignment(Pos.CENTER);
                    setGraphic(container);
                }
            }
        });
    }

    private void chargerDonnees() {
        try { tableCredits.getItems().setAll(cs.SelectAll()); }
        catch (Exception e) { afficherErreur("Erreur chargement : " + e.getMessage()); }
    }

    private void ouvrirNegociationEmprunteur(credit c) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/GestionNegociation.fxml"));
            Parent root = loader.load();
            NegociationController controller = loader.getController();
            controller.setCreditSelectionne(c);

            Stage stage = new Stage();
            stage.setTitle("Offres de négociation");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.show();
        } catch (IOException e) { afficherErreur("Erreur FXML : " + e.getMessage()); }
    }

    @FXML
    private void ouvrirAjout() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/AjouterCredit.fxml"));
            Parent root = loader.load();

            // ✅ RÉPARATION : Passage de l'ID utilisateur pour éviter l'erreur de contrainte SQL
            AjouterCreditController controller = loader.getController();
            // Assurez-vous que l'ID 1 existe dans votre table 'utilisateur' sur phpMyAdmin
            controller.setConnectedUserId(1);

            Stage stage = new Stage();
            stage.setTitle("Nouvelle Demande");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.showAndWait();

            chargerDonnees();
        } catch (IOException e) {
            afficherErreur("Impossible d'ouvrir AjouterCredit.fxml : " + e.getMessage());
        }
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
            stage.setTitle("Modifier Crédit");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.showAndWait();

            chargerDonnees();
        } catch (IOException e) {
            afficherErreur("Impossible d'ouvrir ModifierCredit.fxml : " + e.getMessage());
        }
    }

    @FXML
    private void handleDeleteSelection() {
        credit selected = tableCredits.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Supprimer ce crédit définitivement ?", ButtonType.YES, ButtonType.NO);
        alert.setTitle("Confirmation de suppression");
        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.YES) {
                try {
                    cs.deleteOne(selected);
                    chargerDonnees();
                } catch (Exception e) {
                    afficherErreur("Erreur lors de la suppression : " + e.getMessage());
                }
            }
        });
    }

    private void afficherErreur(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setContentText(message);
        alert.showAndWait();
    }
}