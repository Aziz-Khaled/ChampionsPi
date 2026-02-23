package tn.esprit.Champions.services;

import org.json.JSONArray;
import org.json.JSONObject;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class ImageAiService {

    private static final String GEMINI_KEY = "AIzaSyAN9AvnxAemMENR0gr14tboXjkHDB1NZpI";
    private static final String HF_TOKEN = "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";

    private static final String GEMINI_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=" + GEMINI_KEY;

    // Utilisation du point de terminaison OpenAI-compatible via Together AI
    private static final String HF_ROUTER_URL = "https://router.huggingface.co/together/v1/images/generations";

    public static String generateAndSaveAiImage(String title, String description) {
        try {
            // 1. Obtention du prompt visuel via Gemini
            String smartPrompt = getPromptFromGemini(title, description);
            System.out.println("Prompt IA généré : " + smartPrompt);

            // 2. Requête vers le Router Hugging Face
            URL url = new URL(HF_ROUTER_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Authorization", "Bearer " + HF_TOKEN);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            JSONObject jsonBody = new JSONObject();
            jsonBody.put("model", "black-forest-labs/FLUX.1-dev");
            jsonBody.put("prompt", smartPrompt);
            jsonBody.put("n", 1);
            jsonBody.put("size", "1024x1024");

            conn.getOutputStream().write(jsonBody.toString().getBytes(StandardCharsets.UTF_8));

            int responseCode = conn.getResponseCode();

            if (responseCode == 200) {
                // --- ÉTAPE CORRIGÉE : LECTURE DU JSON ---
                Scanner s = new Scanner(conn.getInputStream()).useDelimiter("\\A");
                String responseBody = s.hasNext() ? s.next() : "";

                JSONObject jsonResponse = new JSONObject(responseBody);

                // Extraction de l'URL de l'image stockée dans l'objet "data" du JSON
                String distantImageUrl = jsonResponse.getJSONArray("data")
                        .getJSONObject(0)
                        .getString("url");

                System.out.println("URL de l'image reçue : " + distantImageUrl);

                // 3. Téléchargement et Standardisation de l'image
                return downloadAndSaveFinalImage(distantImageUrl);

            } else {
                Scanner s = new Scanner(conn.getErrorStream()).useDelimiter("\\A");
                System.err.println("Détail erreur HF (" + responseCode + "): " + (s.hasNext() ? s.next() : ""));
            }
        } catch (Exception e) {
            System.err.println("Exception lors du processus IA : " + e.getMessage());
            e.printStackTrace();
        }
        return "/images/default_project.png";
    }

    /**
     * Télécharge l'image depuis l'URL temporaire fournie par l'IA et la sauve en PNG localement.
     */
    private static String downloadAndSaveFinalImage(String distantUrl) {
        try {
            URL url = new URL(distantUrl);
            BufferedImage bufferedImage = ImageIO.read(url);

            if (bufferedImage != null) {
                String fileName = "ai_flux_" + System.currentTimeMillis() + ".png";

                // Chemin vers ton dossier de ressources
                File dir = new File("src/main/resources/uploads");
                if (!dir.exists()) dir.mkdirs();

                File outputFile = new File(dir, fileName);

                // Réencodage en PNG standard pour éviter les formats corrompus
                boolean success = ImageIO.write(bufferedImage, "png", outputFile);

                if (success) {
                    System.out.println("Succès ! Image sauvegardée : " + fileName);
                    return "/uploads/" + fileName;
                }
            }
        } catch (Exception e) {
            System.err.println("Erreur au téléchargement final : " + e.getMessage());
        }
        return "/images/default_project.png";
    }

    private static String getPromptFromGemini(String title, String description) {
        try {
            URL url = new URL(GEMINI_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            String promptInput = "Write a one-sentence visual description for an image generator about: " + title + ". Professional 3D style.";

            JSONObject content = new JSONObject().put("contents", new JSONArray().put(
                    new JSONObject().put("parts", new JSONArray().put(new JSONObject().put("text", promptInput)))
            ));

            conn.getOutputStream().write(content.toString().getBytes(StandardCharsets.UTF_8));
            Scanner sc = new Scanner(conn.getInputStream());
            String response = sc.useDelimiter("\\A").next();
            sc.close();

            return new JSONObject(response).getJSONArray("candidates").getJSONObject(0)
                    .getJSONObject("content").getJSONArray("parts").getJSONObject(0).getString("text");
        } catch (Exception e) {
            return "A professional business illustration for " + title;
        }
    }
}