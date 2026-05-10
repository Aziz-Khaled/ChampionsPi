package tn.esprit.Champions.gui.invest;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.util.StringConverter;
import tn.esprit.Champions.models.*;
import tn.esprit.Champions.services.TransactionService;
import tn.esprit.Champions.services.WalletService;
import tn.esprit.Champions.services.projetService;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

public class ConfirmTransactionController {

    @FXML
    private VBox confirmRoot;
    @FXML
    private VBox containerRIB;
    @FXML
    private Label lblMontant, lblDestinataire, lblSoldeActuel, lblNouveauSolde;
    @FXML
    private ComboBox<wallet> comboWalletSelection;
    @FXML
    private Button btnConfirmer;

    private credit currentCredit;
    private final WalletService walletService = new WalletService();
    private final projetService ps = new projetService();
    private final int CURRENT_USER_ID = 56;
    private boolean isSelectionMode = false;

    @FXML
    public void initialize() {
        containerRIB.setVisible(false);
        containerRIB.setManaged(false);
        setupWalletSelector();
        setupAutoRefresh();
    }

    private int getCurrencyIdByName(String name) {
        String query = "SELECT id_currency FROM currency WHERE nom = ?";
        try (PreparedStatement pst = walletService.getCnx().prepareStatement(query)) {
            pst.setString(1, name);
            ResultSet rs = pst.executeQuery();
            if (rs.next())
                return rs.getInt("id_currency");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 13; // ID TND par défaut
    }

    private double getSoldeFromWalletCurrency(int idWallet, int idCurrency) {
        String query = "SELECT solde FROM wallet_currency WHERE id_wallet = ? AND id_currency = ?";
        try (PreparedStatement pst = walletService.getCnx().prepareStatement(query)) {
            pst.setInt(1, idWallet);
            pst.setInt(2, idCurrency);
            ResultSet rs = pst.executeQuery();
            if (rs.next())
                return rs.getDouble("solde");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0.0;
    }

    private void setupWalletSelector() {
        try {
            List<wallet> userWallets = walletService.SelectAll().stream()
                    .filter(w -> w.getIdUser() == CURRENT_USER_ID)
                    .filter(w -> w.getTypeWallet() == typeWallet.fiat)
                    .collect(Collectors.toList());

            comboWalletSelection.getItems().setAll(userWallets);
            comboWalletSelection.setConverter(new StringConverter<wallet>() {
                @Override
                public String toString(wallet w) {
                    return (w == null) ? "" : "RIB: " + w.getRib();
                }

                @Override
                public wallet fromString(String string) {
                    return null;
                }
            });

            comboWalletSelection.getSelectionModel().selectedItemProperty().addListener((obs, oldW, newW) -> {
                if (newW != null)
                    updateTransactionUI(newW);
            });
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void updateTransactionUI(wallet selectedWallet) {
        int idTND = getCurrencyIdByName("TND");
        double soldeInitial = getSoldeFromWalletCurrency(selectedWallet.getIdWallet(), idTND);
        double montantInvesti = (currentCredit != null) ? currentCredit.getMontant() : 0;
        double reste = soldeInitial - montantInvesti;

        lblSoldeActuel.setText(String.format("%.2f TND", soldeInitial));
        lblNouveauSolde.setText(String.format("%.2f TND", reste));

        if (reste < 0) {
            btnConfirmer.setDisable(true);
            btnConfirmer.setText("SOLDE INSUFFISANT");
            lblNouveauSolde.setStyle("-fx-text-fill: #ff4d4d; -fx-font-weight: bold;");
        } else {
            btnConfirmer.setDisable(false);
            btnConfirmer.setText(isSelectionMode ? "VALIDER L'INVESTISSEMENT" : "CONFIRMER");
            lblNouveauSolde.setStyle("-fx-text-fill: #00ff88; -fx-font-weight: bold;");
        }
    }

    @FXML
    private void handleConfirmation() {
        if (!isSelectionMode) {
            containerRIB.setVisible(true);
            containerRIB.setManaged(true);
            btnConfirmer.setText("SÉLECTIONNEZ UN COMPTE");
            isSelectionMode = true;
            if (confirmRoot.getScene() != null)
                confirmRoot.getScene().getWindow().sizeToScene();
        } else {
            wallet sourceWallet = comboWalletSelection.getSelectionModel().getSelectedItem();
            if (sourceWallet == null)
                return;

            try {
                int idTND = getCurrencyIdByName("TND");

                // 1. Trouver le projet associé
                projet p = ps.SelectAll().stream()
                        .filter(proj -> proj.getId_projet() == currentCredit.getProject_id())
                        .findFirst()
                        .orElse(null);

                if (p == null || p.getOwner_id() == null) {
                    new Alert(Alert.AlertType.ERROR, "Impossible de trouver le propriétaire du projet.").show();
                    return;
                }

                // 2. Extraire l'ID de l'objet Utilisateur (Vérifiez le nom de la méthode dans
                // Utilisateur.java)
                int idProprietaire = p.getOwner_id().getId_user();

                // 3. Trouver le wallet FIAT du propriétaire
                wallet destWallet = walletService.SelectAll().stream()
                        .filter(w -> w.getIdUser() == idProprietaire)
                        .filter(w -> w.getTypeWallet() == typeWallet.fiat)
                        .findFirst().orElse(null);

                if (destWallet == null) {
                    new Alert(Alert.AlertType.ERROR, "Le destinataire n'a pas de compte FIAT compatible.").show();
                    return;
                }

                // 4. Enregistrement de la transaction
                transaction t = new transaction();
                t.setIdWalletSource(sourceWallet.getIdWallet());
                t.setIdWalletDestination(destWallet.getIdWallet());
                t.setMontant(currentCredit.getMontant());
                t.setCurrencyId(idTND);
                t.setDateTransaction(java.time.LocalDateTime.now());
                t.setType(typeTransaction.TRANSFERT);
                t.setStatut(StatutTransaction.Completed);

                new TransactionService().insertOne(t);

                new Alert(Alert.AlertType.INFORMATION, "Investissement réussi pour le projet : " + p.getTitle()).show();
                closeWindow();

            } catch (SQLException e) {
                new Alert(Alert.AlertType.ERROR, "Erreur de base de données : " + e.getMessage()).show();
            }
        }
    }

    private void setupAutoRefresh() {
        Timeline autoRefresh = new Timeline(new KeyFrame(Duration.seconds(3), event -> {
            wallet selected = comboWalletSelection.getValue();
            if (selected != null)
                updateTransactionUI(selected);
        }));
        autoRefresh.setCycleCount(Animation.INDEFINITE);
        autoRefresh.play();
    }

    public void setTransactionData(credit c, wallet w) {
        this.currentCredit = c;
        if (c != null) {
            lblMontant.setText(String.format("%.2f TND", c.getMontant()));
            lblDestinataire.setText("ID Projet : " + c.getProject_id());
        }
    }

    @FXML
    private void handleAnnuler() {
        closeWindow();
    }

    private void closeWindow() {
        if (confirmRoot.getScene() != null)
            ((Stage) confirmRoot.getScene().getWindow()).close();
    }
}