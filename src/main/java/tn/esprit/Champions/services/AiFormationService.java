package tn.esprit.Champions.services;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.json.JSONObject;
import org.json.JSONArray;
public class AiFormationService {
    // TA CLÉ (Vérifie bien qu'il n'y a pas d'espace à la fin)
    private static final String API_KEY = "AIzaSyBMWnDOZzlklzrfpkyO_F-uMaD3cUkJed8";

    // URL OFFICIELLE VERSION V1
    private static final String API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=" + API_KEY;

    public String askGemini(String prompt) {
        try {
            // Nettoyage rapide du prompt pour le JSON
            String escapedPrompt = prompt.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r");

            // Construction du corps JSON manuellement pour être sûr du format
            String jsonBody = "{"
                    + "\"contents\": [{"
                    + "  \"parts\": [{\"text\": \"" + escapedPrompt + "\"}]"
                    + "}]"
                    + "}";

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            // Affiche ceci dans ta console pour le diagnostic final
            System.out.println("--- DEBUG GEMINI ---");
            System.out.println("URL utilisée: " + API_URL);
            System.out.println("Code de réponse: " + response.statusCode());
            System.out.println("Corps de réponse: " + response.body());

            JSONObject responseJson = new JSONObject(response.body());

            if (response.statusCode() == 200) {
                return responseJson.getJSONArray("candidates")
                        .getJSONObject(0)
                        .getJSONObject("content")
                        .getJSONArray("parts")
                        .getJSONObject(0)
                        .getString("text");
            } else {
                return "Erreur " + response.statusCode() + " : " + response.body();
            }

        } catch (Exception e) {
            return "Erreur technique : " + e.getMessage();
        }
    }
}
