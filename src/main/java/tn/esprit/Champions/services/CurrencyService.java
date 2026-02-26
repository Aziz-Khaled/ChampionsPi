package tn.esprit.Champions.services;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.json.JSONObject;

public class CurrencyService {
    // API gratuite et simple sans clé nécessaire pour les taux de base
    private static final String API_URL = "https://open.er-api.com/v6/latest/TND";

    public static double convertTndToEur(double amountInTnd) {
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            JSONObject json = new JSONObject(response.body());
            double rate = json.getJSONObject("rates").getDouble("EUR");

            return amountInTnd * rate;
        } catch (Exception e) {
            System.err.println("Erreur API Conversion : " + e.getMessage());
            return amountInTnd * 0.30; // Valeur de secours si l'API échoue (1 TND ≈ 0.30 EUR)
        }
    }
}