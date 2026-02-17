package tn.esprit.Champions.services;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class ImageAiService {

    /**
     * Génère une URL d'image professionnelle basée sur le titre et la description.
     */
    public static String generateProjectImageUrl(String title, String description) {
        try {
            // 1. On combine titre et description
            String fullText = (title + " " + description).toLowerCase();

            // 2. Nettoyage : On ne garde que les lettres et on enlève les mots trop courts
            String cleanQuery = fullText.replaceAll("[^a-z ]", " ").trim();

            // 3. Extraction des mots-clés (on prend les 3 premiers mots significatifs)
            String[] words = cleanQuery.split("\\s+");
            StringBuilder keywords = new StringBuilder();
            int count = 0;
            for (String word : words) {
                if (word.length() > 3 && count < 3) { // On ignore "le", "la", "un"...
                    keywords.append(word).append(",");
                    count++;
                }
            }

            // 4. Encodage pour l'URL
            String query = keywords.length() > 0 ? keywords.toString() : "business,startup";
            String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);

            // --- NOUVEAU MOTEUR (Stable en 2026) ---
            // LoremFlickr est parfait pour remplacer Unsplash Source sans clé API
            return "https://loremflickr.com/800/600/" + encodedQuery + "/all";

        } catch (Exception e) {
            // URL de secours en cas d'erreur
            return "https://loremflickr.com/800/600/business";
        }
    }
}