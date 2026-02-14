package tn.esprit.Champions.gui;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import tn.esprit.Champions.models.Status;
import tn.esprit.Champions.models.Utilisateur;
import tn.esprit.Champions.services.UtilisateurService;
import tn.esprit.Champions.utils.UserSession;

import java.awt.Desktop;
import java.io.File;
import java.sql.SQLException;

public class AdminPanelService {

    @FXML private Button btnGestionUsers;
    @FXML private Button btnListeUsers;
    @FXML private VBox mainContent;
    @FXML private Button btnLogout;
    @FXML private Label lblTitle;

    private final UtilisateurService userService = new UtilisateurService();

    @FXML
    private void initialize() {

        Utilisateur currentUser = UserSession.getLoggedInUser();
        if (currentUser != null) {
            lblTitle.setText("Hello, " + currentUser.getNom());
        }
        showGestionUsers();
        btnGestionUsers.setOnAction(e -> showGestionUsers());
        btnListeUsers.setOnAction(e -> showAllUsers());

        if (btnLogout != null) {
            btnLogout.setOnAction(e -> {

                UserSession.clearSession();

                try {
                    javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/LoginPage.fxml"));
                    javafx.scene.Parent root = loader.load();
                    javafx.stage.Stage stage = (javafx.stage.Stage) btnLogout.getScene().getWindow();
                    stage.setScene(new javafx.scene.Scene(root));
                    stage.setTitle("Login - Champions");
                    stage.show();
                } catch (java.io.IOException ex) {
                    ex.printStackTrace();
                }
            });
        }
    }

    private void setActiveButton(Button activeBtn) {
        btnGestionUsers.getStyleClass().remove("nav-button-active");
        btnListeUsers.getStyleClass().remove("nav-button-active");
        activeBtn.getStyleClass().add("nav-button-active");
    }

    /**
     * The "Engine" of your UI: Renders the search bar and the table properly.
     */
    private void renderTable(TableView<Utilisateur> table) {
        mainContent.getChildren().clear();
        mainContent.setSpacing(15);

        // 1. Create a modern search bar
        TextField searchField = new TextField();
        searchField.setPromptText("Search by name or email...");
        searchField.setMaxWidth(350);
        searchField.getStyleClass().add("search-field"); // Link this in your CSS

        // 2. Setup Real-time Filter Logic
        ObservableList<Utilisateur> masterData = table.getItems();
        FilteredList<Utilisateur> filteredData = new FilteredList<>(masterData, p -> true);

        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            filteredData.setPredicate(user -> {
                if (newVal == null || newVal.isEmpty()) return true;
                String lowerCaseFilter = newVal.toLowerCase();
                String fullName = (user.getNom() + " " + user.getPrenom()).toLowerCase();

                return fullName.contains(lowerCaseFilter) ||
                        user.getEmail().toLowerCase().contains(lowerCaseFilter);
            });
        });

        // 3. Connect filtered data back to the table
        table.setItems(filteredData);

        // 4. Add components to the VBox
        mainContent.getChildren().addAll(searchField, table);
        VBox.setVgrow(table, Priority.ALWAYS);
    }

    /* =====================================================
       =============== GESTION USERS (PENDING) =============
       ===================================================== */

    private void showGestionUsers() {
        setActiveButton(btnGestionUsers);
        lblTitle.setText("Pending Verification");
        try {
            ObservableList<Utilisateur> users = FXCollections.observableArrayList(userService.SelectAll());

            TableView<Utilisateur> table = new TableView<>(users);
            table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

            TableColumn<Utilisateur, String> colName = new TableColumn<>("Nom & Prenom");
            colName.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getNom() + " " + data.getValue().getPrenom()));

            TableColumn<Utilisateur, String> colEmail = new TableColumn<>("Email");
            colEmail.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getEmail()));

            TableColumn<Utilisateur, Void> colIdentity = new TableColumn<>("Identity File");
            colIdentity.setCellFactory(param -> new TableCell<>() {
                private final Hyperlink link = new Hyperlink();
                { link.setOnAction(e -> openFile(getTableView().getItems().get(getIndex()).getPiece_identite())); }
                @Override
                protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty) setGraphic(null);
                    else {
                        link.setText(getTableView().getItems().get(getIndex()).getPiece_identite());
                        setGraphic(link);
                    }
                }
            });

            TableColumn<Utilisateur, Void> colActions = new TableColumn<>("Actions");
            colActions.setCellFactory(param -> new TableCell<>() {
                private final Button btnAccept = new Button("Accept");
                private final Button btnReject = new Button("Reject");
                private final HBox box = new HBox(10, btnAccept, btnReject);
                {
                    btnAccept.setStyle("-fx-background-color: #10b981; -fx-text-fill: white; -fx-background-radius: 6; -fx-cursor: hand;");
                    btnReject.setStyle("-fx-background-color: #f43f5e; -fx-text-fill: white; -fx-background-radius: 6; -fx-cursor: hand;");
                    btnAccept.setOnAction(e -> confirmAndUpdate(getTableView().getItems().get(getIndex()), Status.ACTIVE));
                    btnReject.setOnAction(e -> confirmAndUpdate(getTableView().getItems().get(getIndex()), Status.DESACTIVE));
                }
                @Override protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    setGraphic(empty ? null : box);
                }
            });

            table.getColumns().addAll(colName, colEmail, colIdentity, colActions);

            // Critical: Call the helper and STOP. Do not clear mainContent here.
            renderTable(table);

        } catch (SQLException e) { e.printStackTrace(); }
    }

    /* =====================================================
       ============== LISTE DES UTILISATEURS ===============
       ===================================================== */

    private void showAllUsers() {
        setActiveButton(btnListeUsers);
        lblTitle.setText("All Platform Users");
        try {
            ObservableList<Utilisateur> users = FXCollections.observableArrayList(userService.getAllUsers());

            TableView<Utilisateur> table = new TableView<>(users);
            table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

            TableColumn<Utilisateur, String> colName = new TableColumn<>("Nom & Prenom");
            colName.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getNom() + " " + data.getValue().getPrenom()));

            TableColumn<Utilisateur, String> colEmail = new TableColumn<>("Email");
            colEmail.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getEmail()));

            TableColumn<Utilisateur, String> colRole = new TableColumn<>("Role");
            colRole.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getRole().name()));

            TableColumn<Utilisateur, String> colStatus = new TableColumn<>("Status");
            colStatus.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getStatut().name()));

            TableColumn<Utilisateur, Void> colActions = new TableColumn<>("Actions");
            colActions.setCellFactory(param -> new TableCell<>() {
                private final Button btnUpdate = new Button("Update");
                private final Button btnDelete = new Button("Delete");
                private final HBox box = new HBox(10, btnUpdate, btnDelete);
                {
                    btnUpdate.setStyle("-fx-background-color: #2980B9; -fx-text-fill: white; -fx-background-radius: 4; -fx-cursor: hand;");
                    btnDelete.setStyle("-fx-background-color: #C0392B; -fx-text-fill: white; -fx-background-radius: 4; -fx-cursor: hand;");
                    btnUpdate.setOnAction(e -> updateUser(getTableView().getItems().get(getIndex())));
                    btnDelete.setOnAction(e -> deleteUser(getTableView().getItems().get(getIndex())));
                }
                @Override protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    setGraphic(empty ? null : box);
                }
            });

            table.getColumns().addAll(colName, colEmail, colRole, colStatus, colActions);

            // Critical: Call the helper and STOP.
            renderTable(table);

        } catch (SQLException e) { e.printStackTrace(); }
    }

    /* =====================================================
       =================== UTILITY METHODS =================
       ===================================================== */

    private void confirmAndUpdate(Utilisateur user, Status status) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation");
        confirm.setHeaderText(null);
        confirm.setContentText("Are you sure you want to " + status + " this user?");
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) updateStatus(user, status);
        });
    }

    private void updateStatus(Utilisateur user, Status status) {
        try {
            user.setStatut(status);
            userService.updateOne(user);
            showGestionUsers();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void openFile(String fileName) {
        try {
            File file = new File("src/main/resources/assets/" + fileName);
            if (file.exists()) {
                Desktop.getDesktop().open(file);
            } else {
                Alert error = new Alert(Alert.AlertType.ERROR, "File not found: " + fileName);
                error.show();
            }
        } catch (Exception e) { e.printStackTrace(); }
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
                } catch (SQLException e) { e.printStackTrace(); }
            }
        });
    }

    private void updateUser(Utilisateur user) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Update User");
        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        TextField tfNom = new TextField(user.getNom());
        TextField tfPrenom = new TextField(user.getPrenom());
        TextField tfEmail = new TextField(user.getEmail());
        ComboBox<String> cbRole = new ComboBox<>(FXCollections.observableArrayList("ADMIN", "USER"));
        cbRole.setValue(user.getRole().name());
        ComboBox<String> cbStatus = new ComboBox<>(FXCollections.observableArrayList("PENDING", "ACTIVE", "DESACTIVE"));
        cbStatus.setValue(user.getStatut().name());

        VBox content = new VBox(10,
                new Label("Nom"), tfNom,
                new Label("Prenom"), tfPrenom,
                new Label("Email"), tfEmail,
                new Label("Role"), cbRole,
                new Label("Status"), cbStatus
        );
        content.setStyle("-fx-padding: 20;");
        dialog.getDialogPane().setContent(content);

        dialog.showAndWait().ifPresent(response -> {
            if (response == saveButtonType) {
                try {
                    user.setNom(tfNom.getText());
                    user.setPrenom(tfPrenom.getText());
                    user.setEmail(tfEmail.getText());
                    user.setRole(tn.esprit.Champions.models.Role.valueOf(cbRole.getValue()));
                    user.setStatut(Status.valueOf(cbStatus.getValue()));
                    userService.updateOne(user);
                    showAllUsers();
                } catch (SQLException e) { e.printStackTrace(); }
            }
        });
    }
}