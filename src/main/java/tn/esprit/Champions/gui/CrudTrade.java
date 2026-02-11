package tn.esprit.Champions.gui;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import tn.esprit.Champions.models.*;
import tn.esprit.Champions.services.TradeService;

import java.sql.SQLException;
import java.time.LocalDateTime;

public class CrudTrade {

    @FXML
    private TableView<Trade> table;

    @FXML
    private TableColumn<Trade, TradeType> type;

    @FXML
    private TableColumn<Trade, OrderMode> order;

    @FXML
    private TableColumn<Trade, Double> price;

    @FXML
    private TableColumn<Trade, Double> quantity;

    @FXML
    private TableColumn<Trade, Status> status;

    @FXML
    private TableColumn<Trade, LocalDateTime> created;

    @FXML
    private TableColumn<Trade, LocalDateTime> executed;

    @FXML
    private TableColumn<Trade, Void> actions;

    @FXML
    private TextField txtSearch;

    @FXML
    private Label labelSearch;

    private TradeService tradeService = new TradeService();

    @FXML
    public void initialize() {
        initColumns();
        loadTrades();
        addActionButtons();
        setupSearch();
    }

    private void initColumns() {
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
            table.getItems().clear();
            table.getItems().addAll(tradeService.SelectAll());
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de charger les trades: " + e.getMessage());
        }
    }

    private void addActionButtons() {
        actions.setCellFactory(param -> new TableCell<>() {

            private final Button btnEdit = new Button("✏️");
            private final Button btnDelete = new Button("🗑");

            {
                btnEdit.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");
                btnDelete.setStyle("-fx-background-color: #F44336; -fx-text-fill: white;");

                btnEdit.setOnAction(event -> {
                    Trade trade = getTableView().getItems().get(getIndex());
                    openEditDialog(trade);
                });

                btnDelete.setOnAction(event -> {
                    Trade trade = getTableView().getItems().get(getIndex());
                    deleteTrade(trade);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);

                if (empty) {
                    setGraphic(null);
                } else {
                    HBox box = new HBox(10, btnEdit, btnDelete);
                    setGraphic(box);
                }
            }
        });
    }

    private void setupSearch() {
        txtSearch.textProperty().addListener((observable, oldValue, newValue) -> {
            filterTrades(newValue);
        });
    }

    private void filterTrades(String searchText) {
        try {
            table.getItems().clear();

            if (searchText == null || searchText.trim().isEmpty()) {
                // Si la recherche est vide, afficher tous les trades
                table.getItems().addAll(tradeService.SelectAll());
            } else {
                // Filtrer les trades selon le texte de recherche
                String search = searchText.toLowerCase();
                tradeService.SelectAll().stream()
                        .filter(trade ->
                                trade.getTradeType().name().toLowerCase().contains(search) ||
                                        trade.getOrderMode().name().toLowerCase().contains(search) ||
                                        trade.getStatus().name().toLowerCase().contains(search) ||
                                        String.valueOf(trade.getPrice()).contains(search) ||
                                        String.valueOf(trade.getQuantity()).contains(search)
                        )
                        .forEach(table.getItems()::add);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void deleteTrade(Trade trade) {
        try {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Confirmation");
            alert.setHeaderText(null);
            alert.setContentText("Supprimer ce trade ?");

            if (alert.showAndWait().get() == ButtonType.OK) {
                tradeService.deleteOne(trade);
                table.getItems().remove(trade);
                showAlert(Alert.AlertType.INFORMATION, "Succès", "Trade supprimé avec succès");
            }

        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", e.getMessage());
        }
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void openEditDialog(Trade trade) {
        Dialog<Trade> dialog = new Dialog<>();
        dialog.setTitle("Modifier Trade");
        dialog.setHeaderText("Modifier les informations du trade");

        ButtonType okButtonType = new ButtonType("Modifier", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(okButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField txtPrice = new TextField(String.valueOf(trade.getPrice()));
        TextField txtQuantity = new TextField(String.valueOf(trade.getQuantity()));

        ComboBox<TradeType> boxType = new ComboBox<>();
        boxType.getItems().setAll(TradeType.values());
        boxType.setValue(trade.getTradeType());

        ComboBox<OrderMode> boxOrder = new ComboBox<>();
        boxOrder.getItems().setAll(OrderMode.values());
        boxOrder.setValue(trade.getOrderMode());

        ComboBox<Status> boxStatus = new ComboBox<>();
        boxStatus.getItems().setAll(Status.values());
        boxStatus.setValue(trade.getStatus());

        grid.add(new Label("Trade Type:"), 0, 0);
        grid.add(boxType, 1, 0);
        grid.add(new Label("Order Mode:"), 0, 1);
        grid.add(boxOrder, 1, 1);
        grid.add(new Label("Price:"), 0, 2);
        grid.add(txtPrice, 1, 2);
        grid.add(new Label("Quantity:"), 0, 3);
        grid.add(txtQuantity, 1, 3);
        grid.add(new Label("Status:"), 0, 4);
        grid.add(boxStatus, 1, 4);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == okButtonType) {
                trade.setTradeType(boxType.getValue());
                trade.setOrderMode(boxOrder.getValue());
                trade.setPrice(Double.parseDouble(txtPrice.getText()));
                trade.setQuantity(Double.parseDouble(txtQuantity.getText()));
                trade.setStatus(boxStatus.getValue());
                trade.setExecutedAt(LocalDateTime.now());
                return trade;
            }
            return null;
        });

        dialog.showAndWait().ifPresent(updatedTrade -> {
            try {
                tradeService.updateOne(updatedTrade);
                table.refresh();
                showAlert(Alert.AlertType.INFORMATION, "Succès", "Trade modifié avec succès");
            } catch (Exception e) {
                e.printStackTrace();
                showAlert(Alert.AlertType.ERROR, "Erreur", e.getMessage());
            }
        });
    }
}