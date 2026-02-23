package tn.esprit.Champions.gui;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import tn.esprit.Champions.models.Status;
import tn.esprit.Champions.models.Utilisateur;
import tn.esprit.Champions.services.UtilisateurService;
import tn.esprit.Champions.utils.UserSession;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class AdminPanelService {

    // Navigation and Layout
    @FXML private Button btnOverview;
    @FXML private Button btnGestionUsers;
    @FXML private Button btnListeUsers;
    @FXML private Button btnCourses;
    @FXML private Button btnLogout;
    @FXML private VBox mainContent;
    @FXML private Label lblTitle;

    // Dashboard Components (Injected from FXML)
    @FXML private StackPane chartContainer;
    @FXML private StackPane pieChartContainer;
    @FXML private VBox recentActivityList;

    private final UtilisateurService userService = new UtilisateurService();

    // NEW: Variable to store the dashboard layout (ScrollPane)
    private Node dashboardView;

    @FXML
    private void initialize() {
        // 1. IMPORTANT: Capture the dashboard UI (the ScrollPane) before it's cleared
        if (mainContent != null && !mainContent.getChildren().isEmpty()) {
            dashboardView = mainContent.getChildren().get(0);
        }

        // Session Check
        Utilisateur currentUser = UserSession.getLoggedInUser();
        if (currentUser != null) {
            lblTitle.setText("Welcome, " + currentUser.getNom());
        }

        // Set Default View
        showDashboard();

        // Navigation Handlers
        btnOverview.setOnAction(e -> showDashboard());
        btnGestionUsers.setOnAction(e -> showGestionUsers());
        btnListeUsers.setOnAction(e -> showAllUsers());

        if (btnLogout != null) {
            btnLogout.setOnAction(e -> handleLogout());
        }
    }

    // --- DASHBOARD LOGIC ---

    private void showDashboard() {
        setActiveButton(btnOverview);
        lblTitle.setText("System Overview");

        // 2. Restore the original dashboard UI to the mainContent
        if (dashboardView != null) {
            mainContent.getChildren().clear();
            mainContent.getChildren().add(dashboardView);
        }

        setupLineChart();
        setupPieChart();
        setupActivityPulse();
    }

    private void setupLineChart() {
        if (chartContainer == null) return;
        chartContainer.getChildren().clear();

        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        LineChart<String, Number> lineChart = new LineChart<>(xAxis, yAxis);
        lineChart.setLegendVisible(false);
        lineChart.setAnimated(true);

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.getData().add(new XYChart.Data<>("Mon", 12));
        series.getData().add(new XYChart.Data<>("Tue", 25));
        series.getData().add(new XYChart.Data<>("Wed", 18));
        series.getData().add(new XYChart.Data<>("Thu", 45));
        series.getData().add(new XYChart.Data<>("Fri", 35));
        series.getData().add(new XYChart.Data<>("Sat", 65));
        series.getData().add(new XYChart.Data<>("Sun", 58));

        lineChart.getData().add(series);
        chartContainer.getChildren().add(lineChart);
    }

    private void setupPieChart() {
        if (pieChartContainer == null) return;
        pieChartContainer.getChildren().clear();

        ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList(
                new PieChart.Data("Active", 1284),
                new PieChart.Data("Pending", 43),
                new PieChart.Data("Rejected", 12)
        );

        PieChart pieChart = new PieChart(pieData);
        pieChart.setLabelsVisible(false);
        pieChart.setLegendVisible(true);
        pieChartContainer.getChildren().add(pieChart);
    }

    private void setupActivityPulse() {
        if (recentActivityList == null) return;
        recentActivityList.getChildren().clear();
        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));

        addLogEntry("System Scan Completed", "Security Health: 100%", time);
        addLogEntry("User 'Ahmed' requested verification", "Pending Review", time);
        addLogEntry("New Course Added: 'Blockchain 101'", "Content Update", time);
    }

    private void addLogEntry(String action, String type, String time) {
        HBox row = new HBox(15);
        row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        row.setStyle("-fx-padding: 10; -fx-background-color: rgba(255,255,255,0.02); -fx-background-radius: 10;");

        Label lblTime = new Label(time);
        lblTime.setStyle("-fx-text-fill: #2E5BFF; -fx-font-weight: bold; -fx-font-size: 11;");

        VBox texts = new VBox(2);
        Label lblAction = new Label(action);
        lblAction.setStyle("-fx-text-fill: white; -fx-font-size: 13; -fx-font-weight: bold;");
        Label lblType = new Label(type);
        lblType.setStyle("-fx-text-fill: #8E8E93; -fx-font-size: 11;");

        texts.getChildren().addAll(lblAction, lblType);
        row.getChildren().addAll(lblTime, texts);
        recentActivityList.getChildren().add(row);
    }

    // --- USER MANAGEMENT LOGIC ---

    private void showGestionUsers() {
        setActiveButton(btnGestionUsers);
        lblTitle.setText("Pending Verification");
        try {
            ObservableList<Utilisateur> users = FXCollections.observableArrayList(userService.SelectAll());
            TableView<Utilisateur> table = createBaseTable(users);

            TableColumn<Utilisateur, Void> colIdentity = new TableColumn<>("Identity File");
            colIdentity.setCellFactory(param -> new TableCell<>() {
                private final Hyperlink link = new Hyperlink();
                { link.setOnAction(e -> openFile(getTableView().getItems().get(getIndex()).getPiece_identite())); }
                @Override protected void updateItem(Void item, boolean empty) {
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

            table.getColumns().addAll(colIdentity, colActions);
            renderTable(table);
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void showAllUsers() {
        setActiveButton(btnListeUsers);
        lblTitle.setText("All Platform Users");
        try {
            ObservableList<Utilisateur> users = FXCollections.observableArrayList(userService.getAllUsers());
            TableView<Utilisateur> table = createBaseTable(users);

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

            table.getColumns().addAll(colRole, colStatus, colActions);
            renderTable(table);
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private TableView<Utilisateur> createBaseTable(ObservableList<Utilisateur> data) {
        TableView<Utilisateur> table = new TableView<>(data);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<Utilisateur, String> colName = new TableColumn<>("Nom & Prenom");
        colName.setCellValueFactory(dataCell -> new SimpleStringProperty(dataCell.getValue().getNom() + " " + dataCell.getValue().getPrenom()));

        TableColumn<Utilisateur, String> colEmail = new TableColumn<>("Email");
        colEmail.setCellValueFactory(dataCell -> new SimpleStringProperty(dataCell.getValue().getEmail()));

        table.getColumns().addAll(colName, colEmail);
        return table;
    }

    private void renderTable(TableView<Utilisateur> table) {
        mainContent.getChildren().clear();
        mainContent.setSpacing(15);

        TextField searchField = new TextField();
        searchField.setPromptText("Search by name or email...");
        searchField.getStyleClass().add("text-field");

        FilteredList<Utilisateur> filteredData = new FilteredList<>(table.getItems(), p -> true);
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            filteredData.setPredicate(user -> {
                if (newVal == null || newVal.isEmpty()) return true;
                String lowerCaseFilter = newVal.toLowerCase();
                return user.getNom().toLowerCase().contains(lowerCaseFilter) ||
                        user.getEmail().toLowerCase().contains(lowerCaseFilter);
            });
        });

        table.setItems(filteredData);
        mainContent.getChildren().addAll(searchField, table);
        VBox.setVgrow(table, Priority.ALWAYS);
    }

    // --- UTILS & CRUD ACTIONS ---

    private void confirmAndUpdate(Utilisateur user, Status status) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setContentText("Set " + user.getNom() + " status to " + status + "?");
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    user.setStatut(status);
                    userService.updateOne(user);
                    showGestionUsers();
                } catch (SQLException e) { e.printStackTrace(); }
            }
        });
    }

    private void openFile(String fileName) {
        try {
            File file = new File("src/main/resources/assets/" + fileName);
            if (file.exists()) Desktop.getDesktop().open(file);
        } catch (IOException e) { e.printStackTrace(); }
    }

    private void setActiveButton(Button activeBtn) {
        btnOverview.getStyleClass().remove("nav-button-active");
        btnGestionUsers.getStyleClass().remove("nav-button-active");
        btnListeUsers.getStyleClass().remove("nav-button-active");
        activeBtn.getStyleClass().add("nav-button-active");
    }

    private void handleLogout() {
        UserSession.clearSession();
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/LoginPage.fxml"));
            javafx.scene.Parent root = loader.load();
            javafx.stage.Stage stage = (javafx.stage.Stage) btnLogout.getScene().getWindow();
            stage.setScene(new javafx.scene.Scene(root));
            stage.show();
        } catch (IOException ex) { ex.printStackTrace(); }
    }

    private void deleteUser(Utilisateur user) {
        try {
            userService.deleteOne(user);
            showAllUsers();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void updateUser(Utilisateur user) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Update User");

        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.setPrefWidth(500);

        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialogPane.getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        TextField tfNom = new TextField(user.getNom());
        tfNom.setMaxWidth(Double.MAX_VALUE);

        TextField tfPrenom = new TextField(user.getPrenom());
        tfPrenom.setMaxWidth(Double.MAX_VALUE);

        TextField tfEmail = new TextField(user.getEmail());
        tfEmail.setMaxWidth(Double.MAX_VALUE);

        ComboBox<tn.esprit.Champions.models.Role> cbRole = new ComboBox<>(FXCollections.observableArrayList(tn.esprit.Champions.models.Role.values()));
        cbRole.setValue(user.getRole());
        cbRole.getStyleClass().add("filter-combo");
        cbRole.setMaxWidth(Double.MAX_VALUE);

        ComboBox<String> cbStatus = new ComboBox<>(FXCollections.observableArrayList("PENDING", "ACTIVE", "DESACTIVE"));
        cbStatus.setValue(user.getStatut().name());
        cbStatus.getStyleClass().add("filter-combo");
        cbStatus.setMaxWidth(Double.MAX_VALUE);

        VBox content = new VBox(12,
                new Label("Nom"), tfNom,
                new Label("Prenom"), tfPrenom,
                new Label("Email"), tfEmail,
                new Label("Role"), cbRole,
                new Label("Status"), cbStatus
        );

        content.setStyle("-fx-padding: 25; -fx-font-size: 14px;");
        content.setFillWidth(true);
        dialogPane.setContent(content);

        dialog.showAndWait().ifPresent(response -> {
            if (response == saveButtonType) {
                try {
                    user.setNom(tfNom.getText());
                    user.setPrenom(tfPrenom.getText());
                    user.setEmail(tfEmail.getText());
                    user.setRole(cbRole.getValue());
                    user.setStatut(Status.valueOf(cbStatus.getValue()));
                    userService.updateOne(user);
                    showAllUsers();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        });
    }
}