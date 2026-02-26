package tn.esprit.Champions.services;

import org.json.JSONObject;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class ChatbotService {
    private static final String API_KEY = "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";
    private static final String GROQ_URL = "https://api.groq.com/openai/v1/chat/completions";

    public static String askQuestion(String userMessage, String formationTitre) {
        try {
            // 1. TENTATIVE AVEC LA VRAIE IA (GROQ)
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(3)) // On attend max 3 sec
                    .build();

            JSONObject body = new JSONObject();
            body.put("model", "llama3-8b-8192");
            body.put("messages", new org.json.JSONArray()
                    .put(new JSONObject().put("role", "system").put("content", "Réponds en une phrase courte en français sur : " + formationTitre))
                    .put(new JSONObject().put("role", "user").put("content", userMessage)));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(GROQ_URL))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + API_KEY)
                    .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JSONObject jsonResponse = new JSONObject(response.body());
                return jsonResponse.getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content");
            }

            // Si le code n'est pas 200, on déclenche l'alternative
            throw new Exception("Fallback");

        } catch (Exception e) {
            // 2. PLAN B : SIMULATION INTELLIGENTE (Si l'IA échoue)
            String msg = userMessage.toLowerCase();
            if (msg.contains("prix") || msg.contains("dt"))
                return "La formation '" + formationTitre + "' est proposée au tarif indiqué sur l'affiche. Des bourses sont disponibles pour les étudiants méritants.";
            if (msg.contains("durée") || msg.contains("temps"))
                return "Ce programme s'étale sur plusieurs semaines avec un rythme intensif pour garantir votre réussite professionnelle.";

            return "En tant qu'assistant Champions Academy, je vous confirme que la formation '" + formationTitre + "' est un excellent choix pour votre avenir. Souhaitez-vous vous inscrire ?";
        }
    }
}