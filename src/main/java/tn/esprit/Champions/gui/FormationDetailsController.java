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
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.util.Duration;
import tn.esprit.Champions.models.formations;
import tn.esprit.Champions.models.participations;
import tn.esprit.Champions.models.StatutParticipation;
import tn.esprit.Champions.services.*;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

public class FormationDetailsController {

    @FXML private Label lblTitre, lblDomaine, lblPrix, lblDuree;
    @FXML private Text txtDescription;
    @FXML private VBox rootContainer;
    @FXML private Button btnParticiper;

    private formations selectedFormation;
    private final ParticipationService ps = new ParticipationService();

    // NOUVEAU : On utilise l'instance du nouveau service Gemini
    private final GeminiService geminiService = new GeminiService();

    private final String STRIPE_API_KEY = "sk_test_51T4UQ8PIG41aAcfYIq..."; // Garde ta clé actuelle

    @FXML
    public void initialize() {
        if (rootContainer != null) {
            FadeTransition ft = new FadeTransition(Duration.millis(800), rootContainer);
            ft.setFromValue(0); ft.setToValue(1);
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
    private void handleOpenChat(ActionEvent event) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Champions AI Assistant (Gemini)");
        dialog.setHeaderText("Posez votre question sur : " + selectedFormation.getTitre());
        dialog.setContentText("Votre message :");

        Optional<String> result = dialog.showAndWait();
        if (result.isPresent() && !result.get().trim().isEmpty()) {

            // Alerte de chargement
            Alert loadingAlert = new Alert(Alert.AlertType.INFORMATION);
            loadingAlert.setTitle("Gemini en réflexion");
            loadingAlert.setHeaderText(null);
            loadingAlert.setContentText("L'IA de Google prépare votre réponse... Veuillez patienter.");
            loadingAlert.show();

            Task<String> task = new Task<>() {
                @Override
                protected String call() throws Exception {
                    // Construction d'un prompt contextuel pour Gemini
                    String prompt = "Tu es l'assistant de Champions Academy. L'utilisateur pose une question sur la formation '"
                            + selectedFormation.getTitre() + "'. Voici la question : " + result.get();

                    // Appel au nouveau service utilisant le SDK VertexAI/GoogleCloud
                    return geminiService.askGemini(prompt);
                }
            };

            task.setOnSucceeded(e -> {
                loadingAlert.close();
                Alert responseAlert = new Alert(Alert.AlertType.INFORMATION);
                responseAlert.setTitle("Réponse Champions AI");
                responseAlert.setHeaderText("Analyse Gemini terminée");

                // On utilise un TextArea pour que les longs textes soient lisibles
                TextArea textArea = new TextArea(task.getValue());
                textArea.setEditable(false);
                textArea.setWrapText(true);
                textArea.setPrefHeight(300);

                responseAlert.getDialogPane().setContent(textArea);
                responseAlert.showAndWait();
            });

            task.setOnFailed(e -> {
                loadingAlert.close();
                showStyledAlert(Alert.AlertType.ERROR, "Erreur Gemini", "Impossible de joindre le service Google AI.");
            });

            new Thread(task).start();
        }
    }

    // --- Reste du code (Stripe, SaveParticipation, handleBack) identique ---

    @FXML
    private void handleParticipation(ActionEvent event) {
        // ... (Ton code Stripe reste tel quel)
        ScaleTransition st = new ScaleTransition(Duration.millis(100), btnParticiper);
        st.setFromX(1.0); st.setFromY(1.0); st.setToX(0.95); st.setToY(0.95);
        st.setAutoReverse(true); st.setCycleCount(2);
        st.play();

        Task<String> stripeTask = new Task<>() {
            @Override
            protected String call() throws Exception {
                Stripe.apiKey = STRIPE_API_KEY;
                double prixEnEur = CurrencyService.convertTndToEur(selectedFormation.getPrix());
                SessionCreateParams params = SessionCreateParams.builder()
                        .addPaymentMethodType(SessionCreateParams.PaymentMethodType.CARD)
                        .setMode(SessionCreateParams.Mode.PAYMENT)
                        .setSuccessUrl("https://www.google.com")
                        .setCancelUrl("https://www.google.com")
                        .addLineItem(SessionCreateParams.LineItem.builder()
                                .setQuantity(1L)
                                .setPriceData(SessionCreateParams.LineItem.PriceData.builder()
                                        .setCurrency("eur")
                                        .setUnitAmount((long) (prixEnEur * 100))
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
            try {
                Desktop.getDesktop().browse(new URI(stripeTask.getValue()));
                Alert alertWait = new Alert(Alert.AlertType.CONFIRMATION);
                alertWait.setTitle("Paiement");
                alertWait.setContentText("Après avoir payé, cliquez sur 'J'ai payé'.");
                ButtonType btnConfirm = new ButtonType("J'ai payé");
                ButtonType btnCancel = new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);
                alertWait.getButtonTypes().setAll(btnConfirm, btnCancel);

                Optional<ButtonType> res = alertWait.showAndWait();
                if (res.isPresent() && res.get() == btnConfirm) saveParticipation();
            } catch (Exception ex) { ex.printStackTrace(); }
        });
        new Thread(stripeTask).start();
    }

    private void saveParticipation() {
        try {
            participations p = new participations();
            p.setIdFormation(selectedFormation.getIdFormation());
            p.setIdUtilisateur(1); // À lier avec le module de ton ami plus tard
            p.setDateInscription(LocalDateTime.now());
            p.setStatut(StatutParticipation.PAYEE);
            ps.insertOne(p);

            String fileName = "Recu_" + System.currentTimeMillis() + ".pdf";
            PdfService.genererRecuPaiement("Etudiant", selectedFormation.getTitre(), selectedFormation.getPrix(), fileName);
            File pdfFile = new File(fileName);

            // Notification Services
            CloudinaryService.uploadFile(pdfFile);
            GoogleCalendarService.addFormationEvent(selectedFormation.getTitre(), selectedFormation.getDescription(), LocalDateTime.now().plusDays(1));
            SmsService.sendSms("+21695834547", "Champions: Inscription OK pour " + selectedFormation.getTitre());

            showStyledAlert(Alert.AlertType.INFORMATION, "Succès", "Inscription validée !");
            if (pdfFile.exists()) pdfFile.delete();
        } catch (Exception e) {
            showStyledAlert(Alert.AlertType.ERROR, "Erreur", "Problème lors de l'inscription.");
        }
    }

    @FXML
    private void handleBack(ActionEvent event) {
        try {
            // Teste d'abord la racine, puis le package complet
            URL resource = getClass().getResource("/student_dashboard.fxml");
            if (resource == null) {
                resource = getClass().getResource("/tn/esprit/Champions/gui/student_dashboard.fxml");
            }

            if (resource == null) {
                showStyledAlert(Alert.AlertType.ERROR, "Erreur", "Le fichier student_dashboard.fxml est introuvable.");
                return;
            }

            Parent root = FXMLLoader.load(resource);
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            System.err.println("Erreur de navigation : " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void showStyledAlert(Alert.AlertType type, String header, String content) {
        Alert alert = new Alert(type);
        alert.setTitle("Champions Academy");
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }
}