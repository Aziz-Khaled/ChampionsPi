package tn.esprit.Champions.gui.invest;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import tn.esprit.Champions.models.Negociation;
import tn.esprit.Champions.models.credit;
import tn.esprit.Champions.services.negociationService;

import java.io.IOException;
import java.sql.SQLException;

public class NegociationController {

    @FXML private VBox chatBox;
    @FXML private TextField txtMessage;
    @FXML private ScrollPane scrollChat;
    @FXML private Label lblMontantOrigine, lblTauxOrigine, lblDureeOrigine;

    private credit currentCredit;
    private final negociationService ns = new negociationService();

    @FXML
    public void initData(credit c) {
        this.currentCredit = c;
        if (c != null) {
            lblMontantOrigine.setText(String.format("%.2f TND", c.getMontant()));
            lblTauxOrigine.setText(c.getTaux() + " %");
            lblDureeOrigine.setText(c.getDuree() + " Mois");
        }
        ajouterBulle("Bonjour, je souhaite négocier les termes de ce crédit.", false);
    }

    @FXML
    private void envoyerMessage(ActionEvent event) {
        String msg = txtMessage.getText().trim();
        if (!msg.isEmpty()) {
            ajouterBulle(msg, true);
            txtMessage.clear();
            scrollChat.setVvalue(1.0);
        }
    }

    @FXML
    private void proposerOffre(ActionEvent event) {
        // Logique de proposition (utilisez un ID utilisateur qui existe en BDD)
        double nouveauTaux = 4.8;
        Negociation n = new Negociation();
        n.setCredit_id(currentCredit.getId());
        n.setInvestor_id(1); // <--- Vérifie cet ID dans ta table 'utilisateur'
        n.setMontant(currentCredit.getMontant());
        n.setTaux_propose(nouveauTaux);

        try {
            ns.insertOne(n);
            ajouterBulle("Proposition envoyée : " + nouveauTaux + "%", true);
        } catch (SQLException e) {
            System.err.println("Erreur BDD : " + e.getMessage());
        }
    }

    @FXML
    private void retourDetails(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/invest/DetailsCredit.fxml"));
            Parent root = loader.load();
            DetailsCreditController controller = loader.getController();
            controller.initData(currentCredit);
            chatBox.getScene().setRoot(root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void ajouterBulle(String texte, boolean isMe) {
        Label label = new Label(texte);
        label.setWrapText(true);
        label.setMaxWidth(350);
        label.setStyle(isMe ? "-fx-background-color: #4834D4; -fx-text-fill: white; -fx-padding: 10; -fx-background-radius: 15 15 0 15;"
                : "-fx-background-color: #DFF9FB; -fx-text-fill: #130F40; -fx-padding: 10; -fx-background-radius: 15 15 15 0;");
        HBox container = new HBox(label);
        container.setAlignment(isMe ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        chatBox.getChildren().add(container);
    }
}