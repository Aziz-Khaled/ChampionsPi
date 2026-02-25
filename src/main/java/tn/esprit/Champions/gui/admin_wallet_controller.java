package tn.esprit.Champions.gui;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import tn.esprit.Champions.models.*;
import tn.esprit.Champions.services.*;

import java.io.IOException;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class admin_wallet_controller {

    @FXML private TableView<transaction> table;
    @FXML private TableColumn<transaction, String> userCol;
    @FXML private TableColumn<transaction, String> sourceCol;
    @FXML private TableColumn<transaction, String> destCol;
    @FXML private TableColumn<transaction, String> montantCol;
    @FXML private TableColumn<transaction, String> currencyCol;
    @FXML private TableColumn<transaction, String> typeCol;
    @FXML private TableColumn<transaction, String> dateCol;
    @FXML private TableColumn<transaction, String> statutCol;

    @FXML private AnchorPane notifPane;
    @FXML private VBox notifContainer;
    @FXML private Label notifCountLabel;
    @FXML private Circle notifBadge;
    @FXML private Button btnSignOut;

    // Services
    private TransactionService transactionService = new TransactionService();
    private WalletService walletService = new WalletService();
    private UtilisateurService userService = new UtilisateurService();
    private CurrencyService currencyService = new CurrencyService();
    private CreditCardService creditCardService = new CreditCardService();
    private NotificationAdminService notificationService = new NotificationAdminService();

    private ObservableList<transaction> transactionList = FXCollections.observableArrayList();
    private List<Utilisateur> usersLookup = new ArrayList<>();

    @FXML
    public void initialize() {
        // Charger les utilisateurs en mémoire une seule fois pour la résolution des noms
        loadUsersCache();

        setupTableColumns();
        refreshTransactions();
        updateUnreadCount();

        Platform.runLater(() -> {
            if (table.getScene() != null) {
                table.getScene().addEventFilter(javafx.scene.input.MouseEvent.MOUSE_PRESSED, e -> {
                    if (notifPane.isVisible() && !notifPane.getBoundsInParent().contains(e.getSceneX(), e.getSceneY())) {
                        notifPane.setVisible(false);
                    }
                });
            }
        });
    }

    private void loadUsersCache() {
        try {
            this.usersLookup = userService.getAllUsers();
        } catch (SQLException e) {
            e.printStackTrace();
            this.usersLookup = new ArrayList<>();
        }
    }

    private void setupTableColumns() {
        // 1. Utilisateur : Nom + Prénom (via Cache)
        userCol.setCellValueFactory(cell -> {
            try {
                wallet w = walletService.SelectById(cell.getValue().getIdWalletDestination());
                // Recherche dans la liste chargée au lieu d'un appel SQL direct
                Utilisateur u = usersLookup.stream()
                        .filter(user -> user.getId_user() == w.getIdUser())
                        .findFirst()
                        .orElse(null);

                if (u != null) {
                    return new SimpleStringProperty(u.getNom().toUpperCase() + " " + u.getPrenom());
                }
            } catch (Exception e) {
                return new SimpleStringProperty("Système / Inconnu");
            }
            return new SimpleStringProperty("Inconnu");
        });

        // 2. Source : RIB ou Last 4 Digits si Recharge
        sourceCol.setCellValueFactory(cell -> {
            transaction t = cell.getValue();
            String display = "N/A";
            try {
                if (t.getType() == typeTransaction.RECHARGE) {
                    CreditCard c = creditCardService.getCardById(t.getId_card());
                    display = (c != null) ? "**** " + c.getLast4Digits() : "Carte Inconnue";
                } else {
                    wallet w = walletService.SelectById(t.getIdWalletSource());
                    display = (w != null) ? w.getRib() : "Dépôt Direct";
                }
            } catch (Exception e) { display = "---"; }
            return new SimpleStringProperty(display);
        });

        // 3. Destination : RIB
        destCol.setCellValueFactory(cell -> {
            try {
                wallet w = walletService.SelectById(cell.getValue().getIdWalletDestination());
                return new SimpleStringProperty(w != null ? w.getRib() : "N/A");
            } catch (Exception e) { return new SimpleStringProperty("N/A"); }
        });

        // 4. Montant
        montantCol.setCellValueFactory(cell -> new SimpleStringProperty(String.format("%.2f", cell.getValue().getMontant())));

        // 5. Devise
        currencyCol.setCellValueFactory(cell -> {
            try {
                return new SimpleStringProperty(currencyService.getCurrencyNameById(cell.getValue().getCurrencyId()));
            } catch (Exception e) { return new SimpleStringProperty("BTC"); }
        });

        typeCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getType().name()));

        dateCol.setCellValueFactory(cell -> {
            if (cell.getValue().getDateTransaction() != null) {
                return new SimpleStringProperty(cell.getValue().getDateTransaction().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")));
            }
            return new SimpleStringProperty("-");
        });

        statutCol.setCellValueFactory(new PropertyValueFactory<>("statut"));

        table.setItems(transactionList);
    }

    @FXML
    public void refreshTransactions() {
        try {
            // Mettre à jour la liste des utilisateurs pour les noms
            loadUsersCache();

            // Appeler la nouvelle méthode spécifique à l'admin
            List<transaction> allData = transactionService.getAllTransactionsForAdmin();

            // Mettre à jour l'ObservableList de la TableView
            transactionList.setAll(allData);

            System.out.println("Transactions chargées : " + allData.size()); // Pour debug
        } catch (SQLException e) {
            System.err.println("Erreur lors du chargement des transactions : " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void toggleNotifications() {
        notifPane.setVisible(!notifPane.isVisible());
        if (notifPane.isVisible()) {
            loadNotifications();
            try {
                notificationService.markAllAsRead();
                updateUnreadCount();
            } catch (SQLException e) { e.printStackTrace(); }
        }
    }

    private void loadNotifications() {
        notifContainer.getChildren().clear();
        try {
            List<NotificationAdmin> list = notificationService.getAllNotifications();
            for (NotificationAdmin n : list) {
                VBox card = new VBox(5);
                card.setStyle("-fx-background-color: white; -fx-padding: 10; -fx-border-color: #ecf0f1; -fx-border-width: 0 0 1 0;");

                Label type = new Label(n.getTypeNotification().name());
                type.setStyle("-fx-font-weight: bold; -fx-text-fill: #e74c3c; -fx-font-size: 11;");

                Label msg = new Label(n.getMessage());
                msg.setWrapText(true);
                msg.setStyle("-fx-font-size: 10; -fx-text-fill: #34495e;");

                card.getChildren().addAll(type, msg);
                notifContainer.getChildren().add(card);
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void updateUnreadCount() {
        try {
            int count = notificationService.getUnreadCount();
            notifCountLabel.setText(String.valueOf(count));
            notifBadge.setVisible(count > 0);
            notifCountLabel.setVisible(count > 0);
        } catch (Exception e) {}
    }

    @FXML
    private void handleSignOut() {
        try {
            // Assure-toi que le chemin vers Login.fxml est correct
            Parent root = FXMLLoader.load(getClass().getResource("/tn/esprit/Champions/gui/Login.fxml"));
            btnSignOut.getScene().setRoot(root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}