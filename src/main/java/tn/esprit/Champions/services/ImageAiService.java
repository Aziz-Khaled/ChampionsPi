package tn.esprit.Champions.services;

import io.github.cdimascio.dotenv.Dotenv;
import org.json.JSONArray;
import org.json.JSONObject;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class ImageAiService {

    private static final Dotenv dotenv = Dotenv.load();
    private static final String OPENROUTER_KEY = dotenv.get("OPENROUTER_KEY");
    // Utilisation d'un modèle "free" pour garantir l'accès
    private static final String MODEL = "openai/gpt-3.5-turbo";

    private static String queryOpenRouter(String prompt) {
        try {
            // CORRECTION : URL complète indispensable
            URL url = new URL("https://openrouter.ai/api/v1/chat/completions");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Authorization", "Bearer " + OPENROUTER_KEY);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("HTTP-Referer", "http://localhost");
            conn.setDoOutput(true);

            JSONObject jsonBody = new JSONObject();
            jsonBody.put("model", MODEL);
            JSONArray messages = new JSONArray();
            messages.put(new JSONObject().put("role", "user").put("content", prompt));
            jsonBody.put("messages", messages);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(jsonBody.toString().getBytes(StandardCharsets.UTF_8));
            }

            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                Scanner sc = new Scanner(conn.getInputStream(), StandardCharsets.UTF_8);
                String res = sc.useDelimiter("\\A").next();
                sc.close();

                JSONObject responseJson = new JSONObject(res);
                return responseJson.getJSONArray("choices")
                        .getJSONObject(0)
                        .getJSONObject("message")
                        .getString("content").trim();
            } else {
                System.err.println("Erreur API OpenRouter: " + responseCode);
            }
        } catch (Exception e) {
            System.err.println("Erreur Technique: " + e.getMessage());
        }
        return "Technologie";
    }

    public static String detectProjectSector(String titre, String description) {
        System.out.println("IA : Analyse du secteur pour '" + titre + "'...");
        String prompt = "Classe ce projet dans UN SEUL mot : Agriculture, Technologie, Energie, Sante, Immobilier, Education, Artisanat. Projet: " + titre;
        String response = queryOpenRouter(prompt);

        String[] secteurs = {"Agriculture", "Technologie", "Energie", "Sante", "Immobilier", "Education", "Artisanat"};
        for (String s : secteurs) {
            if (response.toLowerCase().contains(s.toLowerCase())) return s;
        }
        return "Technologie";
    }

    public static String generateAndSaveAiImage(String title, String description) {
        System.out.println("Génération du visuel...");
        // Utilisation de Picsum pour éviter l'erreur 530 (Cloudflare)
        // On génère un ID basé sur le titre pour que l'image soit cohérente durant la session
        int imageId = Math.abs(title.hashCode() % 1000);
        String imageUrl = "https://picsum.photos/seed/" + imageId + "/512/512";

        return downloadAndSaveFinalImage(imageUrl);
    }

    private static String downloadAndSaveFinalImage(String distantUrl) {
        try {
            URL url = new URL(distantUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestProperty("User-Agent", "Mozilla/5.0");
            connection.setInstanceFollowRedirects(true);

            BufferedImage image = ImageIO.read(connection.getInputStream());
            if (image != null) {
                String fileName = "ai_proj_" + System.currentTimeMillis() + ".png";
                File dir = new File("src/main/resources/uploads");
                if (!dir.exists()) dir.mkdirs();

                File outputFile = new File(dir, fileName);
                ImageIO.write(image, "png", outputFile);
                System.out.println("✅ Image sauvegardée : " + fileName);
                return "/uploads/" + fileName;
            }
        } catch (Exception e) {
            System.err.println("❌ Erreur téléchargement : " + e.getMessage());
        }
        return "/images/default_project.png";
    }
}