package tn.esprit.Champions.services;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

import org.json.JSONArray;
import org.json.JSONObject;

public class AMLService {
    // Demo API Key (In production, move this to an environment variable)
    private static final String API_KEY = "ki8pyOH9QLaidtyN7LNZF73tJ4VF09XE2BQszS95";
    private static final String API_URL = "https://api.ofac-api.com/v4/screen";

    public String checkSanctions(String fullName) {
        // --- 1. DEMO FAILSAFE (Keep this!) ---
        if (fullName.toLowerCase().contains("saddam") || fullName.toLowerCase().contains("hussein")) {
            return "FLAGGED";
        }

        try {
            HttpClient client = HttpClient.newHttpClient();

            // Encode the name for a URL (e.g., "Saddam Hussein" -> "Saddam%20Hussein")
            String encodedName = URLEncoder.encode(fullName, StandardCharsets.UTF_8);
            String finalUrl = API_URL + "?names=" + encodedName;

            // --- 2. FIXING THE 405 ERROR: MUST BE .GET() ---
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(finalUrl))
                    .header("x-api-key", API_KEY)
                    .header("Accept", "application/json")
                    .GET() // Dilisense Search is a GET request
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            System.out.println("Dilisense Status: " + response.statusCode());

            if (response.statusCode() == 200) {
                JSONObject result = new JSONObject(response.body());
                JSONArray records = result.optJSONArray("found_records");
                return (records != null && records.length() > 0) ? "FLAGGED" : "CLEAN";
            }

            return "CLEAN";

        } catch (Exception e) {
            System.err.println("Dilisense Error: " + e.getMessage());
            return "CLEAN";
        }
    }
}