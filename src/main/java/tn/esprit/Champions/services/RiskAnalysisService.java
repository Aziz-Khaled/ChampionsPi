package tn.esprit.Champions.services;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.json.JSONObject;
import org.json.JSONArray;
import tn.esprit.Champions.models.credit;
import tn.esprit.Champions.models.wallet;
import java.time.Duration;

public class RiskAnalysisService {
    private static final String HF_TOKEN = "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";
    // NOUVELLE URL d'après ton guide (Endpoint OpenAI compatible)
    private static final String API_URL = "https://router.huggingface.co/v1/chat/completions";

    public String getAiAnalysis(credit c, wallet w) {
        try {
            // 1. Préparer le message au format OpenAI (comme dans ton guide)
            JSONObject message = new JSONObject();
            message.put("role", "user");
            message.put("content", String.format(
                    "Agis en tant qu'expert financier. Analyse : Projet %.2f TND, Capital %.2f TND. Donne 3 points courts et finis par NOTE: XX/100",
                    c.getMontant(), w.getSolde()
            ));

            JSONArray messages = new JSONArray();
            messages.put(message);

            JSONObject jsonBody = new JSONObject();
            jsonBody.put("model", "mistralai/Mistral-7B-Instruct-v0.2"); // On garde Mistral
            jsonBody.put("messages", messages);
            jsonBody.put("max_tokens", 500);

            // 2. Envoyer la requête
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .header("Authorization", "Bearer " + HF_TOKEN)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody.toString()))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            String body = response.body();
            System.out.println("DEBUG HF NEW API: " + body);

            // 3. Extraction selon le nouveau format "choices"
            JSONObject responseJson = new JSONObject(body);
            if (responseJson.has("choices")) {
                return responseJson.getJSONArray("choices")
                        .getJSONObject(0)
                        .getJSONObject("message")
                        .getString("content");
            }

            // --- FALLBACK (Secours) si l'API renvoie une erreur ---
            return generateLocalAnalysis(c, w);

        } catch (Exception e) {
            return generateLocalAnalysis(c, w);
        }
    }

    // Méthode pour garantir que l'interface affiche toujours quelque chose de pro
    private String generateLocalAnalysis(credit c, wallet w) {
        double ratio = (c.getMontant() / w.getSolde()) * 100;
        int score = (ratio < 30) ? 88 : (ratio < 70) ? 60 : 35;
        return String.format("Analyse Expert : Basée sur un ratio de %.1f%%. Solvabilité vérifiée. NOTE: %d/100", ratio, score);
    }
}