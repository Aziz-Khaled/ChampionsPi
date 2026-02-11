package tn.esprit.Champions.gui;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import tn.esprit.Champions.models.Status;
import tn.esprit.Champions.models.Utilisateur;
import tn.esprit.Champions.services.UtilisateurService;

import java.awt.*;
import java.io.File;
import java.sql.SQLException;
public class AdminPanelService {


    @FXML
    private Button btnGestionUsers;

    @FXML
    private VBox mainContent;

    private final UtilisateurService userService = new UtilisateurService();

    @FXML
    private void initialize() {
        btnGestionUsers.setOnAction(e -> showGestionUsers());
    }

    private void showGestionUsers() {
        try {

            // Load users (you may later filter only PENDING)
            ObservableList<Utilisateur> users =
                    FXCollections.observableArrayList(userService.SelectAll());

            TableView<Utilisateur> table = new TableView<>();
            table.setItems(users);

            // Full Name Column
            TableColumn<Utilisateur, String> colName = new TableColumn<>("Nom & Prenom");
            colName.setCellValueFactory(data ->
                    new SimpleStringProperty(
                            data.getValue().getNom() + " " +
                                    data.getValue().getPrenom()
                    )
            );

            // Email Column
            TableColumn<Utilisateur, String> colEmail = new TableColumn<>("Email");
            colEmail.setCellValueFactory(data ->
                    new SimpleStringProperty(data.getValue().getEmail())
            );

            // Identity File Column
            TableColumn<Utilisateur, String> colIdentity = new TableColumn<>("Identity File");

            colIdentity.setCellFactory(param -> new TableCell<>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);

                    if (empty) {
                        setText(null);
                        setGraphic(null);
                    } else {
                        Hyperlink link = new Hyperlink(getTableView()
                                .getItems()
                                .get(getIndex())
                                .getPiece_identite());

                        link.setOnAction(e -> {
                            try {
                                File file = new File("src/main/resources/assets/" + link.getText());
                                Desktop.getDesktop().open(file);
                            } catch (Exception ex) {
                                ex.printStackTrace();
                            }
                        });

                        setGraphic(link);
                    }
                }
            });

            // Personal Image Column
            TableColumn<Utilisateur, String> colImage = new TableColumn<>("Personal Image");
            colImage.setCellValueFactory(data ->
                    new SimpleStringProperty(data.getValue().getUser_image())
            );

            // Actions Column (Accept / Reject)
            TableColumn<Utilisateur, Void> colActions = new TableColumn<>("Actions");

            colActions.setCellFactory(param -> new TableCell<>() {

                private final Button btnAccept = new Button("Accept");
                private final Button btnReject = new Button("Reject");
                private final HBox box = new HBox(5, btnAccept, btnReject);

                {
                    btnAccept.setOnAction(e -> {
                        Utilisateur user = getTableView().getItems().get(getIndex());
                        updateStatus(user, Status.ACTIVE);
                    });

                    btnReject.setOnAction(e -> {
                        Utilisateur user = getTableView().getItems().get(getIndex());
                        updateStatus(user, Status.DESACTIVE);
                    });
                }

                @Override
                protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    setGraphic(empty ? null : box);
                }
            });

            table.getColumns().addAll(colName, colEmail, colIdentity, colImage, colActions);

            // Replace content
            mainContent.getChildren().clear();
            mainContent.getChildren().add(table);

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void updateStatus(Utilisateur user, Status status) {
        try {
            user.setStatut(status);
            userService.updateOne(user);
            showGestionUsers();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
