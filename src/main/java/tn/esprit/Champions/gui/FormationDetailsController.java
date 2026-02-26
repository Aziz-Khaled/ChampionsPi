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
                                                .setDescription("Paiement converti via Champions Academy")
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
                alertWait.setHeaderText("Confirmation requise");
                alertWait.setContentText("Après avoir payé dans votre navigateur, cliquez sur 'J'ai payé'.");
                ButtonType btnConfirm = new ButtonType("J'ai payé");
                ButtonType btnCancel = new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);
                alertWait.getButtonTypes().setAll(btnConfirm, btnCancel);

                Optional<ButtonType> result = alertWait.showAndWait();
                if (result.isPresent() && result.get() == btnConfirm) {
                    saveParticipation();
                }
            } catch (Exception ex) { ex.printStackTrace(); }
        });
        stripeTask.setOnFailed(e -> showStyledAlert(Alert.AlertType.ERROR, "Erreur", "Problème avec Stripe."));
        new Thread(stripeTask).start();
    }

    private void saveParticipation() {
        try {
            System.out.println("--- LOG : Exécution des Services ---");
            participations p = new participations();
            p.setIdFormation(selectedFormation.getIdFormation());
            p.setIdUtilisateur(1);
            p.setDateInscription(LocalDateTime.now());
            p.setStatut(StatutParticipation.PAYEE);
            p.setPresence(false);
            p.setNote(0.0f);
            ps.insertOne(p);

            String fileName = "Recu_" + System.currentTimeMillis() + ".pdf";
            PdfService.genererRecuPaiement("Etudiant", selectedFormation.getTitre(), selectedFormation.getPrix(), fileName);
            File pdfFile = new File(fileName);
            Thread.sleep(1500);

            String urlCloudinary = CloudinaryService.uploadFile(pdfFile);

            LocalDateTime dateDebutCalendar;
            if (selectedFormation.getDateDebut() != null) {
                dateDebutCalendar = selectedFormation.getDateDebut().atTime(9, 0);
            } else {
                dateDebutCalendar = LocalDateTime.now().plusDays(1).withHour(9).withMinute(0);
            }

            GoogleCalendarService.addFormationEvent(selectedFormation.getTitre(), selectedFormation.getDescription(), dateDebutCalendar);

            if (urlCloudinary != null && !urlCloudinary.isEmpty()) {
                String finalUrl = urlCloudinary.replace("http://", "https://");
                String messageSms = "Champions: Inscription OK. Agenda mis a jour. Recu: " + finalUrl;
                SmsService.sendSms("+21695834547", messageSms);
                showStyledAlert(Alert.AlertType.INFORMATION, "Succès", "Inscription validée !\n- Agenda Google mis à jour\n- Reçu envoyé par SMS");
            }
            if (pdfFile.exists()) pdfFile.delete();
        } catch (SQLException e) {
            showStyledAlert(Alert.AlertType.ERROR, "Erreur", "Vous êtes déjà inscrit.");
        } catch (Exception ex) {
            ex.printStackTrace();
            showStyledAlert(Alert.AlertType.ERROR, "Erreur Système", "Erreur lors des notifications.");
        }
    }

    @FXML
    private void handleBack(ActionEvent event) {
        try {
            // Tentative avec le chemin complet du package
            URL resource = getClass().getResource("/tn/esprit/Champions/gui/student_dashboard.fxml");

            // Si non trouvé (souvent dû à la structure Maven), tentative à la racine des ressources
            if (resource == null) {
                resource = getClass().getResource("/student_dashboard.fxml");
            }

            if (resource == null) {
                throw new IOException("Fichier FXML introuvable : student_dashboard.fxml");
            }

            Parent root = FXMLLoader.load(resource);
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            Scene scene = new Scene(root);

            FadeTransition ft = new FadeTransition(Duration.millis(500), root);
            ft.setFromValue(0); ft.setToValue(1); ft.play();

            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            System.err.println("Erreur chargement FXML : " + e.getMessage());
            showStyledAlert(Alert.AlertType.ERROR, "Erreur Navigation", "Impossible de charger le dashboard.");
        }
    }
    @FXML
    private void handleOpenChat(ActionEvent event) {
        // 1. Créer une fenêtre de saisie
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Champions AI Assistant");
        dialog.setHeaderText("Posez votre question sur : " + selectedFormation.getTitre());
        dialog.setContentText("Votre message :");

        // 2. Récupérer la question et appeler l'IA
        Optional<String> result = dialog.showAndWait();
        if (result.isPresent() && !result.get().trim().isEmpty()) {

            // On affiche une alerte de chargement
            Alert loadingAlert = new Alert(Alert.AlertType.INFORMATION);
            loadingAlert.setTitle("IA en réflexion");
            loadingAlert.setHeaderText(null);
            loadingAlert.setContentText("L'IA prépare votre réponse... Veuillez patienter.");
            loadingAlert.show();

            Task<String> task = new Task<>() {
                @Override
                protected String call() throws Exception {
                    // Appel au service OpenAI
                    return ChatbotService.askQuestion(result.get(), selectedFormation.getTitre());
                }
            };

            task.setOnSucceeded(e -> {
                loadingAlert.close();
                // Affichage de la réponse finale
                Alert responseAlert = new Alert(Alert.AlertType.INFORMATION);
                responseAlert.setTitle("Réponse Champions AI");
                responseAlert.setHeaderText("Voici la réponse pour la formation " + selectedFormation.getTitre());
                responseAlert.setContentText(task.getValue());
                responseAlert.getDialogPane().setMinWidth(500);
                responseAlert.showAndWait();
            });

            task.setOnFailed(e -> {
                loadingAlert.close();
                showStyledAlert(Alert.AlertType.ERROR, "Erreur AI", "Impossible de contacter l'IA. Vérifiez votre clé API.");
            });

            new Thread(task).start();
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