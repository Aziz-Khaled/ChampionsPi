package tn.esprit.Champions.services;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.UnitValue;
import io.github.cdimascio.dotenv.Dotenv;
import jakarta.mail.*;
import jakarta.mail.Authenticator;
import jakarta.mail.internet.*;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import okhttp3.*;
import org.json.JSONObject;
import tn.esprit.Champions.models.Utilisateur;

import java.io.File;
import java.util.Properties;
import java.util.UUID;

public class SmartContractService {

    private final OkHttpClient httpClient = new OkHttpClient();
    private final Dotenv dotenv = Dotenv.load(); // Charge le fichier .env

    // === CONFIGURATION RÉCUPÉRÉE DEPUIS .ENV ===
    private final String TWILIO_SID = dotenv.get("TWILIO_SID");
    private final String TWILIO_TOKEN = dotenv.get("TWILIO_TOKEN");
    private final String TWILIO_PHONE = dotenv.get("TWILIO_PHONE");

    private final String SMTP_EMAIL = dotenv.get("SMTP_EMAIL");
    private final String SMTP_PASS = dotenv.get("SMTP_PASS");
    // ==========================================

    public void deployAndNotify(int creditId, double montant, String projet, Utilisateur investisseur, double taux, int dureeMois) {
        new Thread(() -> {
            try {
                JSONObject json = new JSONObject();
                json.put("creditId", creditId);
                json.put("investisseur", investisseur.getNom());

                RequestBody body = RequestBody.create(json.toString(), MediaType.get("application/json; charset=utf-8"));
                Request request = new Request.Builder().url("https://jsonplaceholder.typicode.com/posts").post(body).build();

                try (Response response = httpClient.newCall(request).execute()) {
                    if (response.isSuccessful()) {
                        String txHash = "0x" + UUID.randomUUID().toString().replace("-", "");

                        File pdfContrat = generateContractPDF(investisseur, projet, txHash, montant, taux, dureeMois);

                        // Envois sécurisés
                        sendRealSms(investisseur.getTelephone(), investisseur.getNom(), projet, txHash);
                        sendRealEmail(investisseur.getEmail(), projet, pdfContrat);

                        Platform.runLater(() -> showContractDetails(txHash, montant, projet, investisseur, taux, dureeMois));
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.ERROR, "Erreur lors du déploiement : " + e.getMessage());
                    alert.show();
                });
            }
        }).start();
    }

    private File generateContractPDF(Utilisateur inv, String projet, String hash, double montant, double taux, int duree) throws Exception {
        File file = File.createTempFile("Contrat_" + inv.getNom() + "_", ".pdf");
        PdfWriter writer = new PdfWriter(file);
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf);

        document.add(new Paragraph("CERTIFICAT D'INVESTISSEMENT NUMÉRIQUE").setBold().setFontSize(18));
        document.add(new Paragraph("Validé par Blockchain - Transaction : " + hash).setFontSize(10).setItalic());

        document.add(new Paragraph("\nDétails de l'accord :"));
        Table table = new Table(UnitValue.createPercentArray(new float[]{1, 2})).useAllAvailableWidth();
        table.addCell("Investisseur :"); table.addCell(inv.getNom() + " " + inv.getPrenom());
        table.addCell("Projet :"); table.addCell(projet);
        table.addCell("Capital :"); table.addCell(montant + " TND");
        table.addCell("Taux proposé :"); table.addCell(taux + "%");
        table.addCell("Durée :"); table.addCell(duree + " Mois");

        document.add(table);
        document.add(new Paragraph("\nCe document certifie l'engagement financier sur la plateforme Champions."));

        document.close();
        return file;
    }

    private void sendRealSms(String phone, String nom, String projet, String hash) {
        try {
            Twilio.init(TWILIO_SID.trim(), TWILIO_TOKEN.trim());
            String formattedDest = (phone.startsWith("+")) ? phone : "+216" + phone;

            Message.creator(
                    new PhoneNumber(formattedDest),
                    new PhoneNumber(TWILIO_PHONE.trim()),
                    "Champions: " + nom + ", votre investissement pour '" + projet + "' est validé. Hash: " + hash.substring(0,8)
            ).create();
            System.out.println("SMS envoyé avec succès !");
        } catch (Exception e) {
            System.err.println("Erreur SMS Twilio : " + e.getMessage());
        }
    }

    private void sendRealEmail(String toEmail, String projet, File attachment) throws Exception {
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(SMTP_EMAIL, SMTP_PASS.trim());
            }
        });

        jakarta.mail.Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress(SMTP_EMAIL, "Champions Fintech"));
        message.setRecipients(jakarta.mail.Message.RecipientType.TO, InternetAddress.parse(toEmail));
        message.setSubject("📜 Votre Contrat Validé - " + projet);

        MimeBodyPart textPart = new MimeBodyPart();
        textPart.setText("Félicitations ! Trouvez ci-joint votre certificat d'investissement.");

        MimeBodyPart filePart = new MimeBodyPart();
        filePart.attachFile(attachment);

        Multipart multipart = new MimeMultipart();
        multipart.addBodyPart(textPart);
        multipart.addBodyPart(filePart);

        message.setContent(multipart);
        Transport.send(message);
        System.out.println("Email envoyé avec succès !");
    }

    private void showContractDetails(String hash, double montant, String projet, Utilisateur inv, double taux, int duree) {
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("📜 Signature du Smart Contract");

        VBox root = new VBox(20);
        root.setPadding(new Insets(25));
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-color: #0f172a; -fx-border-color: #38bdf8; -fx-border-width: 2;");

        Label title = new Label("INVESTISSEMENT SÉCURISÉ");
        title.setStyle("-fx-text-fill: #38bdf8; -fx-font-size: 18; -fx-font-weight: bold;");

        VBox details = new VBox(10);
        details.setPadding(new Insets(15));
        details.setStyle("-fx-background-color: #1e293b; -fx-background-radius: 10;");
        details.getChildren().addAll(
                createDataRow("🔗 Blockchain Hash", hash.substring(0, 15) + "..."),
                createDataRow("👤 Investisseur", inv.getNom() + " " + inv.getPrenom()),
                createDataRow("💰 Capital", montant + " TND")
        );

        Button btnClose = new Button("Fermer");
        btnClose.setStyle("-fx-background-color: #38bdf8; -fx-text-fill: #0f172a; -fx-font-weight: bold;");
        btnClose.setOnAction(e -> stage.close());

        root.getChildren().addAll(title, details, btnClose);
        stage.setScene(new Scene(root, 450, 400));
        stage.show();
    }

    private HBox createDataRow(String label, String value) {
        HBox row = new HBox(10);
        Label l = new Label(label + " :"); l.setStyle("-fx-text-fill: #94a3b8; -fx-min-width: 150;");
        Label v = new Label(value); v.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");
        row.getChildren().addAll(l, v);
        return row;
    }
}