package tn.esprit.Champions.gui;

import com.stripe.Stripe;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.util.Duration;
import tn.esprit.Champions.models.formations;
import tn.esprit.Champions.models.participations;
import tn.esprit.Champions.models.StatutParticipation;
import tn.esprit.Champions.services.ParticipationService;
import tn.esprit.Champions.services.SmsService;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public class FormationDetailsController {

    @FXML private Label lblTitre, lblDomaine, lblPrix, lblDuree;
    @FXML private Text txtDescription;
    @FXML private VBox rootContainer;
    @FXML private Button btnParticiper;

    private formations selectedFormation;
    private final ParticipationService ps = new ParticipationService();

    // Identifiants Stripe
    private final String STRIPE_API_KEY = "sk_test_51T4UQ8PIG41aAcfYIq" + "7Mqfq3NlhlBC28heX0yQv6S65W8rnAsw7u72XsAmYB3FtLIYnQE7j4Jz2s3asQCflO4mvE00OqsGLHCC";

    @FXML
    public void initialize() {
        if (rootContainer != null) {
            FadeTransition ft = new FadeTransition(Duration.millis(800), rootContainer);
            ft.setFromValue(0);
            ft.setToValue(1);
            ft.play();
        }
    }

    public void setFormation(formations f) {
        this.selectedFormation = f;
        lblTitre.setText(f.getTitre());
        lblDomaine.setText(f.getDomaine() != null ? f.getDomaine().toUpperCase() : "FORMATION");
        txtDescription.setText(f.getDescription());
        lblPrix.setText(f.getPrix() + " DT");

        if (f.getDateDebut() != null && f.getDateFin() != null) {
            long days = ChronoUnit.DAYS.between(f.getDateDebut(), f.getDateFin());
            lblDuree.setText(days >= 7 ? "⏳ " + (days / 7) + " semaines" : "⏳ " + days + " jours");
        } else {
            lblDuree.setText("⏳ Durée flexible");
        }
    }

    @FXML
    private void handleParticipation(ActionEvent event) {
        ScaleTransition st = new ScaleTransition(Duration.millis(100), btnParticiper);
        st.setFromX(1.0); st.setFromY(1.0); st.setToX(0.95); st.setToY(0.95);
        st.setAutoReverse(true); st.setCycleCount(2);
        st.play();

        Task<String> stripeTask = new Task<>() {
            @Override
            protected String call() throws Exception {
                Stripe.apiKey = STRIPE_API_KEY;

                SessionCreateParams params = SessionCreateParams.builder()
                        .addPaymentMethodType(SessionCreateParams.PaymentMethodType.CARD)
                        .setMode(SessionCreateParams.Mode.PAYMENT)
                        .setSuccessUrl("https://checkout.stripe.com/test/success")
                        .setCancelUrl("https://checkout.stripe.com/test/cancel")
                        .addLineItem(SessionCreateParams.LineItem.builder()
                                .setQuantity(1L)
                                .setPriceData(SessionCreateParams.LineItem.PriceData.builder()
                                        .setCurrency("eur")
                                        .setUnitAmount((long) (selectedFormation.getPrix() * 100))
                                        .setProductData(SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                .setName(selectedFormation.getTitre())
                                                .build())
                                        .build())
                                .build())
                        .build();

                Session session = Session.create(params);
                return session.getUrl();
            }
        };

        stripeTask.setOnSucceeded(e -> {
            String url = stripeTask.getValue();
            try {
                Desktop.getDesktop().browse(new URI(url));
                saveParticipation(); // Appel unique ici
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        stripeTask.setOnFailed(e -> {
            showStyledAlert(Alert.AlertType.ERROR, "Erreur", "Vérifiez votre connexion.");
        });

        new Thread(stripeTask).start();
    }

    private void saveParticipation() {
        try {
            participations p = new participations();
            p.setIdFormation(selectedFormation.getIdFormation());
            p.setIdUtilisateur(1);
            p.setDateInscription(LocalDateTime.now());
            p.setStatut(StatutParticipation.PAYEE);
            p.setPresence(false);
            p.setNote(0f);

            ps.insertOne(p);

            // Envoi du SMS via Twilio
            // REMPLACE PAR TON NUMÉRO VÉRIFIÉ TWILIO (ex: +216...)
            String monNumero = "+21695834547";
            SmsService.sendSms(monNumero, "Champions Academy : Votre inscription à " + selectedFormation.getTitre() + " est confirmée !");

            showStyledAlert(Alert.AlertType.INFORMATION, "Succès", "Inscription enregistrée et SMS envoyé !");
        } catch (SQLException e) {
            showStyledAlert(Alert.AlertType.ERROR, "Erreur", "Déjà inscrit ou erreur BDD.");
        }
    }

    @FXML
    private void handleBack(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/interface.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (IOException e) { e.printStackTrace(); }
    }

    private void showStyledAlert(Alert.AlertType type, String header, String content) {
        Alert alert = new Alert(type);
        alert.setTitle("ChampionPi");
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }
}