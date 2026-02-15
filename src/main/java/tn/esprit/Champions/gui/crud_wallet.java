package tn.esprit.Champions.gui;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import tn.esprit.Champions.models.*;
import tn.esprit.Champions.services.CurrencyService;
import tn.esprit.Champions.services.TransactionService;
import tn.esprit.Champions.services.WalletService;
import tn.esprit.Champions.services.wallet_currencyService;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
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
    @FXML
    private TextField walletIdField;
    @FXML
    private ComboBox<currency> currencyComboBox;
    @FXML private VBox walletForm;
    @FXML private VBox transactionForm;
    @FXML private StackPane flipContainer;


    @FXML private TextField sourceWalletField;
    @FXML private TextField destinationWalletField;
    @FXML private TextField amountField;
    @FXML private ComboBox<String> typeTransactionBox;  // transaction_interne / transaction_externe
    @FXML private ComboBox<String> statusTransactionBox; // retrait / recharge
    @FXML private ComboBox<String> currencyTransactionBox; // currency sélectionnée pour la transaction
    @FXML private Button addTransactionBtn; // bouton "+"




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

    private wallet selectedWallet; // wallet sélectionné

    private final int DEFAULT_USER_ID = 1;


    @FXML
    public void initialize() {
        // ComboBox initialisation
        boxType.getItems().addAll("fiat", "crypto", "trading");
        boxStatus.getItems().addAll("actif", "bloque");


        walletService = new WalletService();
        walletCurrencyService = new wallet_currencyService();
        currencyService = new CurrencyService();


        loadWallets();

        if (searchField != null) searchField.setOnKeyReleased(this::handleSearch);
        if (clearSearchButton != null) clearSearchButton.setOnAction(e -> handleClearSearch());
        if (modify_wallet != null) modify_wallet.setOnAction(e -> handleModifyWallet());
        if (delete_wallet != null) delete_wallet.setOnAction(e -> handleDeleteWallet());

        // Listener global pour cliquer **en dehors d'une carte**
        Platform.runLater(() -> {
            Scene scene = walletContainer.getScene();
            if (scene != null) {
                scene.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
                    Node target = (Node) event.getTarget();
                    if (!isWalletCard(target) && !isWalletForm(target)) { // <-- ajouter la vérif formulaire
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
        if (sourceWalletField != null) sourceWalletField.clear();
        if (destinationWalletField != null) destinationWalletField.clear();
        if (amountField != null) amountField.clear();
        if (typeTransactionBox != null) typeTransactionBox.getSelectionModel().clearSelection();
        if (statusTransactionBox != null) statusTransactionBox.getSelectionModel().clearSelection();
        if (currencyTransactionBox != null) currencyTransactionBox.getSelectionModel().clearSelection();

    }
    private void clearWalletSelection() {
        if (selectedCard != null) {
            // Remet le style initial
            selectedCard.setStyle(
                    "-fx-background-color: linear-gradient(to bottom right, #ffffff, #e8e8e8);" +
                            "-fx-background-radius: 20;" +
                            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 8,0,0,3);"
            );
            selectedCard = null;

        }

        // Vider le formulaire
        boxType.setDisable(false);
        walletIdField.clear();
        boxType.getSelectionModel().clearSelection();
        boxStatus.getSelectionModel().clearSelection();
        currencyComboBox.getSelectionModel().clearSelection();

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
            // Créer l'objet wallet_currency à insérer
            wallet_currency wc = new wallet_currency();
            wc.setId_wallet(selectedWallet.getIdWallet());
            wc.setNom_currency(selectedCurrency.getNom());

            // Appeler la fonction insertOne() qui gère déjà la duplication
            walletCurrencyService.insertOne(wc);

            new Alert(Alert.AlertType.INFORMATION, "Currency ajoutée au wallet avec succès !").show();

            // Recharger les wallets pour mettre à jour l'affichage
            loadWallets();

        } catch (SQLException e) {
            // Afficher le message d'erreur de duplication ou autre erreur
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
            // Créer un objet wallet_currency pour passer au service
            wallet_currency wcToDelete = new wallet_currency();
            wcToDelete.setId_wallet(selectedWallet.getIdWallet());
            wcToDelete.setId_currency(selectedCurrency.getId_currency());
            wcToDelete.setNom_currency(selectedCurrency.getNom());

            // Appeler deleteOne du service (qui gère déjà la vérification du solde)
            walletCurrencyService.deleteOne(wcToDelete);

            new Alert(Alert.AlertType.INFORMATION, "Currency supprimée du wallet avec succès !").show();

            loadWallets();

        } catch (SQLException e) {
            // deleteOne lance une exception si le solde n'est pas nul
            new Alert(Alert.AlertType.ERROR, e.getMessage()).show();
            e.printStackTrace();
        }
    }

    @FXML
    private void handleAddTransaction() {
        try {
            // 1️⃣ Récupérer les valeurs du formulaire
            int idWalletSource = Integer.parseInt(sourceWalletField.getText().trim());
            int idWalletDest = Integer.parseInt(destinationWalletField.getText().trim());
            String currencyName = currencyTransactionBox.getValue();
            double montant = Double.parseDouble(amountField.getText().trim());

            if (currencyName == null || currencyName.isEmpty()) {
                new Alert(Alert.AlertType.WARNING, "Veuillez sélectionner une currency !").show();
                return;
            }

            if (montant <= 0) {
                new Alert(Alert.AlertType.WARNING, "Le montant doit être supérieur à 0 !").show();
                return;
            }

            // 2️⃣ Récupérer les wallets
            wallet sourceWallet = walletService.SelectById(idWalletSource);
            wallet destWallet = walletService.SelectById(idWalletDest);

            if (sourceWallet == null || destWallet == null) {
                new Alert(Alert.AlertType.ERROR, "Wallet introuvable !").show();
                return;
            }

            // 3️⃣ Vérifications
            if (sourceWallet.getIdWallet() == destWallet.getIdWallet()) {
                new Alert(Alert.AlertType.WARNING, "Le wallet source et destination doivent être différents !").show();
                return;
            }

            if (sourceWallet.getStatut() == statutWallet.bloque) {
                new Alert(Alert.AlertType.WARNING, "Le wallet source est bloqué !").show();
                return;
            }

            if (sourceWallet.getTypeWallet() == typeWallet.fiat && destWallet.getTypeWallet() != typeWallet.fiat) {
                new Alert(Alert.AlertType.WARNING, "Wallet fiat ne peut envoyer qu'à un wallet fiat !").show();
                return;
            }

            // 4️⃣ Récupérer l'id_currency depuis le nom
            int idCurrency = walletCurrencyService.getCurrencyIdByName(currencyName);
            if (idCurrency == 0) {
                new Alert(Alert.AlertType.ERROR, "Currency introuvable !").show();
                return;
            }

            // 5️⃣ Vérifier le solde du wallet source
            wallet_currency sourceCurrency = walletCurrencyService.getWalletCurrencyByWalletAndId(
                    sourceWallet.getIdWallet(), idCurrency
            );

            if (sourceCurrency == null || sourceCurrency.getSolde() < montant) {
                new Alert(Alert.AlertType.WARNING, "Solde insuffisant dans le wallet source !").show();
                return;
            }

            // 6️⃣ Mettre à jour le solde du wallet source



            // 7️⃣ Mettre à jour le solde du wallet destinataire
            wallet_currency destCurrency = walletCurrencyService.getWalletCurrencyByWalletAndId(
                    destWallet.getIdWallet(), idCurrency
            );

            if (destCurrency == null) {
                destCurrency = new wallet_currency();
                destCurrency.setId_wallet(destWallet.getIdWallet());
                destCurrency.setId_currency(idCurrency);
                destCurrency.setNom_currency(currencyName);
                destCurrency.setSolde(montant);
                walletCurrencyService.insertOne(destCurrency);
            } else {



            }

            // 8️⃣ Créer et enregistrer la transaction
            transaction t = new transaction();
            t.setIdWalletSource(idWalletSource);
            t.setIdWalletDestination(idWalletDest);
            t.setMontant(montant);
            t.setCurrencyId(idCurrency); // on utilise l'ID
            t.setDateTransaction(java.time.LocalDateTime.now());
            t.setType(typeTransaction.TRANSFERT);       // toujours TRANSFERT
            t.setStatut(StatutTransaction.Completed);   // toujours Completed

            TransactionService transactionService = new TransactionService();
            transactionService.insertOne(t);
            loadWallets();

            // 9️⃣ Confirmation
            new Alert(Alert.AlertType.INFORMATION, "Transaction effectuée avec succès !").show();
            loadWallets();

            //  🔟 Vider le formulaire
            sourceWalletField.clear();
            destinationWalletField.clear();
            amountField.clear();
            currencyTransactionBox.getSelectionModel().clearSelection();

        } catch (NumberFormatException e) {
            new Alert(Alert.AlertType.WARNING, "Veuillez entrer des valeurs valides pour les champs numériques !").show();
        } catch (SQLException e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Erreur lors de la transaction : " + e.getMessage()).show();
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

        // Parcours tous les containers
        for (HBox container : List.of(walletContainer, walletContainer1, walletContainer2)) {
            for (Node node : container.getChildren()) {
                if (node instanceof VBox card) {
                    Label lblName = (Label) card.getChildren().get(0); // "Wallet #id"
                    if (lblName.getText().contains(String.valueOf(walletId))) {
                        VBox currencyBox = (VBox) card.getChildren().get(2); // currencyBox
                        // Mettre à jour les soldes dans chaque ligne HBox
                        for (Node lineNode : currencyBox.getChildren()) {
                            if (lineNode instanceof HBox line) {
                                if (line.getChildren().size() < 2) continue; // ignore "View all" button
                                Label nameLabel = (Label) line.getChildren().get(0);
                                Label soldeLabel = (Label) line.getChildren().get(1);

                                String currencyName = nameLabel.getText();
                                currencies.stream()
                                        .filter(wc -> wc.getNom_currency().equals(currencyName))
                                        .findFirst()
                                        .ifPresent(wc -> soldeLabel.setText(String.format("%.2f", wc.getSolde())));
                            }
                        }
                        return; // carte trouvée et mise à jour
                    }
                }
            }
        }
    }

    private void showWalletDetails(wallet w, VBox card) {
        selectedWallet = w;

        // -------------------- Remettre l'ancienne carte à son style initial --------------------
        if (selectedCard != null) {
            boxType.setDisable(true);
            selectedCard.setStyle(
                    "-fx-background-color: linear-gradient(to bottom right, #ffffff, #e8e8e8);" +
                            "-fx-background-radius: 20;" +
                            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 8,0,0,3);"
            );
        }

        // -------------------- Style de la carte sélectionnée --------------------
        selectedCard = card;
        selectedCard.setStyle(
                "-fx-background-color: #d3d3d3;" + // gris clair
                        "-fx-background-radius: 20;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 8,0,0,3);"
        );

        // -------------------- Remplir type et statut du wallet --------------------
        if (w.getTypeWallet() != null) boxType.setValue(w.getTypeWallet().name());
        if (w.getStatut() != null) boxStatus.setValue(w.getStatut().name());
        boxType.setDisable(true);

        // -------------------- Remplir ID du wallet dans le formulaire --------------------
        if (walletIdField != null) {
            walletIdField.setText(String.valueOf(w.getIdWallet()));
        }

        // -------------------- Charger les currencies pour le formulaire Wallet --------------------
        if (currencyComboBox != null) {
            try {
                List<currency> allCurrencies = currencyService.SelectAll();

                // Filtrer selon le type de wallet et is_trading
                List<currency> filteredCurrencies = allCurrencies.stream()
                        .filter(c -> {
                            if (w.getTypeWallet() == typeWallet.trading) {
                                return c.getType_currency() == typeCurrency.crypto && c.isIs_trading();
                            } else if (w.getTypeWallet() == typeWallet.crypto) {
                                return c.getType_currency() == typeCurrency.crypto;
                            } else if (w.getTypeWallet() == typeWallet.fiat) {
                                return c.getType_currency() == typeCurrency.fiat;
                            }
                            return false;
                        })
                        .collect(Collectors.toList());

                currencyComboBox.setItems(FXCollections.observableArrayList(filteredCurrencies));
                currencyComboBox.setPromptText("Sélectionner currency");

                // Afficher le nom de la currency dans la liste déroulante
                currencyComboBox.setCellFactory(c -> new ListCell<>() {
                    @Override
                    protected void updateItem(currency item, boolean empty) {
                        super.updateItem(item, empty);
                        setText(empty || item == null ? "" : item.getNom());
                    }
                });

                // Afficher le nom de la currency sélectionnée dans le bouton
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

        // -------------------- Remplir le formulaire Transaction --------------------
        if (sourceWalletField != null) {
            sourceWalletField.setText(String.valueOf(w.getIdWallet())); // ID wallet source
            sourceWalletField.setEditable(false);
        }

        if (currencyTransactionBox != null) {
            try {
                // Récupérer toutes les currencies du wallet sélectionné
                List<wallet_currency> walletCurrencies = walletCurrencyService.getCurrenciesByWallet(w.getIdWallet());
                List<currency> allCurrencies = currencyService.SelectAll();

                // Créer un map pour accès rapide aux currencies
                Map<String, currency> currencyMap = allCurrencies.stream()
                        .collect(Collectors.toMap(currency::getNom, c -> c));

                // Filtrer selon is_trading si wallet trading
                walletCurrencies = walletCurrencies.stream()
                        .filter(wc -> {
                            currency c = currencyMap.get(wc.getNom_currency());
                            return c != null && (w.getTypeWallet() != typeWallet.trading || c.isIs_trading());
                        })
                        .toList();

                // Extraire les noms pour le ComboBox
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

        // ----------- Titre Wallet -----------
        Label title = new Label("Wallet #" + w.getIdWallet());
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #1a5f7a;");
        title.setAlignment(Pos.CENTER);
        title.setMaxWidth(Double.MAX_VALUE);

        // ----------- Type + Statut -----------
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

        // ----------- Box des currencies -----------
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

        // Scroll si beaucoup de currencies
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

        // ----------- Label emoji transactions en haut à droite -----------
        Label lblTransactions = new Label("💸");
        lblTransactions.setStyle("-fx-font-size: 16px; -fx-text-fill: black; -fx-cursor: hand;");

// HBox pour positionner en haut à gauche, légèrement décalé pour éviter le scroll
        HBox topBar = new HBox(lblTransactions);
        topBar.setAlignment(Pos.TOP_LEFT);
        topBar.setPadding(new Insets(0, 0, 0, 0)); // ← 8px depuis le haut et la gauche
        topBar.setMaxWidth(Double.MAX_VALUE);




        // ----------- Titre Wallet -----------
        Label lblName = new Label("Wallet #" + w.getIdWallet());
        lblName.setStyle("-fx-text-fill: #1a5f7a; -fx-font-size: 18px; -fx-font-weight: bold;");
        lblName.setAlignment(Pos.CENTER);
        lblName.setMaxWidth(Double.MAX_VALUE);

        // ----------- Type + Statut -----------
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

        // ----------- Box des currencies (compact) -----------
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
                for (int i = 0; i < Math.min(2, currencies.size()); i++) {
                    wallet_currency wc = currencies.get(i);

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

        // ----------- Clic sur l'emoji transactions pour ouvrir le tableau -----------

        lblTransactions.setOnMouseClicked(e -> {
            try {
                TransactionService transactionService = new TransactionService();
                List<transaction> transactions = transactionService.getTransactionsByWallet(w.getIdWallet());
                int walletId = w.getIdWallet();

                TableView<transaction> transactionTable = new TableView<>();
                transactionTable.setPrefHeight(300);

                TableColumn<transaction, Integer> sourceCol = new TableColumn<>("Source");
                sourceCol.setCellValueFactory(new PropertyValueFactory<>("idWalletSource"));

                TableColumn<transaction, Integer> destCol = new TableColumn<>("Destination");
                destCol.setCellValueFactory(new PropertyValueFactory<>("idWalletDestination"));

                TableColumn<transaction, Double> montantCol = new TableColumn<>("Montant");
                montantCol.setCellValueFactory(new PropertyValueFactory<>("montant"));

                TableColumn<transaction, LocalDateTime> dateCol = new TableColumn<>("Date");
                dateCol.setCellValueFactory(new PropertyValueFactory<>("dateTransaction"));

                // Colonne poubelle
                TableColumn<transaction, Void> deleteCol = new TableColumn<>("Delete");
                deleteCol.setCellFactory(col -> new TableCell<>() {
                    private final Label trash = new Label("🗑");
                    {
                        trash.setStyle("-fx-text-fill: red; -fx-cursor: hand;");
                        trash.setOnMouseClicked(ev -> {
                            transaction t = getTableView().getItems().get(getIndex());
                            //deleteTransaction(t); // Méthode pour supprimer la transaction
                            getTableView().getItems().remove(t);
                        });
                    }
                    @Override
                    protected void updateItem(Void item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty) setGraphic(null);
                        else setGraphic(trash);
                    }
                });

                transactionTable.getColumns().addAll(sourceCol, destCol, montantCol, dateCol, deleteCol);

                // Coloration rouge/vert selon wallet
                transactionTable.setRowFactory(tv -> new TableRow<transaction>() {
                    @Override
                    protected void updateItem(transaction item, boolean empty) {
                        super.updateItem(item, empty);
                        if (item == null || empty) setStyle("");
                        else if (item.getIdWalletSource() == walletId) setStyle("-fx-background-color: #ffcccc;");
                        else if (item.getIdWalletDestination() == walletId) setStyle("-fx-background-color: #ccffcc;");
                        else setStyle("");
                    }
                });

                transactionTable.setItems(FXCollections.observableArrayList(transactions));

                // Nouvelle fenêtre
                Stage stage = new Stage();
                VBox root = new VBox(transactionTable);
                root.setPadding(new Insets(10));
                Scene scene = new Scene(root);
                stage.setScene(scene);
                stage.setTitle("Transactions Wallet #" + walletId);
                stage.show();

            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        });

        // ----------- Clic sur la carte -----------
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


}