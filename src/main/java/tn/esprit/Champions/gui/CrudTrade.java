package tn.esprit.Champions.gui;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import tn.esprit.Champions.models.*;
import tn.esprit.Champions.services.TradeService;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

public class CrudTrade {


    @FXML private Label labelUser;
    @FXML private Label labelDateTime;


    @FXML private ComboBox<TradeType> boxType;
    @FXML private ComboBox<OrderMode> boxOrder;
    @FXML private TextField txtPrice;
    @FXML private TextField txtQuantity;
    @FXML private ComboBox<Status> boxStatus;


    @FXML private Button btnAdd;
    @FXML private Button btnUpdate;
    @FXML private Button btnClear;
    @FXML private Button btnDelete;


    @FXML private TableView<Trade> table;
    @FXML private TableColumn<Trade, Integer> id_trade;
    @FXML private TableColumn<Trade, TradeType> type;
    @FXML private TableColumn<Trade, OrderMode> order;
    @FXML private TableColumn<Trade, Double> price;
    @FXML private TableColumn<Trade, Double> quantity;
    @FXML private TableColumn<Trade, Status> status;
    @FXML private TableColumn<Trade, LocalDateTime> created;
    @FXML private TableColumn<Trade, LocalDateTime> executed;
    @FXML private TableColumn<Trade, Void> colAction;


    @FXML private TextField txtSearch;
    @FXML private Button btnClearSearch;
    @FXML private Label labelStatus;
    @FXML private Label labelCount;


    private TradeService tradeService = new TradeService();
    private List<Trade> allTrades;
    private Trade currentTrade = null;


    private String currentUsername = "Demo Client";
    private int currentUserId = 1;

    @FXML
    public void initialize() {

        initializeHeader();


        initializeComboBoxes();
        initializeTableColumns();
        loadTrades();
        addActionButtons();
        setupTableRowSelection();
    }



    private void initializeHeader() {
        // Set user label
        labelUser.setText("👤 " + currentUsername);


        updateDateTime();
        Timeline timeline = new Timeline(new KeyFrame(
                javafx.util.Duration.seconds(1),
                event -> updateDateTime()
        ));
        timeline.setCycleCount(Animation.INDEFINITE);
        timeline.play();
    }

    private void updateDateTime() {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
        labelDateTime.setText("📅 " + now.format(formatter));
    }



    private void initializeComboBoxes() {
        boxType.getItems().setAll(TradeType.values());
        boxOrder.getItems().setAll(OrderMode.values());
        boxStatus.getItems().setAll(Status.values());
    }

    private void initializeTableColumns() {

        type.setCellValueFactory(new PropertyValueFactory<>("tradeType"));
        order.setCellValueFactory(new PropertyValueFactory<>("orderMode"));
        price.setCellValueFactory(new PropertyValueFactory<>("price"));
        quantity.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        status.setCellValueFactory(new PropertyValueFactory<>("status"));
        created.setCellValueFactory(new PropertyValueFactory<>("createdAt"));
        executed.setCellValueFactory(new PropertyValueFactory<>("executedAt"));
    }

    private void loadTrades() {
        try {
            allTrades = tradeService.SelectAll();
            refreshTableView();
            updateStatusLabel();
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Database Error", "Failed to load trades: " + e.getMessage());
        }
    }



    @FXML
    private void handleAdd() {
        try {
            if (!validateForm()) {
                return;
            }

            Trade newTrade = new Trade(
                    0,
                    currentUserId,
                    1003,
                    boxType.getValue(),
                    boxOrder.getValue(),
                    Double.parseDouble(txtPrice.getText()),
                    Double.parseDouble(txtQuantity.getText()),
                    boxStatus.getValue(),
                    LocalDateTime.now(),
                    null,
                    2
            );

            tradeService.insertOne(newTrade);
            showAlert(Alert.AlertType.INFORMATION, "✅ Success", "Trade order submitted successfully!");
            loadTrades();
            clearForm();
            currentTrade = null;

        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "❌ Validation Error", "Price and Quantity must be valid numbers");
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "❌ Database Error", e.getMessage());
        }
    }

    @FXML
    private void handleUpdate() {
        try {
            if (currentTrade == null) {
                showAlert(Alert.AlertType.WARNING, "⚠️ Warning", "Please select a trade to update");
                return;
            }

            if (!validateForm()) {
                return;
            }

            currentTrade.setTradeType(boxType.getValue());
            currentTrade.setOrderMode(boxOrder.getValue());
            currentTrade.setPrice(Double.parseDouble(txtPrice.getText()));
            currentTrade.setQuantity(Double.parseDouble(txtQuantity.getText()));
            currentTrade.setStatus(boxStatus.getValue());
            currentTrade.setExecutedAt(LocalDateTime.now());

            tradeService.updateOne(currentTrade);
            showAlert(Alert.AlertType.INFORMATION, "✅ Success", "Trade order updated successfully!");
            loadTrades();
            clearForm();
            currentTrade = null;

        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "❌ Validation Error", "Price and Quantity must be valid numbers");
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "❌ Database Error", e.getMessage());
        }
    }

    @FXML
    private void handleDelete() {
        try {
            if (currentTrade == null) {
                showAlert(Alert.AlertType.WARNING, "⚠️ Warning", "Please select a trade to cancel");
                return;
            }

            Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
            confirmAlert.setTitle("Confirm Order Cancellation");
            confirmAlert.setHeaderText("Cancel Trade Order #" + currentTrade.getId());
            confirmAlert.setContentText("Are you sure you want to cancel this trade order?");

            if (confirmAlert.showAndWait().get() == ButtonType.OK) {
                tradeService.deleteOne(currentTrade);
                showAlert(Alert.AlertType.INFORMATION, "✅ Success", "Trade order cancelled successfully!");
                loadTrades();
                clearForm();
                currentTrade = null;
            }

        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "❌ Database Error", e.getMessage());
        }
    }

    @FXML
    private void handleClear() {
        clearForm();
        currentTrade = null;
        table.getSelectionModel().clearSelection();
    }



    @FXML
    private void handleSearch() {
        String searchText = txtSearch.getText().toLowerCase().trim();

        if (searchText.isEmpty()) {
            refreshTableView();
        } else {
            List<Trade> filteredTrades = allTrades.stream()
                    .filter(trade -> trade.getTradeType().toString().toLowerCase().contains(searchText)
                            || trade.getOrderMode().toString().toLowerCase().contains(searchText)
                            || trade.getStatus().toString().toLowerCase().contains(searchText)
                            || String.valueOf(trade.getPrice()).contains(searchText)
                            || String.valueOf(trade.getQuantity()).contains(searchText))
                    .collect(Collectors.toList());

            table.getItems().clear();
            table.getItems().addAll(filteredTrades);
        }

        updateStatusLabel();
    }

    @FXML
    private void handleClearSearch() {
        txtSearch.clear();
        refreshTableView();
    }



    private void setupTableRowSelection() {
        table.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                populateFormWithTrade(newVal);
            }
        });
    }

    private void populateFormWithTrade(Trade trade) {
        currentTrade = trade;
        boxType.setValue(trade.getTradeType());
        boxOrder.setValue(trade.getOrderMode());
        txtPrice.setText(String.valueOf(trade.getPrice()));
        txtQuantity.setText(String.valueOf(trade.getQuantity()));
        boxStatus.setValue(trade.getStatus());
    }

    private void addActionButtons() {
        colAction.setCellFactory(param -> new TableCell<>() {

            private final Button btnEdit = new Button("✏️");
            private final Button btnDelete = new Button("🗑");

            {
                btnEdit.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-padding: 6 10 6 10; -fx-font-size: 11px;");
                btnDelete.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-padding: 6 10 6 10; -fx-font-size: 11px;");

                btnEdit.setOnAction(event -> {
                    Trade trade = getTableView().getItems().get(getIndex());
                    table.getSelectionModel().select(trade);
                    populateFormWithTrade(trade);
                });

                btnDelete.setOnAction(event -> {
                    Trade trade = getTableView().getItems().get(getIndex());
                    deleteTradeDirectly(trade);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);

                if (empty) {
                    setGraphic(null);
                } else {
                    HBox box = new HBox(6, btnEdit, btnDelete);
                    box.setPadding(new Insets(4));
                    setGraphic(box);
                }
            }
        });
    }

    private void deleteTradeDirectly(Trade trade) {
        try {
            Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
            confirmAlert.setTitle("Confirm Order Cancellation");
            confirmAlert.setHeaderText("Cancel Trade Order #" + trade.getId());
            confirmAlert.setContentText("Are you sure?");

            if (confirmAlert.showAndWait().get() == ButtonType.OK) {
                tradeService.deleteOne(trade);
                showAlert(Alert.AlertType.INFORMATION, "✅ Success", "Trade order cancelled successfully!");
                loadTrades();
                if (currentTrade != null && currentTrade.getId() == trade.getId()) {
                    clearForm();
                    currentTrade = null;
                }
            }

        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "❌ Database Error", e.getMessage());
        }
    }



    private boolean validateForm() {
        if (txtPrice.getText().trim().isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "❌ Validation Error", "Price is required");
            return false;
        }

        if (txtQuantity.getText().trim().isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "❌ Validation Error", "Quantity is required");
            return false;
        }

        if (boxType.getValue() == null) {
            showAlert(Alert.AlertType.ERROR, "❌ Validation Error", "Trade Type is required");
            return false;
        }

        if (boxOrder.getValue() == null) {
            showAlert(Alert.AlertType.ERROR, "❌ Validation Error", "Order Mode is required");
            return false;
        }

        if (boxStatus.getValue() == null) {
            showAlert(Alert.AlertType.ERROR, "❌ Validation Error", "Status is required");
            return false;
        }

        return true;
    }

    private void clearForm() {
        txtPrice.clear();
        txtQuantity.clear();
        boxType.setValue(null);
        boxOrder.setValue(null);
        boxStatus.setValue(null);
    }

    private void refreshTableView() {
        table.getItems().clear();
        table.getItems().addAll(allTrades);
    }

    private void updateStatusLabel() {
        int displayed = table.getItems().size();
        int total = allTrades.size();

        if (total == 0) {
            labelStatus.setText("📈 No trades yet");
        } else {
            labelStatus.setText("📈 Showing " + displayed + " of " + total);
        }
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }


    public void setCurrentUser(String username, int userId) {
        this.currentUsername = username;
        this.currentUserId = userId;
        if (labelUser != null) {
            labelUser.setText("👤 " + username);
        }
    }
}