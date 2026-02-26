package tn.esprit.Champions.services;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class GroqService {
    // Ta clé Groq
    private static final String API_KEY = "";
    private static final String API_URL = "https://api.groq.com/openai/v1/chat/completions";

    private final HttpClient client;

    public GroqService() {
        this.client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(20))
                .build();
    }

    public String askAI(String userMessage, String context) {
        try {
            // Modèle mis à jour : llama-3.3-70b-versatile
            String jsonPayload = "{"
                    + "\"model\": \"llama-3.3-70b-versatile\","
                    + "\"messages\": ["
                    + "  {\"role\": \"system\", \"content\": \"Tu es l'expert financier de Champions Fintech.\"},"
                    + "  {\"role\": \"user\", \"content\": \"Contexte: " + jsonEscape(context) + ". Question: " + jsonEscape(userMessage) + "\"}"
                    + "]"
                    + "}";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + API_KEY)
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return extractText(response.body());
            } else {
                // Debug pour voir exactement l'erreur dans la console Java
                System.out.println("DEBUG GROQ ERROR: " + response.body());
                return "Erreur Groq (" + response.statusCode() + ").";
            }
        } catch (Exception e) {
            return "Erreur de connexion : " + e.getMessage();
        }
    }

    private String extractText(String json) {
        try {
            String token = "\"content\":\"";
            int start = json.indexOf(token) + token.length();
            int end = json.indexOf("\"", start);
            return json.substring(start, end)
                    .replace("\\n", "\n")
                    .replace("\\\"", "\"")
                    .trim();
        } catch (Exception e) {
            return "Erreur de lecture du message.";
        }
    }

    private String jsonEscape(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ");
    }
}