package tn.esprit.Champions.services;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;

import java.io.*;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;

import static com.fasterxml.jackson.databind.type.LogicalType.DateTime;
import static com.itextpdf.kernel.pdf.PdfName.Event;

public class GoogleCalendarService {
    private static final String APPLICATION_NAME = "Champions Academy";
    private static final String TOKENS_DIRECTORY_PATH = "tokens";

    public static void addFormationEvent(String titre, String desc, LocalDateTime start) {
        try {
            final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();

            // 1. Charger les credentials depuis resources
            InputStream in = GoogleCalendarService.class.getResourceAsStream("/credentials.json");
            if (in == null) {
                System.err.println("ERREUR : Fichier credentials.json introuvable dans src/main/resources/");
                return;
            }

            GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(
                    GsonFactory.getDefaultInstance(), new InputStreamReader(in));

            // 2. Configurer OAuth2
            GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                    HTTP_TRANSPORT, GsonFactory.getDefaultInstance(), clientSecrets,
                    Collections.singletonList(CalendarScopes.CALENDAR_EVENTS))
                    .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(TOKENS_DIRECTORY_PATH)))
                    .setAccessType("offline")
                    .build();

            // 3. Demander l'autorisation (Ouvre le navigateur)
            Credential credential = new AuthorizationCodeInstalledApp(flow, new LocalServerReceiver()).authorize("user");

            // 4. Créer le client API Google Calendar
            Calendar service = new Calendar.Builder(HTTP_TRANSPORT, GsonFactory.getDefaultInstance(), credential)
                    .setApplicationName(APPLICATION_NAME)
                    .build();

            // 5. Configurer l'événement
            Event event = new Event()
                    .setSummary("🎓 " + titre)
                    .setDescription(desc);

            // Conversion de la date
            DateTime startDT = new DateTime(start.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
            event.setStart(new EventDateTime().setDateTime(startDT));

            // Fin de l'événement (+1 heure)
            event.setEnd(new EventDateTime().setDateTime(new DateTime(startDT.getValue() + 3600000)));

            // 6. Envoyer à Google
            service.events().insert("primary", event).execute();
            System.out.println("LOG : Événement Google Calendar créé avec succès !");

        } catch (Exception e) {
            System.err.println("Erreur Google Calendar : " + e.getMessage());
            e.printStackTrace();
        }
    }
}