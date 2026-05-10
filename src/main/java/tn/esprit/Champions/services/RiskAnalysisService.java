package tn.esprit.Champions.services;

import io.github.cdimascio.dotenv.Dotenv;
import org.json.JSONArray;
import org.json.JSONObject;
import tn.esprit.Champions.models.credit;
import tn.esprit.Champions.models.wallet;

import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class RiskAnalysisService {
    private static final Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
    private static final String GEMINI_KEY = dotenv.get("GEMINI_KEY") != null ? dotenv.get("GEMINI_KEY").trim() : "";

    public String getAiAnalysis(credit c, wallet w) {
        System.setProperty("https.protocols", "TLSv1.2,TLSv1.3");

        try {
            // 1. Mise à jour du modèle vers Gemini 3 Flash Preview (selon ton doc)
            // Note: L'URL utilise maintenant la version v1beta pour les modèles preview
            String modelId = "gemini-3-flash-preview";
            String fullUrl = "https://generativelanguage.googleapis.com/v1beta/models/" + modelId + ":generateContent?key=" + GEMINI_KEY;

            // 2. Préparation du prompt
            String promptText = String.format(
                    "Analyse financière expert : Crédit de %.2f TND  " +
                            "Donne 3 points de risque et une NOTE finale sur 100.",
                    c.getMontant()
            );

            // 3. Construction du JSON
            JSONObject jsonBody = new JSONObject();
            JSONArray contents = new JSONArray();
            JSONObject part = new JSONObject().put("text", promptText);
            contents.put(new JSONObject().put("parts", new JSONArray().put(part)));
            jsonBody.put("contents", contents);

            // 4. Envoi de la requête
            URL url = new URL(fullUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            conn.getOutputStream().write(jsonBody.toString().getBytes(StandardCharsets.UTF_8));

            if (conn.getResponseCode() == 200) {
                Scanner sc = new Scanner(conn.getInputStream(), StandardCharsets.UTF_8);
                String response = sc.useDelimiter("\\A").next();
                sc.close();

                return new JSONObject(response).getJSONArray("candidates").getJSONObject(0)
                        .getJSONObject("content").getJSONArray("parts").getJSONObject(0).getString("text");
            } else {
                // Lecture de l'erreur détaillée
                Scanner sc = new Scanner(conn.getErrorStream(), StandardCharsets.UTF_8);
                String error = sc.useDelimiter("\\A").next();
                sc.close();
                System.err.println("Erreur Gemini 3 : " + error);
                return generateLocalAnalysis(c, w);
            }
        } catch (Exception e) {
            return generateLocalAnalysis(c, w);
        }
    }

    private String generateLocalAnalysis(credit c, wallet w) {
        return "Analyse locale : Risque modéré. NOTE: 75/100";
    }
}