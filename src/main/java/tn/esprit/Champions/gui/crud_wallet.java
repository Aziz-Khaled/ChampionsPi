package tn.esprit.Champions.gui;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import tn.esprit.Champions.models.statutWallet;
import tn.esprit.Champions.models.typeWallet;
import tn.esprit.Champions.models.wallet;
import tn.esprit.Champions.models.wallet_currency;
import tn.esprit.Champions.services.WalletService;
import tn.esprit.Champions.services.wallet_currencyService;

import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

public class crud_wallet {

    @FXML
    private ComboBox<String> boxType; // type du wallet (lecture seule)
    @FXML
    private ComboBox<String> boxStatus; // statut du wallet (modifiable)
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

    private WalletService walletService;
    private wallet_currencyService walletCurrencyService;

    private wallet selectedWallet; // wallet sélectionné
    private VBox selectedCard; // carte sélectionnée
    private final int DEFAULT_USER_ID = 1;

    @FXML
    public void initialize() {
        // Remplir les ComboBox
        boxType.getItems().addAll("fiat", "crypto", "trading");
        boxStatus.getItems().addAll("actif", "bloque");

        boxType.setDisable(true);

        walletService = new WalletService();
        walletCurrencyService = new wallet_currencyService();

        loadWallets();

        if (searchField != null) searchField.setOnKeyReleased(this::handleSearch);
        if (clearSearchButton != null) clearSearchButton.setOnAction(e -> handleClearSearch());
        if (modify_wallet != null) modify_wallet.setOnAction(e -> handleModifyWallet());
        if (delete_wallet != null) delete_wallet.setOnAction(e -> handleDeleteWallet());
    }

    // Ajouter un wallet
    @FXML
    private void handleAddWallet() {
        String typeStr = boxType.getValue();
        String statusStr = boxStatus.getValue();

        if (typeStr == null || statusStr == null) {
            new Alert(Alert.AlertType.WARNING, "Veuillez sélectionner Type et Statut !").show();
            return;
        }

        wallet wallet = new wallet();
        wallet.setTypeWallet(typeWallet.valueOf(typeStr));
        wallet.setStatut(statutWallet.valueOf(statusStr));
        wallet.setIdUser(DEFAULT_USER_ID);

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

    // Modifier le statut d’un wallet sélectionné
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

    // Supprimer un wallet sélectionné
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
                // Vérifier que tous les soldes sont à 0
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

    // Remplir le formulaire et surligner la carte sélectionnée
    private void showWalletDetails(wallet w, VBox card) {
        selectedWallet = w;

        // Reset style ancienne carte
        if (selectedCard != null) {
            selectedCard.setStyle(
                    "-fx-background-color: linear-gradient(to bottom right, #ffffff, #e8e8e8);" +
                            "-fx-background-radius: 20;" +
                            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 8,0,0,3);"
            );
        }

        // Style carte sélectionnée
        selectedCard = card;
        selectedCard.setStyle(
                "-fx-background-color: #d3d3d3;" + // gris clair
                        "-fx-background-radius: 20;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 8,0,0,3);"
        );

        if (w.getTypeWallet() != null) boxType.setValue(w.getTypeWallet().name());
        if (w.getStatut() != null) boxStatus.setValue(w.getStatut().name());
    }

    // Recherche
    @FXML
    private void handleSearch(KeyEvent event) {
        if (searchField == null) return;

        String filter = searchField.getText().toLowerCase();

        try {
            List<wallet> wallets = walletService.SelectAll();
            List<wallet> filtered = wallets.stream()
                    .filter(w -> String.valueOf(w.getIdWallet()).contains(filter)
                            || w.getTypeWallet().name().toLowerCase().contains(filter)
                            || w.getStatut().name().toLowerCase().contains(filter))
                    .collect(Collectors.toList());

            displayWallets(filtered);

        } catch (SQLException e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Erreur lors de la recherche !").show();
        }
    }

    // Effacer la recherche
    @FXML
    private void handleClearSearch() {
        if (searchField != null) searchField.clear();
        loadWallets();
    }

    // Charger les wallets
    private void loadWallets() {
        try {
            List<wallet> wallets = walletService.SelectAll();
            displayWallets(wallets);
        } catch (SQLException e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Erreur lors du chargement des wallets !").show();
        }
    }

    // Afficher les wallets dans les ScrollPane
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

    // Créer une carte pour un wallet
    private VBox createWalletCard(wallet w) {
        VBox card = new VBox();
        card.setSpacing(12);
        card.setPrefWidth(280);
        card.setPrefHeight(170);
        card.setPadding(new Insets(15));
        card.setAlignment(Pos.CENTER);
        card.setStyle(
                "-fx-background-color: linear-gradient(to bottom right, #ffffff, #e8e8e8);" +
                        "-fx-background-radius: 20;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 8,0,0,3);"
        );

        Label lblName = new Label("Wallet #" + w.getIdWallet());
        lblName.setStyle("-fx-text-fill: #1a5f7a; -fx-font-size: 18px; -fx-font-weight: bold;");
        lblName.setAlignment(Pos.CENTER);
        lblName.setMaxWidth(Double.MAX_VALUE);

        String typeText = (w.getTypeWallet() != null ? w.getTypeWallet().name() : "");
        String statutText = (w.getStatut() != null ? w.getStatut().name() : "");
        Label lblInfo = new Label(typeText + " • " + statutText);

        if ("bloque".equalsIgnoreCase(statutText)) lblInfo.setStyle("-fx-text-fill: red; -fx-font-size: 13px;");
        else if ("actif".equalsIgnoreCase(statutText)) lblInfo.setStyle("-fx-text-fill: green; -fx-font-size: 13px;");
        else lblInfo.setStyle("-fx-text-fill: #6e6e6e; -fx-font-size: 13px;");

        lblInfo.setAlignment(Pos.CENTER);
        lblInfo.setMaxWidth(Double.MAX_VALUE);

        VBox currencyBox = new VBox();
        currencyBox.setSpacing(4);
        currencyBox.setAlignment(Pos.CENTER);

        try {
            List<wallet_currency> currencies = walletCurrencyService.getCurrenciesByWallet(w.getIdWallet());
            if (currencies.isEmpty()) {
                Label empty = new Label("No currencies");
                empty.setStyle("-fx-text-fill: #a0a0a0;");
                currencyBox.getChildren().add(empty);
            } else {
                for (wallet_currency wc : currencies) {
                    HBox line = new HBox();
                    line.setSpacing(10);
                    line.setAlignment(Pos.CENTER);

                    Label name = new Label(wc.getNom_currency());
                    name.setStyle("-fx-text-fill: #2c2c2c; -fx-font-size: 14px; -fx-font-weight: bold;");

                    Label solde = new Label(String.format("%.2f", wc.getSolde()));
                    solde.setStyle("-fx-text-fill: #4a4a4a; -fx-font-size: 14px;");

                    line.getChildren().addAll(name, solde);
                    currencyBox.getChildren().add(line);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        card.getChildren().addAll(lblName, lblInfo, currencyBox);
        return card;
    }
}