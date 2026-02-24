package tn.esprit.Champions.gui.invest;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.Duration;
import tn.esprit.Champions.models.Negociation;
import tn.esprit.Champions.models.credit;
import tn.esprit.Champions.services.negociationService;

import java.io.IOException;
import java.sql.SQLException;

public class NegociationController {

    @FXML private HBox negocRoot;
    @FXML private VBox chatBox;
    @FXML private TextField txtMessage;
    @FXML private ScrollPane scrollChat;
    @FXML private Label lblMontantOrigine, lblTauxOrigine, lblDureeOrigine, lblStatutNegoc;

    private credit currentCredit;
    private final negociationService ns = new negociationService();

    @FXML
    public void initialize() {
        // Animation d'entrée de la page
        negocRoot.setOpacity(0);
        FadeTransition ft = new FadeTransition(Duration.millis(600), negocRoot);
        ft.setToValue(1.0);
        ft.play();
    }

    public void initData(credit c) {
        this.currentCredit = c;
        if (c != null) {
            lblMontantOrigine.setText(String.format("%.2f TND", c.getMontant()));
            lblTauxOrigine.setText(String.format("%.1f %%", c.getTaux()));
            lblDureeOrigine.setText(c.getDuree() + " Mois");
        }

        // Petit délai pour simuler l'arrivée du porteur de projet
        PauseTransition pause = new PauseTransition(Duration.seconds(0.5));
        pause.setOnFinished(e -> ajouterBulle("Bonjour ! Je suis le porteur du projet. Souhaitez-vous discuter des modalités ?", false));
        pause.play();
    }

    @FXML
    private void envoyerMessage(ActionEvent event) {
        String msg = txtMessage.getText().trim();
        if (!msg.isEmpty()) {
            ajouterBulle(msg, true);
            txtMessage.clear();
        }
    }

    @FXML
    private void proposerOffre(ActionEvent event) {
        // Création d'une boîte de dialogue stylisée pour demander le taux
        TextInputDialog dialog = new TextInputDialog(String.valueOf(currentCredit.getTaux()));
        dialog.setTitle("Nouvelle Proposition");
        dialog.setHeaderText("Proposer un nouveau taux d'intérêt");
        dialog.setContentText("Taux (%) :");

        dialog.showAndWait().ifPresent(tauxStr -> {
            try {
                double nouveauTaux = Double.parseDouble(tauxStr);
                Negociation n = new Negociation();
                n.setCredit_id(currentCredit.getId());
                n.setInvestor_id(1); // À lier à la session utilisateur
                n.setMontant(currentCredit.getMontant());
                n.setTaux_propose(nouveauTaux);

                ns.insertOne(n);
                ajouterBulle("💼 NOUVELLE OFFRE : Je propose un taux de " + nouveauTaux + "%", true);
                lblStatutNegoc.setText("OFFRE ENVOYÉE");

            } catch (NumberFormatException | SQLException e) {
                System.err.println("Erreur proposition : " + e.getMessage());
            }
        });
    }

    @FXML
    private void retourDetails(ActionEvent event) {
        try {
            // Animation de sortie
            FadeTransition ft = new FadeTransition(Duration.millis(300), negocRoot);
            ft.setToValue(0);
            ft.setOnFinished(e -> {
                try {
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/invest/DetailsCredit.fxml"));
                    Parent root = loader.load();
                    DetailsCreditController controller = loader.getController();
                    controller.initData(currentCredit);
                    negocRoot.getScene().setRoot(root);
                } catch (IOException ex) { ex.printStackTrace(); }
            });
            ft.play();
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void ajouterBulle(String texte, boolean isMe) {
        Label label = new Label(texte);
        label.setWrapText(true);
        label.setMaxWidth(400);

        // Application du style Premium Dark
        if (isMe) {
            label.setStyle("-fx-background-color: #38bdf8; -fx-text-fill: #020617; -fx-padding: 12 18; -fx-background-radius: 20 20 0 20; -fx-font-size: 14px; -fx-font-weight: 500;");
        } else {
            label.setStyle("-fx-background-color: #1e293b; -fx-text-fill: white; -fx-padding: 12 18; -fx-background-radius: 20 20 20 0; -fx-font-size: 14px; -fx-border-color: rgba(255,255,255,0.05); -fx-border-radius: 20 20 20 0;");
        }

        HBox container = new HBox(label);
        container.setAlignment(isMe ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        container.setPadding(new javafx.geometry.Insets(5, 0, 5, 0));

        // --- ANIMATION DE POP ---
        container.setOpacity(0);
        container.setScaleX(0.8);
        container.setScaleY(0.8);
        container.setTranslateY(10);

        chatBox.getChildren().add(container);

        // Animation combinée (Fade + Scale + Move)
        FadeTransition ft = new FadeTransition(Duration.millis(300), container);
        ft.setToValue(1.0);

        ScaleTransition st = new ScaleTransition(Duration.millis(300), container);
        st.setToX(1.0); st.setToY(1.0);

        TranslateTransition tt = new TranslateTransition(Duration.millis(300), container);
        tt.setToY(0);

        ParallelTransition pt = new ParallelTransition(ft, st, tt);
        pt.play();

        // Scroll automatique vers le bas
        Platform.runLater(() -> scrollChat.setVvalue(1.0));
    }
}