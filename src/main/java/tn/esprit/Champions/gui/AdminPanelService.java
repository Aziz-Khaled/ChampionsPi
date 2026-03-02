package tn.esprit.Champions.gui;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import tn.esprit.Champions.models.AccountStatus;
import tn.esprit.Champions.models.Status;
import tn.esprit.Champions.models.Utilisateur;
import tn.esprit.Champions.models.LogEntry;
import tn.esprit.Champions.services.IdentityAIService;
import tn.esprit.Champions.services.UtilisateurService;
import tn.esprit.Champions.services.LogEntryService;
import tn.esprit.Champions.services.AMLService; // Added
import tn.esprit.Champions.utils.FaceMatchService;
import tn.esprit.Champions.utils.UserSession;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

public class AdminPanelService {

    // Navigation and Layout
    @FXML private Button btnOverview, btnGestionUsers, btnListeUsers, btnLogs, btnCourses, btnLogout, rv_now , btnAssetManagement;
    @FXML private VBox mainContent, recentActivityList;
    @FXML private Label lblTitle, lblTotalUsers, lblPendingUsers, lblDisabledUsers;
    @FXML private StackPane chartContainer, pieChartContainer;
    @FXML private Label lblActiveUsers;

    private final UtilisateurService userService = new UtilisateurService();
    private final LogEntryService logEntryService = new LogEntryService();
    private final FaceMatchService faceMatchService = new FaceMatchService();
    private final AMLService amlService = new AMLService();
    private final IdentityAIService identityAIService = new IdentityAIService();
    private Node dashboardView;

    private final Map<Integer, String> amlResults = new HashMap<>();
    private final Map<Integer, String> verificationScores = new HashMap<>();
    private final Map<Integer, String> ocrResults = new HashMap<>();

    @FXML
    private void initialize() {
        if (mainContent != null && !mainContent.getChildren().isEmpty()) {
            dashboardView = mainContent.getChildren().get(0);
        }

        Utilisateur currentUser = UserSession.getLoggedInUser();
        if (currentUser != null) {
            lblTitle.setText("Welcome, " + currentUser.getNom());
        }

        showDashboard();

        rv_now.setOnAction(e -> showGestionUsers());
        btnOverview.setOnAction(e -> showDashboard());
        btnGestionUsers.setOnAction(e -> showGestionUsers());
        btnListeUsers.setOnAction(e -> showAllUsers());
        btnLogs.setOnAction(e -> showLogs());

        btnAssetManagement.setOnAction(e -> navigateTo("/crudAsset.fxml"));
        btnCourses.setOnAction(event -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/MainView.fxml"));
                Parent root = loader.load();
                Stage stage = (Stage) btnCourses.getScene().getWindow();
                Scene scene = new Scene(root);
                stage.setScene(scene);
                stage.show();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        if (btnLogout != null) {
            btnLogout.setOnAction(e -> handleLogout());
        }
        refreshStatistics();
    }

    private void navigateTo(String fxml) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxml));
            Parent root = loader.load();
            Stage stage = (Stage) btnOverview.getScene().getWindow();
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.show();
        } catch (IOException ex) {
            ex.printStackTrace();
            showAlert("Navigation Error", "Could not load " + fxml);
        }
    }
    private void runAIVerification(Utilisateur user, TableView<Utilisateur> table) {
        String assetPath = "src/main/resources/assets/";
        File idFile = new File(assetPath + user.getPiece_identite());
        File selfieFile = new File(assetPath + user.getUser_image());

        if (!idFile.exists() || !selfieFile.exists()) {
            showAlert("File Error", "Required images not found in: " + assetPath);
            return;
        }

        // Set initial "loading" states in the UI maps
        verificationScores.put(user.getId_user(), "Analyzing...");
        amlResults.put(user.getId_user(), "Screening...");
        ocrResults.put(user.getId_user(), "Reading ID...");
        table.refresh();

        // Run heavy AI tasks in a background thread to prevent UI freezing
        new Thread(() -> {
            try {
                // 1. Face Comparison (Remote AI - Face++)
                float score = faceMatchService.compareFaces(idFile, selfieFile);
                String faceResultText = (score > 0) ? String.format("%.2f%%", score) : "No Match";

                // 2. AML/Sanctions Check (Remote API - Dilisense/OFAC)
                String amlStatus = amlService.checkSanctions(user.getNom() + " " + user.getPrenom());

                // 3. Identity OCR Check (Local AI - Tesseract)
                // Correctly using the 'identityAIService' instance here
                String extractedText = identityAIService.extractTextFromID(idFile.getAbsolutePath());
                boolean isNameValid = identityAIService.verifyNameMatch(extractedText, user.getNom());
                String ocrStatus = isNameValid ? "MATCH" : "MISMATCH";

                // Update the UI on the JavaFX Application Thread
                Platform.runLater(() -> {
                    verificationScores.put(user.getId_user(), faceResultText);
                    amlResults.put(user.getId_user(), amlStatus);
                    ocrResults.put(user.getId_user(), ocrStatus);

                    table.refresh();

                    logEntryService.saveLog("COMPLIANCE_CHECK",
                            String.format("User: %s | Face: %s | AML: %s | OCR: %s",
                                    user.getNom(), faceResultText, amlStatus, ocrStatus));
                });
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    verificationScores.put(user.getId_user(), "Error");
                    amlResults.put(user.getId_user(), "Failed");
                    ocrResults.put(user.getId_user(), "Error");
                    table.refresh();
                });
            }
        }).start();
    }

    private void showGestionUsers() {
        setActiveButton(btnGestionUsers);
        lblTitle.setText("Pending Verification");
        try {
            ObservableList<Utilisateur> users = FXCollections.observableArrayList(userService.SelectAll());
            TableView<Utilisateur> table = createBaseTable(users);

            // 1. FULL NAME COLUMN (Added Back)
            TableColumn<Utilisateur, String> colFullName = new TableColumn<>("User Full Name");
            colFullName.setCellValueFactory(cell -> new SimpleStringProperty(
                    cell.getValue().getNom() + " " + cell.getValue().getPrenom()
            ));
            colFullName.setMinWidth(150);

            // 2. Identity File Link Column
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

            // 3. Face Match Column
            TableColumn<Utilisateur, String> colMatchResult = new TableColumn<>("Face Match");
            colMatchResult.setCellValueFactory(data -> new SimpleStringProperty(
                    verificationScores.getOrDefault(data.getValue().getId_user(), "--%")
            ));

            // 4. ID NAME CHECK COLUMN (The AI OCR Result)
            TableColumn<Utilisateur, String> colOCR = new TableColumn<>("ID Name Check");
            colOCR.setCellFactory(param -> new TableCell<>() {
                @Override protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty) {
                        setGraphic(null);
                    } else {
                        String status = ocrResults.getOrDefault(getTableView().getItems().get(getIndex()).getId_user(), "WAITING");
                        Label lbl = new Label(status);
                        if (status.equals("MATCH")) {
                            lbl.setStyle("-fx-background-color: #10b981; -fx-text-fill: white; -fx-padding: 3 8; -fx-background-radius: 5; -fx-font-weight: bold;");
                        } else if (status.equals("MISMATCH")) {
                            lbl.setStyle("-fx-background-color: #f43f5e; -fx-text-fill: white; -fx-padding: 3 8; -fx-background-radius: 5; -fx-font-weight: bold;");
                        } else {
                            lbl.setStyle("-fx-text-fill: #94a3b8;");
                        }
                        setGraphic(lbl);
                    }
                }
            });

            // 5. AML Security Column
            TableColumn<Utilisateur, String> colAML = new TableColumn<>("AML Security");
            colAML.setCellFactory(param -> new TableCell<>() {
                @Override protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty) setGraphic(null);
                    else {
                        String status = amlResults.getOrDefault(getTableView().getItems().get(getIndex()).getId_user(), "NOT_CHECKED");
                        Label lblStatus = new Label(status);
                        if (status.equals("CLEAN")) {
                            lblStatus.setStyle("-fx-background-color: #10b981; -fx-text-fill: white; -fx-padding: 3 8; -fx-background-radius: 5;");
                        } else if (status.equals("FLAGGED")) {
                            lblStatus.setStyle("-fx-background-color: #f43f5e; -fx-text-fill: white; -fx-padding: 3 8; -fx-background-radius: 5;");
                        }
                        setGraphic(lblStatus);
                    }
                }
            });

            // 6. Action Buttons Column
            TableColumn<Utilisateur, Void> colActions = new TableColumn<>("Actions");
            colActions.setCellFactory(param -> new TableCell<>() {
                private final Button btnAI = new Button("Verify All");
                private final Button btnAccept = new Button("Accept");
                private final Button btnReject = new Button("Reject");
                private final HBox box = new HBox(8, btnAI, btnAccept, btnReject);
                {
                    btnAI.setStyle("-fx-background-color: #2E5BFF; -fx-text-fill: white; -fx-background-radius: 6;");
                    btnAccept.setStyle("-fx-background-color: #10b981; -fx-text-fill: white; -fx-background-radius: 6;");
                    btnReject.setStyle("-fx-background-color: #f43f5e; -fx-text-fill: white; -fx-background-radius: 6;");
                    btnAI.setOnAction(e -> runAIVerification(getTableView().getItems().get(getIndex()), table));
                    btnAccept.setOnAction(e -> confirmAndUpdate(getTableView().getItems().get(getIndex()), AccountStatus.ACTIVE));
                    btnReject.setOnAction(e -> confirmAndUpdate(getTableView().getItems().get(getIndex()), AccountStatus.DESACTIVE));
                }
                @Override protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    setGraphic(empty ? null : box);
                }
            });

            // Clear and rebuild columns in the correct order
            table.getColumns().clear();
            table.getColumns().addAll(colFullName, colIdentity, colMatchResult, colOCR, colAML, colActions);

            renderTable(table);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void showDashboard() {
        setActiveButton(btnOverview);
        lblTitle.setText("System Overview");
        if (dashboardView != null) {
            mainContent.getChildren().clear();
            mainContent.getChildren().add(dashboardView);
        }
        setupLineChart();
        setupPieChart();
        setupActivityPulse();
        refreshStatistics();
    }

    private void showLogs() {
        setActiveButton(btnLogs);
        lblTitle.setText("Live System Governance Logs");
        mainContent.getChildren().clear();
        mainContent.setSpacing(15);
        ObservableList<LogEntry> logData = FXCollections.observableArrayList(logEntryService.getAllLogs());
        TableView<LogEntry> logTable = new TableView<>(logData);
        logTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        TableColumn<LogEntry, String> colTime = new TableColumn<>("Timestamp");
        colTime.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getTime()));
        TableColumn<LogEntry, String> colAction = new TableColumn<>("Action");
        colAction.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getAction()));
        TableColumn<LogEntry, String> colDetails = new TableColumn<>("Details");
        colDetails.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getDetails()));
        logTable.getColumns().addAll(colTime, colAction, colDetails);
        TextField searchField = new TextField();
        searchField.setPromptText("Filter logs...");
        mainContent.getChildren().addAll(searchField, logTable);
        VBox.setVgrow(logTable, Priority.ALWAYS);
    }

    private void confirmAndUpdate(Utilisateur user, AccountStatus status) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Status Change");
        confirm.setContentText("Set " + user.getNom() + " status to " + status + "?");
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    user.setStatut(status);
                    userService.updateOne(user);
                    logEntryService.saveLog("USER_STATUS_CHANGE", "Admin changed " + user.getNom() + " to " + status);
                    showGestionUsers();
                    refreshStatistics();
                } catch (SQLException e) { e.printStackTrace(); }
            }
        });
    }
    private void setupLineChart() {
        if (chartContainer == null) return;
        chartContainer.getChildren().clear();

        CategoryAxis xAxis = new CategoryAxis();
        // Hide the dates (Tick Labels) and the small tick marks
        xAxis.setTickLabelsVisible(false);
        xAxis.setTickMarkVisible(false);
        xAxis.setOpacity(0); // Optional: makes the axis line itself invisible for a floating look

        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("New Users");

        LineChart<String, Number> lineChart = new LineChart<>(xAxis, yAxis);
        lineChart.setTitle("7-Day User Growth");
        lineChart.setLegendVisible(false);
        lineChart.setAnimated(false);
        lineChart.setCreateSymbols(true); // Keeps the dots on the line for clarity

        XYChart.Series<String, Number> series = new XYChart.Series<>();

        try {
            java.util.Map<String, Integer> data = userService.getUserAcquisitionStats();

            // Sorting and adding data
            data.entrySet().stream()
                    .sorted(java.util.Map.Entry.comparingByKey())
                    .forEach(entry -> {
                        series.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue()));
                    });

        } catch (SQLException e) {
            e.printStackTrace();
        }

        lineChart.getData().add(series);
        chartContainer.getChildren().add(lineChart);
    }

    private void setupPieChart() {
        try {
            java.util.Map<String, Integer> distribution = userService.getStatusDistribution();
            ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();
            distribution.forEach((status, count) -> {
                pieData.add(new PieChart.Data(status, count));
            });

            PieChart pieChart = new PieChart(pieData);
            pieChart.setLabelsVisible(true);
            pieChart.setLegendVisible(true);

            if (pieChartContainer != null) {
                pieChartContainer.getChildren().clear();
                pieChartContainer.getChildren().add(pieChart);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    private void setupActivityPulse() {
        if (recentActivityList == null) return;
        recentActivityList.getChildren().clear();
        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
        addLogEntry("System Scan Completed", "Security Health: 100%", time);
    }

    private void addLogEntry(String action, String type, String time) {
        HBox row = new HBox(15);
        row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        row.setStyle("-fx-padding: 10; -fx-background-color: rgba(255,255,255,0.02); -fx-background-radius: 10;");
        Label lblTime = new Label(time);
        lblTime.setStyle("-fx-text-fill: #2E5BFF; -fx-font-weight: bold;");
        VBox texts = new VBox(2);
        Label lblAction = new Label(action);
        lblAction.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");
        texts.getChildren().addAll(lblAction, new Label(type));
        row.getChildren().addAll(lblTime, texts);
        recentActivityList.getChildren().add(row);
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
                    btnUpdate.setStyle("-fx-background-color: #2980B9; -fx-text-fill: white; -fx-background-radius: 4;");
                    btnDelete.setStyle("-fx-background-color: #C0392B; -fx-text-fill: white; -fx-background-radius: 4;");
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
        TableColumn<Utilisateur, String> colName = new TableColumn<>("Name");
        colName.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getNom() + " " + cell.getValue().getPrenom()));
        table.getColumns().add(colName);
        return table;
    }

    private void renderTable(TableView<Utilisateur> table) {
        mainContent.getChildren().clear();
        mainContent.setSpacing(15);
        TextField searchField = new TextField();
        searchField.setPromptText("Search users...");
        searchField.getStyleClass().add("text-field");

        FilteredList<Utilisateur> filteredData = new FilteredList<>(table.getItems(), p -> true);
        searchField.textProperty().addListener((obs, old, newVal) -> {
            filteredData.setPredicate(user -> newVal == null || newVal.isEmpty() ||
                    user.getNom().toLowerCase().contains(newVal.toLowerCase()) ||
                    user.getEmail().toLowerCase().contains(newVal.toLowerCase()));
        });
        table.setItems(filteredData);
        mainContent.getChildren().addAll(searchField, table);
        VBox.setVgrow(table, Priority.ALWAYS);
    }


    private void openFile(String fileName) {
        try {
            File file = new File("src/main/resources/assets/" + fileName);
            if (file.exists()) Desktop.getDesktop().open(file);
            else showAlert("Error", "File not found: " + fileName);
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
            ((javafx.stage.Stage) btnLogout.getScene().getWindow()).setScene(new javafx.scene.Scene(root));
        } catch (IOException ex) { ex.printStackTrace(); }
    }

    private void deleteUser(Utilisateur user) {
        try {
            userService.deleteOne(user);
            showAllUsers();
            refreshStatistics();
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
                    user.setStatut(AccountStatus.valueOf(cbStatus.getValue()));
                    userService.updateOne(user);
                    showAllUsers();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void refreshStatistics() {
        try {
            int total = userService.getTotalUsersCount();
            int active = userService.getActiveUsersCount();
            int pending = userService.getPendingUsersCount();
            int disabled = userService.getDisabledUsersCount();
            if (lblTotalUsers != null) lblTotalUsers.setText(String.format("%,d", total));
            if (lblPendingUsers != null) lblPendingUsers.setText(String.valueOf(pending));
            if (lblDisabledUsers != null) lblDisabledUsers.setText(String.valueOf(disabled));
            if (lblActiveUsers != null) lblActiveUsers.setText(String.format("%,d", active));
            updatePieChartData(total, pending, disabled);
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void updatePieChartData(int total, int pending, int disabled) {
        if (pieChartContainer == null) return;
        pieChartContainer.getChildren().clear();
        int active = total - pending - disabled;
        ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList(
                new PieChart.Data("Active", active),
                new PieChart.Data("Pending", pending),
                new PieChart.Data("Disabled", disabled)
        );
        PieChart pieChart = new PieChart(pieData);
        pieChart.setLegendVisible(true);
        pieChartContainer.getChildren().add(pieChart);
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}