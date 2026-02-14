package tn.esprit.Champions.gui;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import tn.esprit.Champions.models.*;
import tn.esprit.Champions.services.CurrencyService;
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
    @FXML
    private TextField walletIdField;
    @FXML
    private ComboBox<currency> currencyComboBox;

    private WalletService walletService;
    private wallet_currencyService walletCurrencyService;
    private CurrencyService currencyService;

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
        currencyService = new CurrencyService();

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


    private void showWalletDetails(wallet w, VBox card) {
        selectedWallet = w;


        if (selectedCard != null) {
            selectedCard.setStyle(
                    "-fx-background-color: linear-gradient(to bottom right, #ffffff, #e8e8e8);" +
                            "-fx-background-radius: 20;" +
                            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 8,0,0,3);"
            );
        }

        // Style de la carte sélectionnée
        selectedCard = card;
        selectedCard.setStyle(
                "-fx-background-color: #d3d3d3;" + // gris clair
                        "-fx-background-radius: 20;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 8,0,0,3);"
        );

        // Remplir le type et le statut du wallet
        if (w.getTypeWallet() != null) boxType.setValue(w.getTypeWallet().name());
        if (w.getStatut() != null) boxStatus.setValue(w.getStatut().name());


        if (walletIdField != null) {
            walletIdField.setText(String.valueOf(w.getIdWallet()));
        }

        // Charger les currencies correspondant au type du wallet
        if (currencyComboBox != null) {
            try {
                List<currency> allCurrencies = currencyService.SelectAll();
                List<currency> filtered = allCurrencies.stream()
                        .filter(c -> c.getType_currency() == w.getTypeWallet())
                        .collect(Collectors.toList());

                currencyComboBox.setItems(FXCollections.observableArrayList(filtered));
                currencyComboBox.setPromptText("Sélectionner currency");

                // Afficher le nom de la currency dans le ComboBox
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

        card.setStyle(
                "-fx-background-color: linear-gradient(to bottom right, #ffffff, #e8e8e8);" +
                        "-fx-background-radius: 20;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 8,0,0,3);"
        );

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

        // ----------- Box des currencies -----------
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

                // Afficher seulement les 2 premières currencies
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

                // Bouton View all si plus de 2 currencies
                if (currencies.size() > 2) {
                    Button viewAll = new Button("View all");
                    viewAll.setStyle(
                            "-fx-background-color: transparent;" +
                                    "-fx-text-fill: #1a5f7a;" +
                                    "-fx-font-weight: bold;"
                    );

                    viewAll.setOnAction(e -> showExpandedWallet(w, currencies));
                    currencyBox.getChildren().add(viewAll);
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        // ----------- Sélection du wallet -----------
        card.setOnMouseClicked(e -> showWalletDetails(w, card));

        // ----------- Ajout des éléments -----------
        card.getChildren().addAll(lblName, lblInfo, currencyBox);

        return card;
    }
}