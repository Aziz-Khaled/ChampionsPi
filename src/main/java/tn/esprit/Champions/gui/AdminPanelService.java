package tn.esprit.Champions.gui;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import tn.esprit.Champions.models.Status;
import tn.esprit.Champions.models.Utilisateur;
import tn.esprit.Champions.services.UtilisateurService;

import java.awt.Desktop;
import java.io.File;
import java.sql.SQLException;

public class AdminPanelService {

    @FXML
    private Button btnGestionUsers;

    @FXML
    private Button btnListeUsers;

    @FXML
    private VBox mainContent;

    private final UtilisateurService userService = new UtilisateurService();

    @FXML
    private void initialize() {
        btnGestionUsers.setOnAction(e -> showGestionUsers());
        btnListeUsers.setOnAction(e -> showAllUsers());
    }

    /* =====================================================
       =============== GESTION USERS (PENDING) =============
       ===================================================== */

    private void showGestionUsers() {
        try {

            // IMPORTANT: SelectAll() must return ONLY PENDING users
            ObservableList<Utilisateur> users =
                    FXCollections.observableArrayList(userService.SelectAll());

            TableView<Utilisateur> table = new TableView<>();
            table.setItems(users);
            table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

            // Full Name
            TableColumn<Utilisateur, String> colName = new TableColumn<>("Nom & Prenom");
            colName.setCellValueFactory(data ->
                    new SimpleStringProperty(
                            data.getValue().getNom() + " " +
                                    data.getValue().getPrenom()
                    )
            );

            // Email
            TableColumn<Utilisateur, String> colEmail = new TableColumn<>("Email");
            colEmail.setCellValueFactory(data ->
                    new SimpleStringProperty(data.getValue().getEmail())
            );

            // Identity File (Clickable)
            TableColumn<Utilisateur, Void> colIdentity = new TableColumn<>("Identity File");

            colIdentity.setCellFactory(param -> new TableCell<>() {
                private final Hyperlink link = new Hyperlink();

                {
                    link.setOnAction(e -> {
                        Utilisateur user = getTableView().getItems().get(getIndex());
                        openFile(user.getPiece_identite());
                    });
                }

                @Override
                protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);

                    if (empty) {
                        setGraphic(null);
                    } else {
                        Utilisateur user = getTableView().getItems().get(getIndex());
                        link.setText(user.getPiece_identite());
                        setGraphic(link);
                    }
                }
            });

            // Personal Image (Clickable)
            TableColumn<Utilisateur, Void> colImage = new TableColumn<>("Personal Image");

            colImage.setCellFactory(param -> new TableCell<>() {
                private final Hyperlink link = new Hyperlink();

                {
                    link.setOnAction(e -> {
                        Utilisateur user = getTableView().getItems().get(getIndex());
                        openFile(user.getUser_image());
                    });
                }

                @Override
                protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);

                    if (empty) {
                        setGraphic(null);
                    } else {
                        Utilisateur user = getTableView().getItems().get(getIndex());
                        link.setText(user.getUser_image());
                        setGraphic(link);
                    }
                }
            });

            // Actions (Accept / Reject with confirmation)
            TableColumn<Utilisateur, Void> colActions = new TableColumn<>("Actions");

            colActions.setCellFactory(param -> new TableCell<>() {

                private final Button btnAccept = new Button("Accept");
                private final Button btnReject = new Button("Reject");
                private final HBox box = new HBox(10, btnAccept, btnReject);

                {
                    btnAccept.setStyle("-fx-background-color: #27AE60; -fx-text-fill: white;");
                    btnReject.setStyle("-fx-background-color: #E74C3C; -fx-text-fill: white;");

                    btnAccept.setOnAction(e -> {
                        Utilisateur user = getTableView().getItems().get(getIndex());
                        confirmAndUpdate(user, Status.ACTIVE);
                    });

                    btnReject.setOnAction(e -> {
                        Utilisateur user = getTableView().getItems().get(getIndex());
                        confirmAndUpdate(user, Status.DESACTIVE);
                    });
                }

                @Override
                protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    setGraphic(empty ? null : box);
                }
            });

            table.getColumns().addAll(colName, colEmail, colIdentity, colImage, colActions);

            mainContent.getChildren().clear();
            mainContent.getChildren().add(table);

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void confirmAndUpdate(Utilisateur user, Status status) {

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation");
        confirm.setHeaderText(null);
        confirm.setContentText("Are you sure you want to " + status + " this user?");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                updateStatus(user, status);
            }
        });
    }

    private void updateStatus(Utilisateur user, Status status) {
        try {
            user.setStatut(status);
            userService.updateOne(user);
            showGestionUsers(); // refresh (user disappears)
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void openFile(String fileName) {
        try {
            File file = new File("src/main/resources/assets/" + fileName);
            Desktop.getDesktop().open(file);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /* =====================================================
       ============== LISTE DES UTILISATEURS ===============
       ===================================================== */

    private void showAllUsers() {
        try {

            ObservableList<Utilisateur> users =
                    FXCollections.observableArrayList(userService.getAllUsers());

            TableView<Utilisateur> table = new TableView<>();
            table.setItems(users);
            table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

            // Full Name
            TableColumn<Utilisateur, String> colName = new TableColumn<>("Nom & Prenom");
            colName.setCellValueFactory(data ->
                    new SimpleStringProperty(
                            data.getValue().getNom() + " " +
                                    data.getValue().getPrenom()
                    )
            );

            // Email
            TableColumn<Utilisateur, String> colEmail = new TableColumn<>("Email");
            colEmail.setCellValueFactory(data ->
                    new SimpleStringProperty(data.getValue().getEmail())
            );

            // Role
            TableColumn<Utilisateur, String> colRole = new TableColumn<>("Role");
            colRole.setCellValueFactory(data ->
                    new SimpleStringProperty(data.getValue().getRole().name())
            );

            // Status
            TableColumn<Utilisateur, String> colStatus = new TableColumn<>("Status");
            colStatus.setCellValueFactory(data ->
                    new SimpleStringProperty(data.getValue().getStatut().name())
            );

            // Actions (Update / Delete)
            TableColumn<Utilisateur, Void> colActions = new TableColumn<>("Actions");

            colActions.setCellFactory(param -> new TableCell<>() {

                private final Button btnUpdate = new Button("Update");
                private final Button btnDelete = new Button("Delete");
                private final HBox box = new HBox(10, btnUpdate, btnDelete);

                {
                    btnUpdate.setStyle("-fx-background-color: #2980B9; -fx-text-fill: white;");
                    btnDelete.setStyle("-fx-background-color: #C0392B; -fx-text-fill: white;");

                    btnUpdate.setOnAction(e -> {
                        Utilisateur user = getTableView().getItems().get(getIndex());
                        updateUser(user);
                    });

                    btnDelete.setOnAction(e -> {
                        Utilisateur user = getTableView().getItems().get(getIndex());
                        deleteUser(user);
                    });
                }

                @Override
                protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    setGraphic(empty ? null : box);
                }
            });

            table.getColumns().addAll(colName, colEmail, colRole, colStatus, colActions);

            mainContent.getChildren().clear();
            mainContent.getChildren().add(table);

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void deleteUser(Utilisateur user) {

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete User");
        confirm.setContentText("Are you sure you want to delete this user?");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    userService.deleteOne(user);
                    showAllUsers();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void updateUser(Utilisateur user) {

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Update User");

        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        // Create form fields
        TextField tfNom = new TextField(user.getNom());
        TextField tfPrenom = new TextField(user.getPrenom());
        TextField tfEmail = new TextField(user.getEmail());
        TextField tfPassword = new TextField(user.getMot_de_passe());
        TextField tfTelephone = new TextField(user.getTelephone());
        TextField tfIdentity = new TextField(user.getPiece_identite());
        TextField tfImage = new TextField(user.getUser_image());

        ComboBox<String> cbRole = new ComboBox<>();
        cbRole.getItems().addAll("ADMIN", "USER");
        cbRole.setValue(user.getRole().name());

        ComboBox<String> cbStatus = new ComboBox<>();
        cbStatus.getItems().addAll("PENDING", "ACTIVE", "DESACTIVE");
        cbStatus.setValue(user.getStatut().name());

        VBox content = new VBox(10,
                new Label("Nom"), tfNom,
                new Label("Prenom"), tfPrenom,
                new Label("Email"), tfEmail,
                new Label("Password"), tfPassword,
                new Label("Telephone"), tfTelephone,
                new Label("Role"), cbRole,
                new Label("Status"), cbStatus,
                new Label("Identity File"), tfIdentity,
                new Label("User Image"), tfImage
        );

        content.setStyle("-fx-padding: 20;");
        dialog.getDialogPane().setContent(content);

        dialog.showAndWait().ifPresent(response -> {

            if (response == saveButtonType) {

                try {
                    user.setNom(tfNom.getText());
                    user.setPrenom(tfPrenom.getText());
                    user.setEmail(tfEmail.getText());
                    user.setMot_de_passe(tfPassword.getText());
                    user.setTelephone(tfTelephone.getText());


                    user.setRole(tn.esprit.Champions.models.Role.valueOf(cbRole.getValue()));
                    user.setStatut(Status.valueOf(cbStatus.getValue()));

                    userService.updateOne(user);

                    showAllUsers(); // refresh table

                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        });
    }

}
