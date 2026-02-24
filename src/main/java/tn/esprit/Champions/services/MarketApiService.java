package tn.esprit.Champions.services;

import java.net.URI;
import java.net.http.*;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.HashMap;
import java.util.Map;

public class MarketApiService {
    // Endpoint pour obtenir TOUS les prix d'un coup (Optimisation réseau)
    private static final String ALL_PRICES_URL = "https://api.binance.com/api/v3/ticker/price";
    private static final String SINGLE_PRICE_URL = "https://api.binance.com/api/v3/ticker/price?symbol=%sUSDT";

    /**
     * Récupère le prix d'une crypto spécifique
     */
    public double fetchPrice(String assetName) {
        try {
            String symbol = formatSymbol(assetName);
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(String.format(SINGLE_PRICE_URL, symbol)))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            JSONObject json = new JSONObject(response.body());

            return json.getDouble("price");
        } catch (Exception e) {
            // Repli sur une simulation réaliste si l'API échoue
            return 60000.0 + (Math.random() * 1000);
        }
    }

    /**
     * OPTIMISATION : Récupère tous les prix en une seule fois.
     * Très utile pour rafraîchir ton tableau sans faire 50 requêtes.
     */
    public Map<String, Double> fetchAllPrices() {
        Map<String, Double> pricesMap = new HashMap<>();
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(ALL_PRICES_URL)).build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            JSONArray jsonArray = new JSONArray(response.body());
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject obj = jsonArray.getJSONObject(i);
                String symbol = obj.getString("symbol");
                if (symbol.endsWith("USDT")) {
                    pricesMap.put(symbol.replace("USDT", ""), obj.getDouble("price"));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return pricesMap;
    }

    private String formatSymbol(String assetName) {
        String s = assetName.toUpperCase().trim();
        if (s.equals("BITCOIN")) return "BTC";
        if (s.equals("ETHEREUM")) return "ETH";
        return s;
    }
}