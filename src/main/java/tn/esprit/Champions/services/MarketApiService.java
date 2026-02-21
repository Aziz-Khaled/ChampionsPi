package tn.esprit.Champions.services;

import java.net.URI;
import java.net.http.*;
import org.json.JSONObject;

public class MarketApiService {
    private static final String API_URL = "https://api.coingecko.com/api/v3/simple/price?ids=bitcoin,ethereum,solana&vs_currencies=usd";

    public double fetchPrice(String assetName) {
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(API_URL)).build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            JSONObject json = new JSONObject(response.body());
            return json.getJSONObject(assetName.toLowerCase()).getDouble("usd");
        } catch (Exception e) {
            // Simulation si l'API est indisponible
            return 50000.0 + (Math.random() * 500);
        }
    }
}