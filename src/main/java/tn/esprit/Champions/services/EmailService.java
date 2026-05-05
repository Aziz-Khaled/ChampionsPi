package tn.esprit.Champions.services;

import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import java.util.Properties;

public class EmailService {

    private static final String SMTP_HOST = "smtp.gmail.com";
    private static final String SMTP_PORT = "587";
    private static final String SENDER_EMAIL = "hedfialaa2@gmail.com";
    private static final String SENDER_PASSWORD = "cssmpbylhlkuldby"; // App Password

    public void sendEmail(String to, String subject, String body) {
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", SMTP_HOST);
        props.put("mail.smtp.port", SMTP_PORT);

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(SENDER_EMAIL, SENDER_PASSWORD);
            }
        });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(SENDER_EMAIL));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
            message.setSubject(subject);
            message.setText(body);

            Transport.send(message);
            System.out.println("Email sent successfully to: " + to);
        } catch (MessagingException e) {
            System.err.println("Error sending email: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void sendOrderConfirmation(String recipientEmail, String orderDetails, double totalAmount) {
        String subject = "Confirmation de votre commande - Fintech BTC";
        String body = "Bonjour,\n\n" +
                "Merci pour votre achat sur ChampionsPi BTC Marketplace.\n\n" +
                "Détails de la commande :\n" + orderDetails + "\n" +
                "Total : " + String.format("%.8f BTC", totalAmount) + "\n\n" +
                "Votre commande est en cours de traitement sur la Blockchain.\n" +
                "Cordialement,\n" +
                "L'équipe Fintech BTC";

        sendEmail(recipientEmail, subject, body);
    }
}
