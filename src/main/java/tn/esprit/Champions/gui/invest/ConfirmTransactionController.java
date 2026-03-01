package tn.esprit.Champions.gui.invest;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import tn.esprit.Champions.models.credit;
import tn.esprit.Champions.models.wallet;

public class ConfirmTransactionController {

    @FXML private Label lblMontant, lblDestinataire, lblSoldeActuel, lblNouveauSolde, lblRibSource;
    @FXML private Button btnConfirmer;

    private credit currentCredit;
    private wallet investorWallet;

    /**
     * Cette méthode sera appelée par DetailsCreditController.
     * Pour l'instant, elle simule l'affichage sans services.
     */
    public void setTransactionData(credit c, wallet w) {
        this.currentCredit = c;
        this.investorWallet = w;

        // 1. Affichage statique des montants
        lblMontant.setText(String.format("%.2f TND", c.getMontant()));

        // 2. Simulation des infos du Wallet (Données statiques pour le test)
        lblRibSource.setText("RIB: " + (w != null ? w.getRib() : "87654321"));
        lblSoldeActuel.setText(String.format("Solde actuel: %.2f TND", (w != null ? w.getSolde() : 25000.0)));

        // 3. Simulation du destinataire
        lblDestinataire.setText("Bénéficiaire: Projet #" + c.getProject_id());

        // 4. Calcul du reste
        double soldeInitial = (w != null ? w.getSolde() : 25000.0);
        double nouveauSolde = soldeInitial - c.getMontant();
        lblNouveauSolde.setText(String.format("Solde après transfert: %.2f TND", nouveauSolde));

        // 5. Logique de blocage statique
        if (nouveauSolde < 0) {
            btnConfirmer.setDisable(true);
            btnConfirmer.setText("SOLDE INSUFFISANT");
            lblNouveauSolde.setStyle("-fx-text-fill: #ff4d4d;");
        }
    }

    @FXML
    private void handleConfirmation() {
        // --- ZONE D'INTÉGRATION FUTURE ---
        // Ici, vous ajouterez :
        // TransactionService ts = new TransactionService();
        // ts.insertOne(t);

        System.out.println("Simulation : Transaction envoyée vers la Blockchain pour le montant de " + lblMontant.getText());

        // Simulation d'un succès visuel
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Simulation Réussie");
        alert.setHeaderText("Intégration prête !");
        alert.setContentText("L'interface est connectée. Il suffira d'appeler le TransactionService ici.");
        alert.showAndWait();

        closeWindow();
    }

    @FXML
    private void handleAnnuler() {
        closeWindow();
    }

    private void closeWindow() {
        Stage stage = (Stage) lblMontant.getScene().getWindow();
        stage.close();
    }
}