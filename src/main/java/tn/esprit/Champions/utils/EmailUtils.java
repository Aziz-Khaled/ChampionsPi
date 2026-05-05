package tn.esprit.Champions.utils;
import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.util.Properties;
public class EmailUtils {
    private static final String SENDER_EMAIL = "azizkhaled108@gmail.com"; // Your email
    private static final String APP_PASSWORD = "vqvximxyrymfuqdf"; // Your App Password

    public static void sendOTP(String recipientEmail, String otpCode) {
        // SMTP Server Settings
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");

        // Create Session
        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(SENDER_EMAIL, APP_PASSWORD);
            }
        });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(SENDER_EMAIL));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipientEmail));
            message.setSubject("Champions Platform - Verification Code");

            // Stylish HTML Content
            String htmlContent = "<div style='font-family: Arial, sans-serif; border: 1px solid #ddd; padding: 20px;'>"
                    + "<h2 style='color: #2E5BFF;'>Welcome to Champions!</h2>"
                    + "<p>Use the following code to verify your email address:</p>"
                    + "<h1 style='letter-spacing: 5px; color: #333;'>" + otpCode + "</h1>"
                    + "<p>This code will expire in 10 minutes.</p>"
                    + "</div>";

            message.setContent(htmlContent, "text/html");

            // Send in a separate thread so the UI doesn't freeze
            new Thread(() -> {
                try {
                    Transport.send(message);
                    System.out.println("Email sent successfully to " + recipientEmail);
                } catch (MessagingException e) {
                    e.printStackTrace();
                }
            }).start();

        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }
}
