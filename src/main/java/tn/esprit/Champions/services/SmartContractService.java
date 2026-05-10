package tn.esprit.Champions.services;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.UnitValue;
import io.github.cdimascio.dotenv.Dotenv;
import jakarta.mail.*;
import jakarta.mail.Authenticator;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
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
    private final Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();

    // Récupération sécurisée depuis le fichier .env
    private final String SMTP_EMAIL = dotenv.get("SMTP_EMAIL");
    private final String SMTP_PASS = dotenv.get("SMTP_PASS");

    /**
     * Lance le processus de déploiement (Blockchain sim), génération PDF et notification Email.
     */
    public void deployAndNotify(int creditId, double montant, String projet, Utilisateur investisseur, double taux, int dureeMois) {
        new Thread(() -> {
            try {
                // 1. Simulation Blockchain (Appel API externe)
                JSONObject json = new JSONObject();
                json.put("creditId", creditId);
                json.put("investisseur", investisseur.getNom());

                RequestBody body = RequestBody.create(json.toString(), MediaType.get("application/json; charset=utf-8"));
                Request request = new Request.Builder().url("https://jsonplaceholder.typicode.com/posts").post(body).build();

                try (Response response = httpClient.newCall(request).execute()) {
                    if (response.isSuccessful()) {
                        String txHash = "0x" + UUID.randomUUID().toString().replace("-", "");

                        // 2. Génération du PDF avec iText 7
                        File pdfContrat = generateContractPDF(investisseur, projet, txHash, montant, taux, dureeMois);

                        // 3. Envoi de l'Email avec le contrat en pièce jointe
                        sendRealEmail(investisseur.getEmail(), projet, pdfContrat);

                        // 4. Affichage de l'interface graphique moderne
                        Platform.runLater(() -> showContractDetails(txHash, montant, projet, investisseur));
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.ERROR, "Erreur système : " + e.getMessage());
                    alert.show();
                });
            }
        }).start();
    }

    private File generateContractPDF(Utilisateur inv, String projet, String hash, double montant, double taux, int duree) throws Exception {
        File file = File.createTempFile("Contrat_Champions_", ".pdf");
        PdfWriter writer = new PdfWriter(file);
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf);

        document.add(new Paragraph("CERTIFICAT D'INVESTISSEMENT NUMÉRIQUE").setBold().setFontSize(18));
        document.add(new Paragraph("Transaction ID : " + hash).setFontSize(10).setItalic());

        document.add(new Paragraph("\nRécapitulatif de l'accord :"));
        Table table = new Table(UnitValue.createPercentArray(new float[]{1, 2})).useAllAvailableWidth();
        table.addCell("Investisseur :"); table.addCell(inv.getNom() + " " + inv.getPrenom());
        table.addCell("Projet :"); table.addCell(projet);
        table.addCell("Montant :"); table.addCell(montant + " TND");
        table.addCell("Taux annuel :"); table.addCell(taux + "%");
        table.addCell("Durée :"); table.addCell(duree + " mois");

        document.add(table);
        document.add(new Paragraph("\nValidé par le système intelligent de Champions Fintech."));
        document.close();
        return file;
    }

    private void sendRealEmail(String toEmail, String projet, File attachment) throws Exception {
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");
        props.put("mail.smtp.ssl.trust", "smtp.gmail.com");

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(SMTP_EMAIL, SMTP_PASS.replace(" ", ""));
            }
        });

        Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress(SMTP_EMAIL, "Champions Fintech"));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
        message.setSubject("📜 Votre Contrat est disponible - " + projet);

        MimeBodyPart textPart = new MimeBodyPart();
        textPart.setText("Félicitations ! Votre investissement a été traité. Veuillez trouver votre contrat en pièce jointe.");

        MimeBodyPart filePart = new MimeBodyPart();
        filePart.attachFile(attachment);

        Multipart multipart = new MimeMultipart();
        multipart.addBodyPart(textPart);
        multipart.addBodyPart(filePart);

        message.setContent(multipart);
        Transport.send(message);
        System.out.println("✅ Email envoyé avec succès !");
    }

    /**
     * Affiche la fenêtre de succès avec le design attractif.
     */
    private void showContractDetails(String hash, double montant, String projet, Utilisateur inv) {
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("✅ Champions Fintech - Opération Réussie");

        VBox root = new VBox();
        root.setSpacing(0);
        root.setAlignment(Pos.TOP_CENTER);
        root.setStyle("-fx-background-color: #0f172a; -fx-border-color: #38bdf8; -fx-border-width: 1; -fx-border-radius: 8;");

        // -- Header Bleu Foncé --
        VBox header = new VBox(10);
        header.setPadding(new Insets(30, 0, 30, 0));
        header.setAlignment(Pos.CENTER);
        header.setStyle("-fx-background-color: #1e293b;");

        Label icon = new Label("✔️");
        icon.setStyle("-fx-text-fill: #38bdf8; -fx-font-size: 40;");

        Label title = new Label("Investissement Réussi !");
        title.setStyle("-fx-text-fill: white; -fx-font-size: 18; -fx-font-weight: bold;");

        Label sub = new Label("Le contrat a été généré et envoyé par email.");
        sub.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 12;");

        header.getChildren().addAll(icon, title, sub);

        // -- Section Informations (Tableau) --
        VBox content = new VBox(15);
        content.setPadding(new Insets(30, 40, 30, 40));

        GridPane grid = new GridPane();
        grid.setHgap(20);
        grid.setVgap(15);

        grid.add(createLabel("Bénéficiaire :"), 0, 0);
        grid.add(createValue(inv.getNom() + " " + inv.getPrenom()), 1, 0);

        grid.add(createLabel("E-mail :"), 0, 1);
        grid.add(createValue(inv.getEmail(), "#38bdf8"), 1, 1);

        grid.add(createLabel("Montant :"), 0, 2);
        grid.add(createValue(String.format("%.2f TND", montant)), 1, 2);

        grid.add(createLabel("Blockchain Hash :"), 0, 3);
        String shortHash = hash.substring(0, 8) + "..." + hash.substring(hash.length()-8);
        Label hLabel = createValue(shortHash);
        hLabel.setStyle("-fx-text-fill: #64748b; -fx-font-size: 11; -fx-font-family: 'Courier New';");
        grid.add(hLabel, 1, 3);

        content.getChildren().add(grid);

        // -- Bouton Terminer --
        Button btn = new Button("Terminer l'opération");
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setStyle("-fx-background-color: #38bdf8; -fx-text-fill: #0f172a; -fx-font-weight: bold; -fx-padding: 12; -fx-cursor: hand; -fx-background-radius: 5;");
        btn.setOnAction(e -> stage.close());

        VBox footer = new VBox(btn);
        footer.setPadding(new Insets(0, 40, 30, 40));

        root.getChildren().addAll(header, content, footer);

        stage.setScene(new Scene(root, 480, 500));
        stage.show();
    }

    private Label createLabel(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 12;");
        return l;
    }

    private Label createValue(String text) {
        return createValue(text, "white");
    }

    private Label createValue(String text, String color) {
        Label l = new Label(text);
        l.setStyle("-fx-text-fill: " + color + "; -fx-font-weight: bold; -fx-font-size: 13;");
        return l;
    }
}