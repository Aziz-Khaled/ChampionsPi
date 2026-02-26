package tn.esprit.Champions.services;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import tn.esprit.Champions.models.OrderItem;
import tn.esprit.Champions.models.Product;

import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class GeminiService {

    private static final String API_KEY = "xxxxxxxxxxxxxx";

    
    private static final String API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key="
            + API_KEY;
    private final Gson gson = new Gson();

    public String correctDescription(String description) {
        try {
            String prompt = "You are a professional copywriter. Correct the spelling, grammar, and flow of the following product description. "
                    +
                    "Keep the same tone and meaning. Return ONLY the corrected text without any introductory or concluding remarks.\n\n"
                    +
                    "Description to correct:\n" + description;

            JsonObject requestBody = new JsonObject();
            JsonArray contents = new JsonArray();
            JsonObject content = new JsonObject();
            JsonArray parts = new JsonArray();
            JsonObject part = new JsonObject();
            part.addProperty("text", prompt);
            parts.add(part);
            content.add("parts", parts);
            contents.add(content);
            requestBody.add("contents", contents);

            URL url = new URL(API_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = requestBody.toString().getBytes("utf-8");
                os.write(input, 0, input.length);
            }

            if (conn.getResponseCode() == 200) {
                JsonObject response = gson.fromJson(new InputStreamReader(conn.getInputStream(), "utf-8"),
                        JsonObject.class);
                String resultText = response.getAsJsonArray("candidates")
                        .get(0).getAsJsonObject()
                        .getAsJsonObject("content")
                        .getAsJsonArray("parts")
                        .get(0).getAsJsonObject()
                        .get("text").getAsString().trim();

                return resultText;
            } else {
                System.err.println("Gemini API Error: " + conn.getResponseCode());
            }

        } catch (Exception e) {
            System.err.println("Error calling Gemini API for correction: " + e.getMessage());
            e.printStackTrace();
        }
        return description; // Return original if error
    }

    public List<Integer> getRecommendedProductIds(List<OrderItem> currentCart, List<Product> allProducts) {
        try {
            String cartContext = currentCart.stream()
                    .map(item -> item.getProduct().getName() + " (Cat: " + item.getProduct().getCategory() + ")")
                    .collect(Collectors.joining(", "));

            String availableProducts = allProducts.stream()
                    .map(p -> "ID: " + p.getId() + " - Name: " + p.getName() + " (Cat: " + p.getCategory() + ")")
                    .collect(Collectors.joining("\n"));

            String prompt = "You are a specialized BTC Fintech Marketplace assistant. Given the user's current cart: ["
                    + cartContext + "], " +
                    "and the following available products:\n" + availableProducts + "\n\n" +
                    "Recommend exactly 3 product IDs that would be most useful or relevant to this user. " +
                    "Return ONLY a JSON array of integers, for example: [1, 5, 8]. No text explanation.";

            JsonObject requestBody = new JsonObject();
            JsonArray contents = new JsonArray();
            JsonObject content = new JsonObject();
            JsonArray parts = new JsonArray();
            JsonObject part = new JsonObject();
            part.addProperty("text", prompt);
            parts.add(part);
            content.add("parts", parts);
            contents.add(content);
            requestBody.add("contents", contents);

            URL url = new URL(API_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = requestBody.toString().getBytes("utf-8");
                os.write(input, 0, input.length);
            }

            if (conn.getResponseCode() == 200) {
                JsonObject response = gson.fromJson(new InputStreamReader(conn.getInputStream(), "utf-8"),
                        JsonObject.class);
                String resultText = response.getAsJsonArray("candidates")
                        .get(0).getAsJsonObject()
                        .getAsJsonObject("content")
                        .getAsJsonArray("parts")
                        .get(0).getAsJsonObject()
                        .get("text").getAsString().trim();

                // Clean potential markdown backticks from the response
                if (resultText.startsWith("```json")) {
                    resultText = resultText.substring(7);
                }
                if (resultText.startsWith("```")) {
                    resultText = resultText.substring(3);
                }
                if (resultText.endsWith("```")) {
                    resultText = resultText.substring(0, resultText.length() - 3);
                }
                resultText = resultText.trim();

                return gson.fromJson(resultText, ArrayList.class);
            } else {
                System.err.println("Gemini API Error: " + conn.getResponseCode());
            }

        } catch (Exception e) {
            System.err.println("Error calling Gemini API: " + e.getMessage());
            e.printStackTrace();
        }
        return new ArrayList<>();
    }
}
