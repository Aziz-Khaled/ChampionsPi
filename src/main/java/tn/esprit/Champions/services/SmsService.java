package tn.esprit.Champions.services;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;

public class SmsService {
    // Tes identifiants Twilio
    public static final String ACCOUNT_SID = "AC8654e2475ba0e6" + "5a73abe048b807cbf7";
    public static final String AUTH_TOKEN = "1829454b43d9d7c3d9" + "257cf171079beb";
    public static final String TWILIO_NUMBER = "+17742142663";

    public static void sendSms(String toPhoneNumber, String content) {
        try {
            Twilio.init(ACCOUNT_SID, AUTH_TOKEN);
            Message message = Message.creator(
                            new PhoneNumber(toPhoneNumber), // Numéro du destinataire
                            new PhoneNumber(TWILIO_NUMBER), // Ton numéro Twilio
                            content)
                    .create();

            System.out.println("SMS envoyé avec succès ! SID: " + message.getSid());
        } catch (Exception e) {
            System.err.println("Erreur Twilio : " + e.getMessage());
        }
    }
}