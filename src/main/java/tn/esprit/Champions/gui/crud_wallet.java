package tn.esprit.Champions.gui;

import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.PaymentIntent;
import com.stripe.model.PaymentMethod;
import com.stripe.param.*;
import javafx.animation.RotateTransition;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.PieChart;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.transform.Rotate;
import javafx.stage.Stage;
import javafx.util.Duration;
import tn.esprit.Champions.models.*;
import tn.esprit.Champions.services.*;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.math.BigDecimal;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import tn.esprit.Champions.models.Conversion;
import tn.esprit.Champions.services.ConversionService;
import tn.esprit.Champions.utils.UserSession;


public class crud_wallet {


    @FXML
    private ComboBox<String> boxType;
    @FXML
    private ComboBox<String> boxStatus;
    @FXML
    private Button add_wallet;
    @FXML
    private Button modify_wallet;
    @FXML
    private Button delete_wallet;
    @FXML
    private Button clearSearchButton;
    @FXML
    private HBox walletContainer;   // Fiat
    @FXML
    private HBox walletContainer1;  // Crypto
    @FXML
    private HBox walletContainer2;  // Trading
    @FXML
    private TextField searchField;
    @FXML
    private TextField walletIdField;
    @FXML
    private ComboBox<currency> currencyComboBox;
    @FXML private VBox walletForm;
    @FXML private VBox transactionForm;
    @FXML private StackPane flipContainer;
    @FXML private ComboBox<String> walletSourceBox;
    @FXML private ComboBox<String> walletDestBox;

    @FXML private TextField sourceWalletField;
    @FXML private TextField destinationWalletField;
    @FXML private TextField amountField;
    @FXML private ComboBox<String> typeTransactionBox;
    @FXML private ComboBox<String> statusTransactionBox;
    @FXML private ComboBox<String> currencyTransactionBox;
    @FXML private Button addTransactionBtn;
    @FXML
    private Button btnSignOut;
    @FXML
    private PieChart currencyChart;
    @FXML private VBox cardForm;
    @FXML private TextField cardHolderField, cardNumberField, expMonthField, expYearField;
    @FXML private Label nom;
    @FXML private Label rib;
    @FXML
    private Label cardErrorLabel;
    @FXML
    private TextField convAmountField;

    @FXML
    private ComboBox<String> convFromBox;

    @FXML
    private ComboBox<String> convToBox;

    @FXML
    private TextField convResultField;

    @FXML
    private Button btnMarketplace;
    @FXML
    private Button btnFormation;
    @FXML
    private Button btnCredit;

    private VBox selectedCard = null;

    @FXML
    private void flipToTransaction() {
        walletForm.setVisible(false);
        transactionForm.setVisible(true);
    }

    @FXML
    private void flipToWallet() {
        transactionForm.setVisible(false);
        walletForm.setVisible(true);
    }

    private WalletService walletService;
    private wallet_currencyService walletCurrencyService;
    private CurrencyService currencyService;

    private wallet selectedWallet;
    private transaction selectedTransaction;
    private double oldAmount = 0;

    // ✅ Méthode dynamique pour récupérer l'ID du user connecté
    private int getCurrentUserId() {
        Utilisateur user = UserSession.getLoggedInUser();
        if (user == null) {
            System.err.println("ERROR: No user logged in!");
            return -1;
        }
        return user.getId_user();
    }

    @FXML
    public void initialize() {

        boxType.getItems().addAll("fiat", "crypto", "trading");
        boxStatus.getItems().addAll("actif", "bloque");

        walletService = new WalletService();
        walletCurrencyService = new wallet_currencyService();
        currencyService = new CurrencyService();

        loadCurrencies();
        loadWallets();
        loadWalletRIBs();
        resetConversionFields();

        if (searchField != null) {
            searchField.setOnKeyReleased(this::handleSearch);
        }
        if (clearSearchButton != null) {
            clearSearchButton.setOnAction(e -> handleClearSearch());
        }

        if (modify_wallet != null) {
            modify_wallet.setOnAction(e -> handleModifyWallet());
        }
        if (delete_wallet != null) {
            delete_wallet.setOnAction(e -> handleDeleteWallet());
        }

        if (btnSignOut != null) {
            btnSignOut.setOnAction(event -> {
                try {
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/DashboardWalletClient.fxml"));
                    Parent root = loader.load();
                    Stage stage = (Stage) btnSignOut.getScene().getWindow();
                    stage.setScene(new Scene(root));
                    stage.show();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            updateGlobalCurrencyChart();
            loadCard();
        }

        Platform.runLater(() -> {
            Scene scene = walletContainer.getScene();
            if (scene != null) {
                scene.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
                    Node target = (Node) event.getTarget();
                    if (!isWalletCard(target) && !isWalletForm(target)) {
                        clearWalletSelection();
                        clearTransactionForm();
                    }
                });
            }
        });
    }

    private boolean isWalletForm(Node node) {
        while (node != null) {
            if (node == walletForm || node == transactionForm) return true;
            node = node.getParent();
        }
        return false;
    }

    private void clearTransactionForm() {
        if (sourceWalletField != null) {
            sourceWalletField.clear();
            sourceWalletField.setDisable(false);
        }
        if (destinationWalletField != null) {
            destinationWalletField.clear();
            destinationWalletField.setDisable(false);
        }
        if (amountField != null) {
            amountField.clear();
        }
        if (typeTransactionBox != null) {
            typeTransactionBox.getSelectionModel().clearSelection();
        }
        if (statusTransactionBox != null) {
            statusTransactionBox.getSelectionModel().clearSelection();
        }
        if (currencyTransactionBox != null) {
            currencyTransactionBox.getSelectionModel().clearSelection();
            currencyTransactionBox.setDisable(false);
        }
        selectedTransaction = null;
    }

    private void clearWalletSelection() {
        if (selectedCard != null) {
            selectedCard.setStyle(
                    "-fx-background-color: linear-gradient(to bottom right, #ffffff, #e8e8e8);" +
                            "-fx-background-radius: 20;" +
                            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 8,0,0,3);"
            );
            selectedCard = null;
        }
        boxType.setDisable(false);
        walletIdField.clear();
        boxType.getSelectionModel().clearSelection();
        boxStatus.getSelectionModel().clearSelection();
        currencyComboBox.getSelectionModel().clearSelection();
    }

    @FXML
    private void handleAddWallet() {
        String typeStr = boxType.getValue();
        String statusStr = boxStatus.getValue();

        if (typeStr == null || statusStr == null) {
            new Alert(Alert.AlertType.WARNING, "Veuillez sélectionner Type et Statut !").show();
            return;
        }

        // ✅ User dynamique
        int userId = getCurrentUserId();
        if (userId <= 0) {
            new Alert(Alert.AlertType.ERROR, "Utilisateur non connecté !").show();
            return;
        }

        wallet wallet = new wallet();
        wallet.setTypeWallet(typeWallet.valueOf(typeStr));
        wallet.setStatut(statutWallet.valueOf(statusStr));
        wallet.setIdUser(userId); // ✅ dynamique

        try {
            walletService.insertOne(wallet);
            loadWallets();
            boxType.setValue(null);
            boxStatus.setValue(null);
        } catch (SQLException e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Erreur lors de l'ajout du wallet !").show();
        }
    }

    @FXML
    private void handleModifyWallet() {
        if (selectedWallet == null) {
            new Alert(Alert.AlertType.WARNING, "Veuillez sélectionner un wallet !").show();
            return;
        }

        String statutStr = boxStatus.getValue();
        if (statutStr == null) {
            new Alert(Alert.AlertType.WARNING, "Veuillez sélectionner un statut !").show();
            return;
        }

        selectedWallet.setStatut(statutWallet.valueOf(statutStr));

        try {
            walletService.updateOne(selectedWallet);
            loadWallets();
            new Alert(Alert.AlertType.INFORMATION, "Wallet mis à jour !").show();
        } catch (SQLException e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Erreur lors de la mise à jour !").show();
        }
    }

    @FXML
    private void handleDeleteWallet() {
        if (selectedWallet == null) {
            new Alert(Alert.AlertType.WARNING, "Veuillez sélectionner un wallet à supprimer !").show();
            return;
        }

        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION,
                "Voulez-vous vraiment supprimer ce wallet ?", ButtonType.YES, ButtonType.NO);
        confirmAlert.setHeaderText(null);
        confirmAlert.showAndWait();

        if (confirmAlert.getResult() == ButtonType.YES) {
            try {
                List<wallet_currency> currencies = walletCurrencyService.getCurrenciesByWallet(selectedWallet.getIdWallet());
                boolean hasBalance = currencies.stream().anyMatch(c -> c.getSolde() > 0);
                if (hasBalance) {
                    new Alert(Alert.AlertType.ERROR, "Impossible de supprimer : certaines currencies ont encore des soldes !").show();
                    return;
                }

                walletService.deleteOne(selectedWallet);
                selectedWallet = null;
                selectedCard = null;
                loadWallets();
                boxType.setValue(null);
                boxStatus.setValue(null);
                new Alert(Alert.AlertType.INFORMATION, "Wallet supprimé avec succès !").show();

            } catch (SQLException e) {
                e.printStackTrace();
                new Alert(Alert.AlertType.ERROR, "Erreur lors de la suppression !").show();
            }
        }
    }

    @FXML
    private void handleAddWalletCurrency() {
        if (selectedWallet == null) {
            new Alert(Alert.AlertType.WARNING, "Veuillez sélectionner un wallet !").show();
            return;
        }

        currency selectedCurrency = currencyComboBox.getValue();
        if (selectedCurrency == null) {
            new Alert(Alert.AlertType.WARNING, "Veuillez sélectionner une currency !").show();
            return;
        }

        try {
            wallet_currency wc = new wallet_currency();
            wc.setId_wallet(selectedWallet.getIdWallet());
            wc.setNom_currency(selectedCurrency.getNom());

            walletCurrencyService.insertOne(wc);

            new Alert(Alert.AlertType.INFORMATION, "Currency ajoutée au wallet avec succès !").show();

            loadWallets();

        } catch (SQLException e) {
            new Alert(Alert.AlertType.ERROR, e.getMessage()).show();
            e.printStackTrace();
        }
    }

    @FXML
    private void handleDeleteWalletCurrency() {
        if (selectedWallet == null) {
            new Alert(Alert.AlertType.WARNING, "Veuillez sélectionner un wallet !").show();
            return;
        }

        currency selectedCurrency = currencyComboBox.getValue();
        if (selectedCurrency == null) {
            new Alert(Alert.AlertType.WARNING, "Veuillez sélectionner une currency à supprimer !").show();
            return;
        }

        try {
            wallet_currency wcToDelete = new wallet_currency();
            wcToDelete.setId_wallet(selectedWallet.getIdWallet());
            wcToDelete.setId_currency(selectedCurrency.getId_currency());
            wcToDelete.setNom_currency(selectedCurrency.getNom());

            walletCurrencyService.deleteOne(wcToDelete);

            new Alert(Alert.AlertType.INFORMATION, "Currency supprimée du wallet avec succès !").show();

            loadWallets();

        } catch (SQLException e) {
            new Alert(Alert.AlertType.ERROR, e.getMessage()).show();
            e.printStackTrace();
        }
    }

    @FXML
    private void handleAddTransaction() {
        try {
            String ribSource = sourceWalletField.getText().trim();
            String ribDest = destinationWalletField.getText().trim();
            String currencyName = currencyTransactionBox.getValue();
            double montant = Double.parseDouble(amountField.getText().trim());

            if (ribSource.isEmpty() || ribDest.isEmpty()) {
                new Alert(Alert.AlertType.WARNING, "Veuillez entrer les RIB !").show();
                return;
            }
            if (currencyName == null || currencyName.isEmpty()) {
                new Alert(Alert.AlertType.WARNING, "Veuillez sélectionner une currency !").show();
                return;
            }
            if (montant <= 0) {
                new Alert(Alert.AlertType.WARNING, "Le montant doit être supérieur à 0 !").show();
                return;
            }

            WalletService walletService = new WalletService();

            wallet sourceWallet = walletService.getByRib(ribSource);
            wallet destWallet = walletService.getByRib(ribDest);

            if (sourceWallet == null || destWallet == null) {
                new Alert(Alert.AlertType.ERROR, "Wallet introuvable !").show();
                return;
            }

            if (sourceWallet.getIdWallet() == destWallet.getIdWallet()) {
                new Alert(Alert.AlertType.WARNING,
                        "Vous ne pouvez pas transférer vers le même wallet !").show();
                return;
            }

            int idCurrency = walletCurrencyService.getCurrencyIdByName(currencyName);
            if (idCurrency == 0) {
                new Alert(Alert.AlertType.ERROR, "Currency introuvable !").show();
                return;
            }

            wallet_currency sourceCurrency = walletCurrencyService
                    .getWalletCurrencyByWalletAndId(sourceWallet.getIdWallet(), idCurrency);
            if (sourceCurrency == null) {
                new Alert(Alert.AlertType.WARNING,
                        "Le wallet source ne contient pas cette currency !").show();
                return;
            }
            if (sourceCurrency.getSolde() < montant) {
                new Alert(Alert.AlertType.WARNING,
                        "Solde insuffisant dans cette currency !").show();
                return;
            }

            transaction t = new transaction();
            t.setIdWalletSource(sourceWallet.getIdWallet());
            t.setIdWalletDestination(destWallet.getIdWallet());
            t.setMontant(montant);
            t.setCurrencyId(idCurrency);
            t.setDateTransaction(java.time.LocalDateTime.now());
            t.setType(typeTransaction.TRANSFERT);
            t.setStatut(StatutTransaction.Completed);

            TransactionService transactionService = new TransactionService();
            transactionService.insertOne(t);

            loadWallets();

            new Alert(Alert.AlertType.INFORMATION, "Transaction effectuée avec succès !").show();

            sourceWalletField.clear();
            destinationWalletField.clear();
            amountField.clear();
            currencyTransactionBox.getSelectionModel().clearSelection();

        } catch (NumberFormatException e) {
            new Alert(Alert.AlertType.WARNING, "Veuillez entrer un montant valide !").show();
        } catch (SQLException e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Erreur lors de la transaction : " + e.getMessage()).show();
        }
    }

    @FXML
    private void handleUpdateTransaction() {
        try {
            if (selectedTransaction == null) {
                new Alert(Alert.AlertType.WARNING, "Veuillez sélectionner une transaction !").show();
                return;
            }

            double newAmount = Double.parseDouble(amountField.getText().trim());
            if (newAmount <= 0) {
                new Alert(Alert.AlertType.WARNING, "Montant invalide !").show();
                return;
            }

            int sourceId = selectedTransaction.getIdWalletSource();
            int destId = selectedTransaction.getIdWalletDestination();
            int currencyId = selectedTransaction.getCurrencyId();

            double sourceBalance = walletCurrencyService.getBalance(sourceId, currencyId);
            double destBalance = walletCurrencyService.getBalance(destId, currencyId);

            double difference = newAmount - oldAmount;

            if (difference > 0) {
                if (sourceBalance < difference) {
                    new Alert(Alert.AlertType.ERROR, "Solde insuffisant dans le wallet source !").show();
                    return;
                }
                sourceBalance -= difference;
                destBalance += difference;
            } else {
                difference = Math.abs(difference);
                destBalance -= difference;
                sourceBalance += difference;
            }

            selectedTransaction.setMontant(newAmount);
            selectedTransaction.setDateTransaction(LocalDateTime.now());

            TransactionService transactionService = new TransactionService();
            transactionService.updateOne(selectedTransaction);

            loadWallets();

            new Alert(Alert.AlertType.INFORMATION, "Transaction mise à jour avec succès !").show();

            clearTransactionForm();
            selectedTransaction = null;

        } catch (NumberFormatException e) {
            new Alert(Alert.AlertType.WARNING, "Montant invalide !").show();
        } catch (SQLException e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Erreur : " + e.getMessage()).show();
        }
    }

    private void updateWalletBalanceDisplay(int walletId) {
        List<wallet_currency> currencies;
        try {
            currencies = walletCurrencyService.getCurrenciesByWallet(walletId);
        } catch (SQLException e) {
            e.printStackTrace();
            return;
        }

        for (HBox container : List.of(walletContainer, walletContainer1, walletContainer2)) {
            for (Node node : container.getChildren()) {
                if (node instanceof VBox card) {
                    Label lblName = (Label) card.getChildren().get(1);
                    if (lblName.getText().contains(String.valueOf(walletId))
                            || lblName.getText().contains(selectedWallet != null ? selectedWallet.getRib() : "")) {
                        VBox currencyBox = (VBox) card.getChildren().get(3);
                        for (Node lineNode : currencyBox.getChildren()) {
                            if (lineNode instanceof HBox line) {
                                if (line.getChildren().size() < 2) continue;
                                Label nameLabel = (Label) line.getChildren().get(0);
                                Label soldeLabel = (Label) line.getChildren().get(1);
                                String currencyName = nameLabel.getText();
                                currencies.stream()
                                        .filter(wc -> {
                                            try {
                                                String dbName = currencyService.getCurrencyNameById(wc.getId_currency());
                                                return dbName.equals(currencyName);
                                            } catch (SQLException ex) {
                                                return false;
                                            }
                                        })
                                        .findFirst()
                                        .ifPresent(wc -> soldeLabel.setText(String.format("%.2f", wc.getSolde())));
                            }
                        }
                    }
                }
            }
        }
    }

    private void showWalletDetails(wallet w, VBox card) {
        selectedWallet = w;

        if (selectedCard != null) {
            boxType.setDisable(true);
            selectedCard.setStyle(
                    "-fx-background-color: linear-gradient(to bottom right, #ffffff, #e8e8e8);" +
                            "-fx-background-radius: 20;" +
                            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 8,0,0,3);"
            );
        }

        selectedCard = card;
        selectedCard.setStyle(
                "-fx-background-color: #d3d3d3;" +
                        "-fx-background-radius: 20;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 8,0,0,3);"
        );

        if (w.getTypeWallet() != null) boxType.setValue(w.getTypeWallet().name());
        if (w.getStatut() != null) boxStatus.setValue(w.getStatut().name());
        boxType.setDisable(true);

        if (walletIdField != null) {
            walletIdField.setText(w.getRib());
        }

        if (currencyComboBox != null) {
            try {
                List<currency> allCurrencies = currencyService.SelectAll();
                List<currency> filteredCurrencies = allCurrencies.stream()
                        .filter(c -> {
                            if (w.getTypeWallet() == typeWallet.trading) return c.getType_currency() == typeCurrency.crypto && c.isIs_trading();
                            else if (w.getTypeWallet() == typeWallet.crypto) return c.getType_currency() == typeCurrency.crypto;
                            else if (w.getTypeWallet() == typeWallet.fiat) return c.getType_currency() == typeCurrency.fiat;
                            return false;
                        })
                        .collect(Collectors.toList());

                currencyComboBox.setItems(FXCollections.observableArrayList(filteredCurrencies));
                currencyComboBox.setPromptText("Sélectionner currency");

                currencyComboBox.setCellFactory(c -> new ListCell<>() {
                    @Override
                    protected void updateItem(currency item, boolean empty) {
                        super.updateItem(item, empty);
                        setText(empty || item == null ? "" : item.getNom());
                    }
                });
                currencyComboBox.setButtonCell(new ListCell<>() {
                    @Override
                    protected void updateItem(currency item, boolean empty) {
                        super.updateItem(item, empty);
                        setText(empty || item == null ? "" : item.getNom());
                    }
                });
            } catch (SQLException e) {
                e.printStackTrace();
                new Alert(Alert.AlertType.ERROR, "Erreur lors du chargement des currencies !").show();
            }
        }

        if (sourceWalletField != null) {
            sourceWalletField.setText(w.getRib());
            sourceWalletField.setEditable(false);
        }

        if (currencyTransactionBox != null) {
            try {
                List<wallet_currency> walletCurrencies = walletCurrencyService.getCurrenciesByWallet(w.getIdWallet());
                List<currency> allCurrencies = currencyService.SelectAll();
                Map<String, currency> currencyMap = allCurrencies.stream()
                        .collect(Collectors.toMap(currency::getNom, c -> c));

                walletCurrencies = walletCurrencies.stream()
                        .filter(wc -> {
                            currency c = currencyMap.get(wc.getNom_currency());
                            return c != null && (w.getTypeWallet() != typeWallet.trading || c.isIs_trading());
                        })
                        .toList();

                List<String> currencyNames = walletCurrencies.stream()
                        .map(wallet_currency::getNom_currency)
                        .toList();

                currencyTransactionBox.getItems().clear();
                currencyTransactionBox.getItems().addAll(currencyNames);
                currencyTransactionBox.setPromptText("Sélectionner currency");

            } catch (SQLException e) {
                e.printStackTrace();
                new Alert(Alert.AlertType.ERROR, "Erreur lors du chargement des currencies pour la transaction !").show();
            }
        }
    }

    // ------------------------ Recherche Wallet ------------------------
    @FXML
    private void handleSearch(KeyEvent event) {
        if (searchField == null) return;

        String filter = searchField.getText().toLowerCase();
        // ✅ Filtrer aussi par user connecté
        int userId = getCurrentUserId();

        try {
            List<wallet> wallets = walletService.SelectAll().stream()
                    .filter(w -> w.getIdUser() == userId) // ✅ uniquement ses wallets
                    .filter(w -> String.valueOf(w.getIdWallet()).contains(filter)
                            || w.getTypeWallet().name().toLowerCase().contains(filter)
                            || w.getStatut().name().toLowerCase().contains(filter))
                    .collect(Collectors.toList());

            displayWallets(wallets);
        } catch (SQLException e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Erreur lors de la recherche !").show();
        }
    }

    @FXML
    private void handleClearSearch() {
        if (searchField != null) searchField.clear();
        loadWallets();
    }

    // ------------------------ Chargement Wallets — filtré par user connecté ------------------------
    private void loadWallets() {
        try {
            int userId = getCurrentUserId();
            if (userId <= 0) {
                new Alert(Alert.AlertType.WARNING, "Utilisateur non connecté !").show();
                return;
            }

            // ✅ Uniquement les wallets du user connecté
            List<wallet> wallets = walletService.SelectAll().stream()
                    .filter(w -> w.getIdUser() == userId)
                    .collect(Collectors.toList());

            displayWallets(wallets);

        } catch (SQLException e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Erreur lors du chargement des wallets !").show();
        }
    }

    // ------------------------ Affichage Wallets ------------------------
    private void displayWallets(List<wallet> wallets) {
        walletContainer.getChildren().clear();
        walletContainer1.getChildren().clear();
        walletContainer2.getChildren().clear();

        for (wallet w : wallets) {
            VBox card = createWalletCard(w);
            card.setOnMouseClicked(event -> showWalletDetails(w, card));

            if (w.getTypeWallet() == typeWallet.fiat) walletContainer.getChildren().add(card);
            else if (w.getTypeWallet() == typeWallet.crypto) walletContainer1.getChildren().add(card);
            else if (w.getTypeWallet() == typeWallet.trading) walletContainer2.getChildren().add(card);
        }
    }

    private void showExpandedWallet(wallet w, List<wallet_currency> currencies) {
        Stage stage = new Stage();
        stage.setTitle("Wallet #" + w.getIdWallet());

        VBox root = new VBox();
        root.setSpacing(15);
        root.setPadding(new Insets(20));
        root.setStyle(
                "-fx-background-color: linear-gradient(to bottom right, #ffffff, #e8e8e8);" +
                        "-fx-background-radius: 20;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 8,0,0,3);"
        );

        Label title = new Label("Wallet #" + w.getIdWallet());
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #1a5f7a;");
        title.setAlignment(Pos.CENTER);
        title.setMaxWidth(Double.MAX_VALUE);

        String typeText = w.getTypeWallet() != null ? w.getTypeWallet().name() : "";
        String statutText = w.getStatut() != null ? w.getStatut().name() : "";
        Label lblInfo = new Label(typeText + " • " + statutText);
        lblInfo.setStyle(switch (statutText.toLowerCase()) {
            case "bloque" -> "-fx-text-fill: red; -fx-font-size: 13px;";
            case "actif" -> "-fx-text-fill: green; -fx-font-size: 13px;";
            default -> "-fx-text-fill: #6e6e6e; -fx-font-size: 13px;";
        });
        lblInfo.setAlignment(Pos.CENTER);
        lblInfo.setMaxWidth(Double.MAX_VALUE);

        VBox currencyBox = new VBox();
        currencyBox.setSpacing(10);
        currencyBox.setPadding(new Insets(10));
        currencyBox.setAlignment(Pos.TOP_CENTER);

        for (wallet_currency wc : currencies) {
            HBox line = new HBox();
            line.setSpacing(12);
            line.setAlignment(Pos.CENTER_LEFT);
            line.setPadding(new Insets(10));
            line.setStyle(
                    "-fx-background-color: #ffffff;" +
                            "-fx-background-radius: 15;" +
                            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 4,0,0,2);"
            );

            Label name = new Label(wc.getNom_currency());
            name.setStyle("-fx-text-fill: #2c2c2c; -fx-font-size: 14px; -fx-font-weight: bold;");
            HBox.setHgrow(name, Priority.ALWAYS);

            Label solde = new Label(String.format("%.2f", wc.getSolde()));
            solde.setStyle("-fx-text-fill: #4a4a4a; -fx-font-size: 14px;");

            line.getChildren().addAll(name, solde);
            currencyBox.getChildren().add(line);
        }

        ScrollPane scrollPane = new ScrollPane(currencyBox);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefHeight(300);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-insets: 0;");

        root.getChildren().addAll(title, lblInfo, scrollPane);
        Scene scene = new Scene(root, 320, 400);
        stage.setScene(scene);
        stage.show();
    }

    private VBox createWalletCard(wallet w) {

        VBox card = new VBox();
        card.setSpacing(12);
        card.setPrefWidth(280);
        card.setPrefHeight(170);
        card.setPadding(new Insets(15));
        card.setAlignment(Pos.CENTER);
        card.getStyleClass().add("wallet-card");

        String styleInitial = "-fx-background-color: linear-gradient(to bottom right, #ffffff, #e8e8e8);" +
                "-fx-background-radius: 20;" +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 8,0,0,3);";
        String styleSelected = "-fx-background-color: #d3d3d3;" +
                "-fx-background-radius: 20;" +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 10,0,0,5);";

        card.setStyle(styleInitial);

        Label lblTransactions = new Label("💸");
        lblTransactions.setStyle("-fx-font-size: 16px; -fx-text-fill: black; -fx-cursor: hand;");

        Label lblDates = new Label("\uD83D\uDD04");
        lblDates.setStyle("-fx-font-size: 18px; -fx-cursor: hand;");

        HBox topBar = new HBox(10, lblTransactions, lblDates);
        topBar.setAlignment(Pos.TOP_LEFT);
        topBar.setMaxWidth(Double.MAX_VALUE);

        Label lblName = new Label("Wallet RIB: " + w.getRib());
        lblName.setStyle("-fx-text-fill: #1a5f7a; -fx-font-size: 18px; -fx-font-weight: bold;");
        lblName.setAlignment(Pos.CENTER);
        lblName.setMaxWidth(Double.MAX_VALUE);

        String typeText = (w.getTypeWallet() != null ? w.getTypeWallet().name() : "");
        String statutText = (w.getStatut() != null ? w.getStatut().name() : "");

        Label lblInfo = new Label(typeText + " • " + statutText);
        if ("bloque".equalsIgnoreCase(statutText))
            lblInfo.setStyle("-fx-text-fill: red; -fx-font-size: 13px;");
        else if ("actif".equalsIgnoreCase(statutText))
            lblInfo.setStyle("-fx-text-fill: green; -fx-font-size: 13px;");
        else
            lblInfo.setStyle("-fx-text-fill: #6e6e6e; -fx-font-size: 13px;");

        lblInfo.setAlignment(Pos.CENTER);
        lblInfo.setMaxWidth(Double.MAX_VALUE);

        VBox currencyBox = new VBox();
        currencyBox.setSpacing(4);
        currencyBox.setAlignment(Pos.CENTER);

        try {
            List<wallet_currency> currencies =
                    walletCurrencyService.getCurrenciesByWallet(w.getIdWallet());

            if (currencies.isEmpty()) {
                Label empty = new Label("No currencies");
                empty.setStyle("-fx-text-fill: #a0a0a0;");
                currencyBox.getChildren().add(empty);
            } else {
                for (int i = 0; i < Math.min(2, currencies.size()); i++) {
                    wallet_currency wc = currencies.get(i);

                    HBox line = new HBox(10);
                    line.setAlignment(Pos.CENTER);

                    String currencyName;
                    try {
                        currencyName = currencyService.getCurrencyNameById(wc.getId_currency());
                    } catch (SQLException ex) {
                        currencyName = "N/A";
                    }

                    Label name = new Label(currencyName);
                    name.setStyle("-fx-text-fill: #2c2c2c; -fx-font-size: 14px; -fx-font-weight: bold;");

                    Label solde = new Label(String.format("%.2f", wc.getSolde()));
                    solde.setStyle("-fx-text-fill: #4a4a4a; -fx-font-size: 14px;");

                    line.getChildren().addAll(name, solde);
                    currencyBox.getChildren().add(line);
                }

                if (currencies.size() > 2) {
                    Button viewAll = new Button("View all");
                    viewAll.setStyle("-fx-background-color: transparent; -fx-text-fill: #1a5f7a; -fx-font-weight: bold;");
                    viewAll.setOnAction(e -> showExpandedWallet(w, currencies));
                    currencyBox.getChildren().add(viewAll);
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        lblTransactions.setOnMouseClicked(e -> {
            try {
                TransactionService transactionService = new TransactionService();
                CreditCardService creditCardService = new CreditCardService();
                ConversionService conversionService = new ConversionService();
                CurrencyService currencyService = new CurrencyService();

                List<transaction> transactions = transactionService.getTransactionsByWallet(w.getIdWallet());
                int walletId = w.getIdWallet();

                TableView<transaction> table = new TableView<>();

                TableColumn<transaction, String> typeCol = new TableColumn<>("Type");
                typeCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getType().name()));

                TableColumn<transaction, String> sourceCol = new TableColumn<>("Source");
                sourceCol.setCellValueFactory(cell -> {
                    transaction t = cell.getValue();
                    String display = "A/N";
                    if (t.getType() != null) {
                        String typeName = t.getType().name().toUpperCase();
                        if (typeName.equals("TRANSFERT") || typeName.equals("RETRAIT") ||
                                typeName.equals("ACHAT") || typeName.equals("CONVERSION")) {
                            try {
                                display = walletService.SelectById(t.getIdWalletSource()).getRib();
                            } catch (Exception ex) { display = "Erreur RIB"; }
                        } else if (typeName.equals("RECHARGE")) {
                            try {
                                CreditCard c = creditCardService.getCardById(t.getId_card());
                                display = (c != null) ? "**** " + c.getLast4Digits() : "Carte";
                            } catch (Exception ex) { display = "A/N"; }
                        }
                    }
                    return new SimpleStringProperty(display);
                });

                TableColumn<transaction, String> destCol = new TableColumn<>("Destination");
                destCol.setCellValueFactory(cell -> {
                    String display = "N/A";
                    try {
                        display = walletService.SelectById(cell.getValue().getIdWalletDestination()).getRib();
                    } catch (Exception ex) { display = "Inconnu"; }
                    return new SimpleStringProperty(display);
                });

                TableColumn<transaction, String> montantCol = new TableColumn<>("Montant");
                montantCol.setCellValueFactory(cell -> {
                    transaction t = cell.getValue();
                    boolean isConv = t.getType() != null && t.getType().name().equalsIgnoreCase("CONVERSION");
                    if (isConv && t.getId_conversion() > 0) {
                        try {
                            Conversion conv = conversionService.getById(t.getId_conversion());
                            if (conv != null) {
                                return new SimpleStringProperty(conv.getAmountFrom() + " ➔ " + conv.getAmountTo());
                            }
                        } catch (Exception ex) { ex.printStackTrace(); }
                    }
                    return new SimpleStringProperty(String.valueOf(t.getMontant()));
                });

                TableColumn<transaction, String> currencyCol = new TableColumn<>("Currency");
                currencyCol.setCellValueFactory(cell -> {
                    transaction t = cell.getValue();
                    boolean isConv = t.getType() != null && t.getType().name().equalsIgnoreCase("CONVERSION");
                    try {
                        if (isConv && t.getId_conversion() > 0) {
                            Conversion conv = conversionService.getById(t.getId_conversion());
                            if (conv != null) {
                                String from = currencyService.getCurrencyNameById(conv.getCurrencyFrom());
                                String to = currencyService.getCurrencyNameById(conv.getCurrencyTo());
                                return new SimpleStringProperty(from + " ➔ " + to);
                            }
                        }
                        return new SimpleStringProperty(currencyService.getCurrencyNameById(t.getCurrencyId()));
                    } catch (Exception ex) {
                        return new SimpleStringProperty("Err");
                    }
                });

                TableColumn<transaction, LocalDateTime> dateCol = new TableColumn<>("Date");
                dateCol.setCellValueFactory(new PropertyValueFactory<>("dateTransaction"));

                TableColumn<transaction, Void> deleteCol = new TableColumn<>("Delete");
                deleteCol.setCellFactory(col -> new TableCell<>() {
                    private final Label trash = new Label("🗑");
                    {
                        trash.setStyle("-fx-text-fill: red; -fx-cursor: hand;");
                        trash.setOnMouseClicked(ev -> {
                            transaction t = getTableView().getItems().get(getIndex());
                            try {
                                transactionService.deleteOne(t);
                                getTableView().getItems().remove(t);
                            } catch (Exception ex) { ex.printStackTrace(); }
                        });
                    }
                    @Override protected void updateItem(Void item, boolean empty) {
                        super.updateItem(item, empty);
                        setGraphic(empty ? null : trash);
                    }
                });

                table.getColumns().addAll(typeCol, sourceCol, destCol, montantCol, currencyCol, dateCol, deleteCol);

                table.setRowFactory(tv -> new TableRow<transaction>() {
                    @Override
                    protected void updateItem(transaction item, boolean empty) {
                        super.updateItem(item, empty);
                        if (item == null || empty) {
                            setStyle("");
                        } else if (item.getIdWalletSource() == walletId) {
                            setStyle("-fx-background-color: #ffcccc;");
                        } else if (item.getIdWalletDestination() == walletId) {
                            setStyle("-fx-background-color: #ccffcc;");
                        } else {
                            setStyle("");
                        }
                    }
                });

                table.setItems(FXCollections.observableArrayList(transactions));

                Stage stage = new Stage();
                VBox root = new VBox(10, table);
                root.setPadding(new Insets(10));
                VBox.setVgrow(table, Priority.ALWAYS);
                stage.setScene(new Scene(root, 950, 500));
                stage.setTitle("Transactions du Wallet: " + w.getRib());
                stage.show();

            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        lblDates.setOnMouseClicked(e -> {
            try {
                WalletService walletService = new WalletService();
                wallet wUpdated = walletService.SelectById(w.getIdWallet());

                RotateTransition rotateOut = new RotateTransition(Duration.millis(300), card);
                rotateOut.setAxis(Rotate.Y_AXIS);
                rotateOut.setFromAngle(0);
                rotateOut.setToAngle(90);
                rotateOut.setOnFinished(ev -> {

                    Label lblBack = new Label("\uD83D\uDD04");
                    lblBack.setStyle("-fx-font-size: 20px; -fx-cursor: hand;");

                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMMM yyyy - HH:mm");

                    Label lblCreationTitle = new Label("Date de création :");
                    lblCreationTitle.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #1a5f7a;");
                    Label lblCreationValue = new Label(
                            wUpdated.getDateCreation() != null ? wUpdated.getDateCreation().format(formatter) : "N/A");
                    lblCreationValue.setStyle("-fx-font-size: 16px; -fx-text-fill: #1a5f7a;");
                    VBox creationBox = new VBox(2, lblCreationTitle, lblCreationValue);
                    creationBox.setAlignment(Pos.CENTER);

                    Label lblModifTitle = new Label("Dernière modification :");
                    lblModifTitle.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #1a5f7a;");
                    Label lblModifValue = new Label(
                            wUpdated.getDateDerniereModification() != null ? wUpdated.getDateDerniereModification().format(formatter) : "N/A");
                    lblModifValue.setStyle("-fx-font-size: 16px; -fx-text-fill: #1a5f7a;");
                    VBox modifBox = new VBox(2, lblModifTitle, lblModifValue);
                    modifBox.setAlignment(Pos.CENTER);

                    Button btnRecharge = null;

                    if (wUpdated.getTypeWallet() == typeWallet.fiat
                            && wUpdated.getStatut() == statutWallet.actif) {

                        btnRecharge = new Button("💰 Recharger");
                        btnRecharge.setPrefWidth(145);
                        btnRecharge.setPrefHeight(35);
                        btnRecharge.setStyle(
                                "-fx-background-color: #10b981;" +
                                        "-fx-background-radius: 5;" +
                                        "-fx-text-fill: white;" +
                                        "-fx-cursor: hand;" +
                                        "-fx-font-weight: bold;"
                        );
                        btnRecharge.setOnAction(ev2 -> openRechargeForm(w));
                    }

                    VBox verso;
                    if (btnRecharge != null) {
                        verso = new VBox(15, lblBack, creationBox, modifBox, btnRecharge);
                    } else {
                        verso = new VBox(15, lblBack, creationBox, modifBox);
                    }
                    verso.setAlignment(Pos.TOP_CENTER);
                    verso.setPadding(new Insets(20));
                    card.getChildren().clear();
                    card.getChildren().add(verso);

                    RotateTransition rotateIn = new RotateTransition(Duration.millis(300), card);
                    rotateIn.setAxis(Rotate.Y_AXIS);
                    rotateIn.setFromAngle(90);
                    rotateIn.setToAngle(0);
                    rotateIn.play();

                    lblBack.setOnMouseClicked(ev2 -> {
                        RotateTransition rotateOutBack = new RotateTransition(Duration.millis(300), card);
                        rotateOutBack.setAxis(Rotate.Y_AXIS);
                        rotateOutBack.setFromAngle(0);
                        rotateOutBack.setToAngle(90);
                        rotateOutBack.setOnFinished(ev3 -> {
                            card.getChildren().clear();
                            HBox topBarOriginal = new HBox(10, lblTransactions, lblDates);
                            topBarOriginal.setAlignment(Pos.TOP_LEFT);
                            topBarOriginal.setMaxWidth(Double.MAX_VALUE);
                            card.getChildren().addAll(topBarOriginal, lblName, lblInfo, currencyBox);

                            RotateTransition rotateInBack = new RotateTransition(Duration.millis(300), card);
                            rotateInBack.setAxis(Rotate.Y_AXIS);
                            rotateInBack.setFromAngle(90);
                            rotateInBack.setToAngle(0);
                            rotateInBack.play();
                        });
                        rotateOutBack.play();
                    });
                });
                rotateOut.play();

            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        card.setOnMouseClicked(e -> {
            if (selectedCard != null && selectedCard != card) {
                selectedCard.setStyle(styleInitial);
            }
            selectedCard = card;
            card.setStyle(styleSelected);
            showWalletDetails(w, card);
            e.consume();
        });

        card.getChildren().addAll(topBar, lblName, lblInfo, currencyBox);
        return card;
    }

    private boolean isWalletCard(Node node) {
        while (node != null) {
            if (node.getStyleClass().contains("wallet-card")) return true;
            node = node.getParent();
        }
        return false;
    }

    // ✅ PieChart filtré par user connecté uniquement
    private void updateGlobalCurrencyChart() {
        try {
            int userId = getCurrentUserId();
            if (userId <= 0) return;

            // Récupérer les wallets du user connecté
            List<Integer> userWalletIds = walletService.SelectAll().stream()
                    .filter(w -> w.getIdUser() == userId)
                    .map(wallet::getIdWallet)
                    .collect(Collectors.toList());

            // Récupérer uniquement les wallet_currency appartenant aux wallets du user
            List<wallet_currency> userWalletCurrencies = walletCurrencyService.SelectAll().stream()
                    .filter(wc -> userWalletIds.contains(wc.getId_wallet()))
                    .collect(Collectors.toList());

            if (userWalletCurrencies == null || userWalletCurrencies.isEmpty()) {
                System.out.println("Aucune donnée de devise trouvée pour cet utilisateur.");
                return;
            }

            // Regrouper par currency et sommer les soldes
            Map<String, Double> totalsByCurrency = userWalletCurrencies.stream()
                    .collect(Collectors.groupingBy(
                            wallet_currency::getNom_currency,
                            Collectors.summingDouble(wallet_currency::getSolde)
                    ));

            ObservableList<PieChart.Data> pieChartData = FXCollections.observableArrayList();

            totalsByCurrency.forEach((name, total) -> {
                if (total > 0) {
                    pieChartData.add(new PieChart.Data(name + " (" + String.format("%.2f", total) + ")", total));
                }
            });

            if (currencyChart != null) {
                Platform.runLater(() -> currencyChart.setData(pieChartData));
            }

        } catch (Exception e) {
            System.err.println("Erreur lors de la mise à jour du graphique : " + e.getMessage());
            e.printStackTrace();
        }
    }

    private CreditCardService cardService = new CreditCardService();

    private void setErrorStyle(TextField field) {
        field.setStyle("-fx-border-color: red; -fx-border-width: 2; -fx-border-radius: 5;");
    }

    private void setSuccessStyle(TextField field) {
        field.setStyle("-fx-border-color: #10b981; -fx-border-width: 2; -fx-border-radius: 5;");
    }

    private void resetStyles() {
        cardHolderField.setStyle(null);
        cardNumberField.setStyle(null);
        expMonthField.setStyle(null);
        expYearField.setStyle(null);
    }

    private void showError(String message) {
        cardErrorLabel.setText(message);
        cardErrorLabel.setTextFill(javafx.scene.paint.Color.RED);
        cardErrorLabel.setVisible(true);
        cardErrorLabel.setManaged(true);
    }

    private void showSuccess(String message) {
        cardErrorLabel.setText(message);
        cardErrorLabel.setTextFill(javafx.scene.paint.Color.GREEN);
        cardErrorLabel.setVisible(true);
        cardErrorLabel.setManaged(true);
    }

    @FXML
    private void handleSave() {
        try {
            resetStyles();
            cardErrorLabel.setVisible(false);
            cardErrorLabel.setManaged(false);

            // ✅ User dynamique
            int userId = getCurrentUserId();
            if (userId <= 0) {
                showError("Utilisateur non connecté !");
                return;
            }

            String holder = cardHolderField.getText();
            String number = cardNumberField.getText();
            String monthText = expMonthField.getText();
            String yearText = expYearField.getText();

            if (monthText.isBlank() || yearText.isBlank()) {
                showError("⚠ Tous les champs sont obligatoires");
                if (monthText.isBlank()) setErrorStyle(expMonthField);
                if (yearText.isBlank()) setErrorStyle(expYearField);
                return;
            }

            int month = Integer.parseInt(monthText);
            if (month < 1 || month > 12) {
                showError("⚠ Le mois doit être entre 1 et 12");
                setErrorStyle(expMonthField);
                return;
            }
            setSuccessStyle(expMonthField);

            int year = Integer.parseInt(yearText);
            int currentYear = LocalDate.now().getYear();
            if (year < currentYear) {
                showError("⚠ L'année doit être ≥ " + currentYear);
                setErrorStyle(expYearField);
                return;
            }
            setSuccessStyle(expYearField);

            if (year == currentYear && month < LocalDate.now().getMonthValue()) {
                showError("⚠ La date d'expiration est invalide");
                setErrorStyle(expMonthField);
                setErrorStyle(expYearField);
                return;
            }

            // ✅ Utilise userId dynamique
            CreditCard activeCard = cardService.getActiveCardByUserId(userId);

            if (activeCard == null) {
                if (holder.isBlank() || number.isBlank()) {
                    showError("⚠ Le numéro et le nom de la carte sont obligatoires pour créer une nouvelle carte");
                    if (holder.isBlank()) setErrorStyle(cardHolderField);
                    if (number.isBlank()) setErrorStyle(cardNumberField);
                    return;
                }
                if (!number.matches("\\d{16}")) {
                    showError("⚠ Le numéro de carte doit contenir 16 chiffres");
                    setErrorStyle(cardNumberField);
                    return;
                }
                setSuccessStyle(cardNumberField);

                CreditCard newCard = new CreditCard();
                newCard.setIdUser(userId); // ✅ dynamique
                newCard.setCardHolderName(holder);
                newCard.setLast4Digits(number.substring(number.length() - 4));
                newCard.setExpiryMonth(month);
                newCard.setExpiryYear(year);

                cardService.insertCard(newCard);
                showSuccess("✅ Carte ajoutée avec succès !");
            } else {
                activeCard.setExpiryMonth(month);
                activeCard.setExpiryYear(year);
                cardService.updateCard(activeCard);
                showSuccess("✅ Carte mise à jour avec succès !");
            }
            loadCard();

            // ================= STRIPE =================
            CreditCard existing = cardService.getActiveCardByUserId(userId); // ✅ dynamique
            CreditCard card = existing != null ? existing : new CreditCard();
            card.setIdUser(userId); // ✅ dynamique
            card.setCardHolderName(holder);
            card.setLast4Digits(number.substring(number.length() - 4));
            card.setExpiryMonth(month);
            card.setExpiryYear(year);

            if (card.getStripeCustomerId() == null) {
                Map<String, Object> customerParams = new HashMap<>();
                customerParams.put("name", holder);
                customerParams.put("email", "eya.bouraoui2005@gmail.com");
                com.stripe.model.Customer stripeCustomer = com.stripe.model.Customer.create(customerParams);
                card.setStripeCustomerId(stripeCustomer.getId());
            }

            Map<String, Object> paymentMethodParams = new HashMap<>();
            paymentMethodParams.put("type", "card");
            paymentMethodParams.put("card", Map.of("token", "tok_visa"));
            com.stripe.model.PaymentMethod paymentMethod = com.stripe.model.PaymentMethod.create(paymentMethodParams);
            card.setStripePaymentMethodId(paymentMethod.getId());

            cardService.updateCard(card);

            showSuccess("✅ Carte enregistrée et configurée sur Stripe avec succès !");
            cardForm.setVisible(false);
            cardForm.setManaged(false);

        } catch (Exception e) {
            e.printStackTrace();
            showError("Erreur inattendue ! " + e.getMessage());
        }
    }

    public void refreshCard() {
        loadCard();
    }

    private void loadCard() {
        // ✅ User dynamique
        int userId = getCurrentUserId();
        if (userId <= 0) return;

        CreditCard card = cardService.getActiveCardByUserId(userId); // ✅ dynamique

        if (card != null) {
            rib.setText("**** **** **** " + card.getLast4Digits());
            nom.setText(card.getCardHolderName());
        } else {
            rib.setText("**** **** **** ----");
            nom.setText("NOM");
        }
    }

    @FXML
    private void handleInfosCarte() {
        walletForm.setVisible(false);
        walletForm.setManaged(false);

        cardForm.setVisible(true);
        cardForm.setManaged(true);

        // ✅ User dynamique
        int userId = getCurrentUserId();
        CreditCard card = (userId > 0) ? cardService.getActiveCardByUserId(userId) : null; // ✅ dynamique
        loadCardInForm(card);
    }

    private void loadCardInForm(CreditCard card) {
        if (card != null) {
            cardHolderField.setText(card.getCardHolderName());
            cardHolderField.setDisable(true);
            cardNumberField.setText(card.getLast4Digits());
            cardNumberField.setDisable(true);
            expMonthField.setText(String.valueOf(card.getExpiryMonth()));
            expMonthField.setDisable(false);
            expYearField.setText(String.valueOf(card.getExpiryYear()));
            expYearField.setDisable(false);
        } else {
            cardHolderField.clear();
            cardHolderField.setDisable(false);
            cardNumberField.clear();
            cardNumberField.setDisable(false);
            expMonthField.clear();
            expMonthField.setDisable(false);
            expYearField.clear();
            expYearField.setDisable(false);
        }
    }

    public PaymentIntent createStripePayment(
            double amount,
            String currency,
            String stripeCustomerId,
            String stripePaymentMethodId
    ) throws StripeException {

        long amountInCents = (long) (amount * 100);

        PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setAmount(amountInCents)
                .setCurrency(currency.toLowerCase())
                .setCustomer(stripeCustomerId)
                .setPaymentMethod(stripePaymentMethodId)
                .setConfirm(true)
                .setAutomaticPaymentMethods(
                        PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                                .setEnabled(true)
                                .setAllowRedirects(PaymentIntentCreateParams.AutomaticPaymentMethods.AllowRedirects.NEVER)
                                .build()
                )
                .build();

        PaymentIntent paymentIntent = PaymentIntent.create(params);

        System.out.println("PaymentIntent créé : " + paymentIntent.getId() +
                ", statut : " + paymentIntent.getStatus());

        return paymentIntent;
    }

    @FXML
    private void handleRecharge(wallet w,
                                currency selectedCurrency,
                                String amountText,
                                Stage stage) {

        if (selectedCurrency == null) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Veuillez choisir une devise.");
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(amountText);
            if (amount <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Montant invalide.");
            return;
        }

        try {
            // ✅ User dynamique
            int userId = getCurrentUserId();
            if (userId <= 0) {
                showAlert(Alert.AlertType.ERROR, "Erreur", "Utilisateur non connecté.");
                return;
            }

            CreditCard card = cardService.getActiveCardByUserId(userId); // ✅ dynamique
            if (card == null) {
                showAlert(Alert.AlertType.ERROR,
                        "Erreur",
                        "Aucune carte disponible. Ajoutez une carte d'abord.");
                return;
            }

            if (card.getStripeCustomerId() == null || card.getStripeCustomerId().isEmpty()) {
                Customer customer = Customer.create(
                        CustomerCreateParams.builder()
                                .setEmail("eya.bouraoui2005@gmail.com")
                                .build()
                );
                card.setStripeCustomerId(customer.getId());
                cardService.updateCard(card);
            }

            PaymentMethod pm = PaymentMethod.retrieve(card.getStripePaymentMethodId());
            if (pm.getCustomer() == null) {
                pm.attach(PaymentMethodAttachParams.builder()
                        .setCustomer(card.getStripeCustomerId())
                        .build());
            }

            PaymentIntent intent = createStripePayment(amount,
                    selectedCurrency.getNom(),
                    card.getStripeCustomerId(),
                    card.getStripePaymentMethodId());

            TransactionService transactionService = new TransactionService();
            transactionService.insertRechargeTransaction(
                    w.getIdWallet(),
                    selectedCurrency.getId_currency(),
                    amount,
                    intent.getStatus(),
                    card.getIdCard()
            );

            if ("succeeded".equals(intent.getStatus())) {
                Platform.runLater(() -> {
                    showAlert(Alert.AlertType.INFORMATION,
                            "Succès",
                            "Wallet rechargé avec succès 💰");

                    updateGlobalCurrencyChart();
                    loadWallets();

                    stage.close();
                });
            } else {
                showAlert(Alert.AlertType.WARNING,
                        "Paiement en cours",
                        "Statut du paiement : " + intent.getStatus());
            }

        } catch (StripeException se) {
            se.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur Stripe", se.getMessage());
        } catch (SQLException sqlEx) {
            sqlEx.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur BD", "Impossible d'enregistrer la transaction : " + sqlEx.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur lors de la recharge : " + e.getMessage());
        }
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void openRechargeForm(wallet w) {

        Stage stage = new Stage();
        stage.setTitle("Recharger Wallet - RIB : " + w.getRib());

        VBox root = new VBox(20);
        root.setPadding(new Insets(30));
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-color: #f4f6f9;");

        VBox card = new VBox(15);
        card.setPadding(new Insets(25));
        card.setAlignment(Pos.CENTER);
        card.setPrefWidth(350);
        card.setStyle(
                "-fx-background-color: white;" +
                        "-fx-background-radius: 20;" +
                        "-fx-border-radius: 20;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 15, 0.2, 0, 4);"
        );

        Label title = new Label("Recharger votre Wallet");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        Label lblCurrency = new Label("Devise");
        ComboBox<currency> currencyComboBox = new ComboBox<>();
        currencyComboBox.setPrefWidth(250);

        try {
            List<currency> allCurrencies = currencyService.SelectAll();
            List<currency> filteredCurrencies = allCurrencies.stream()
                    .filter(c -> {
                        if (w.getTypeWallet() == typeWallet.trading)
                            return c.getType_currency() == typeCurrency.crypto && c.isIs_trading();
                        else if (w.getTypeWallet() == typeWallet.crypto)
                            return c.getType_currency() == typeCurrency.crypto;
                        else if (w.getTypeWallet() == typeWallet.fiat)
                            return c.getType_currency() == typeCurrency.fiat;
                        return false;
                    })
                    .collect(Collectors.toList());

            currencyComboBox.setItems(FXCollections.observableArrayList(filteredCurrencies));
            currencyComboBox.setPromptText("Sélectionner une devise");

            currencyComboBox.setCellFactory(lv -> new ListCell<currency>() {
                @Override
                protected void updateItem(currency item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? null : item.getNom());
                }
            });

            currencyComboBox.setButtonCell(new ListCell<currency>() {
                @Override
                protected void updateItem(currency item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? null : item.getNom());
                }
            });

        } catch (SQLException ex) {
            ex.printStackTrace();
        }

        Label lblAmount = new Label("Montant");
        TextField txtAmount = new TextField();
        txtAmount.setPromptText("Ex: 100.00");
        txtAmount.setPrefWidth(250);

        Button btnConfirm = new Button("Valider la recharge");
        btnConfirm.setPrefWidth(250);
        btnConfirm.setOnAction(e ->
                handleRecharge(
                        w,
                        currencyComboBox.getValue(),
                        txtAmount.getText(),
                        stage
                )
        );

        card.getChildren().addAll(title, lblCurrency, currencyComboBox, lblAmount, txtAmount, btnConfirm);
        root.getChildren().add(card);

        stage.setScene(new Scene(root, 400, 450));
        stage.show();
    }

    @FXML
    private void handleCloseCardForm() {
        cardForm.setVisible(false);
        cardForm.setManaged(false);
        walletForm.setVisible(true);
        walletForm.setManaged(true);
    }

    @FXML
    private void handleDeleteCarte() {
        try {
            // ✅ User dynamique
            int userId = getCurrentUserId();
            if (userId <= 0) {
                Platform.runLater(() -> showError("Utilisateur non connecté."));
                return;
            }

            CreditCard card = cardService.getActiveCardByUserId(userId); // ✅ dynamique
            if (card == null) {
                Platform.runLater(() -> showError("Aucune carte active trouvée."));
                return;
            }

            cardService.deleteCard(card.getIdCard());

            rib.setText("");
            nom.setText("");

            Platform.runLater(() -> showSuccess("✅ Carte supprimée avec succès (statut DELETED)."));

        } catch (Exception e) {
            e.printStackTrace();
            Platform.runLater(() -> showError("Erreur lors de la suppression de la carte : " + e.getMessage()));
        }
    }

    private void loadCurrencies() {
        try {
            CurrencyService service = new CurrencyService();
            List<currency> list = service.SelectAll();
            ObservableList<String> currencyNames = FXCollections.observableArrayList();
            for (currency c : list) {
                currencyNames.add(c.getNom());
            }
            convFromBox.setItems(currencyNames);
            convToBox.setItems(currencyNames);
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de charger les currencies.");
        }
    }

    @FXML
    private void handleConvert(ActionEvent event) {
        try {
            if (convAmountField.getText().isEmpty()) {
                showAlert(Alert.AlertType.ERROR, "Erreur", "Veuillez entrer un montant.");
                return;
            }

            double amountFrom = Double.parseDouble(convAmountField.getText());
            if (convFromBox.getValue() == null || convToBox.getValue() == null) {
                showAlert(Alert.AlertType.ERROR, "Erreur", "Veuillez sélectionner les monnaies.");
                return;
            }

            String fromCurrency = convFromBox.getValue().toString();
            String toCurrency = convToBox.getValue().toString();

            ConversionService service = new ConversionService();
            double rate = service.getExchangeRate(fromCurrency, toCurrency);
            double amountTo = amountFrom * rate;

            convResultField.setText(String.format("%.8f", amountTo));

            convFromBox.setDisable(true);
            convToBox.setDisable(true);
            convAmountField.setDisable(true);

            walletSourceBox.setDisable(false);
            walletDestBox.setDisable(false);

            showAlert(Alert.AlertType.INFORMATION, "Succès", "Taux récupéré. Sélectionnez maintenant les Wallets pour valider la transaction.");

        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", "Problème API : " + e.getMessage());
        }
    }

    // ✅ loadWalletRIBs filtré par user connecté
    private void loadWalletRIBs() {
        try {
            int userId = getCurrentUserId();
            if (userId <= 0) return;

            List<wallet> list = walletService.SelectAll().stream()
                    .filter(w -> w.getIdUser() == userId) // ✅ uniquement les wallets du user connecté
                    .collect(Collectors.toList());

            ObservableList<String> ribs = FXCollections.observableArrayList();
            for (wallet w : list) {
                ribs.add(w.getRib());
            }
            walletSourceBox.setItems(ribs);
            walletDestBox.setItems(ribs);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void openTradingDashboard(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/TradingDashboard.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleQuickTransaction(ActionEvent event) {
        try {
            if (walletSourceBox.getValue() == null || walletDestBox.getValue() == null) {
                showAlert(Alert.AlertType.ERROR, "Selection Error", "Please select source and destination wallets.");
                return;
            }

            WalletService walletService = new WalletService();
            wallet sourceW = walletService.getByRib(walletSourceBox.getValue().toString());
            wallet destW = walletService.getByRib(walletDestBox.getValue().toString());

            CurrencyService cs = new CurrencyService();
            currency fromCurr = cs.getByName(convFromBox.getValue().toString());
            currency toCurr = cs.getByName(convToBox.getValue().toString());

            ConversionService convService = new ConversionService();
            Conversion conversion = new Conversion();
            conversion.setAmountFrom(Double.parseDouble(convAmountField.getText()));
            conversion.setCurrencyFrom(fromCurr.getId_currency());
            conversion.setAmountTo(Double.parseDouble(convResultField.getText().replace(",", ".")));
            conversion.setCurrencyTo(toCurr.getId_currency());

            double rate = convService.getExchangeRate(fromCurr.getCode(), toCurr.getCode());
            conversion.setExchangeRate(rate);

            int generatedConversionId = convService.insertConversion(conversion);

            transaction t = new transaction();
            t.setIdWalletSource(sourceW.getIdWallet());
            t.setIdWalletDestination(destW.getIdWallet());
            t.setMontant(Double.parseDouble(convAmountField.getText()));
            t.setType(typeTransaction.CONVERSION);
            t.setStatut(StatutTransaction.Completed);
            t.setCurrencyId(fromCurr.getId_currency());
            t.setId_conversion(generatedConversionId);

            TransactionService transService = new TransactionService();
            transService.insertOne(t);

            showAlert(Alert.AlertType.INFORMATION, "Success", "Transaction completed successfully!");

            resetConversionFields();

            Platform.runLater(() -> {
                loadCard();
                loadWallets();
                updateGlobalCurrencyChart();

                try {
                    List<wallet_currency> updatedCurrencies = walletCurrencyService.getCurrenciesByWallet(sourceW.getIdWallet());
                    System.out.println("Balances updated for wallet: " + sourceW.getIdWallet());
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Transaction Error", e.getMessage());
        }
    }

    private void resetConversionFields() {
        convAmountField.clear();
        convResultField.clear();

        convFromBox.setDisable(false);
        convToBox.setDisable(false);
        convAmountField.setDisable(false);

        walletSourceBox.setDisable(true);
        walletDestBox.setDisable(true);

        walletSourceBox.setValue(null);
        walletDestBox.setValue(null);
    }

    @FXML
    private void handleFormationNavigation(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/student_dashboard.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) btnFormation.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Student Dashboard - Formations");
            stage.show();
        } catch (IOException e) {
            System.err.println("Error loading student_dashboard.fxml: " + e.getMessage());
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR, "Impossible de charger le tableau de bord de formation.");
            alert.show();
        }
    }

    @FXML
    private void openMarketplace(ActionEvent event) {
        Utilisateur user = UserSession.getLoggedInUser();

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/MainLayout.fxml"));
            Parent root = loader.load();

            MainLayoutController controller = loader.getController();
            controller.setUser(user);

            Stage stage = (Stage) btnMarketplace.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleCreditClick(ActionEvent event) {
        Utilisateur user = UserSession.getLoggedInUser();
        if (user == null) return;

        String fxmlPath;

        switch (user.getRole()) {
            case CLIENT, COMMERCANT -> fxmlPath = "/MainDashboard.fxml";
            case INVESTISSEUR -> fxmlPath = "/invest/Marketplace.fxml";
            default -> fxmlPath = "/MainDashboard.fxml";
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
