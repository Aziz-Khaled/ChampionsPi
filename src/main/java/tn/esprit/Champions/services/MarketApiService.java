package tn.esprit.Champions.services;

import java.net.URI;
import java.net.http.*;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public class MarketApiService {
    private static final String API_URL = "https://api.binance.com/api/v3/";

    public double fetchPrice(String symbol) {
        try {
            String sym = symbol.toUpperCase().endsWith("USDT") ? symbol : symbol + "USDT";
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(API_URL + "ticker/price?symbol=" + sym)).build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            return new JSONObject(response.body()).getDouble("price");
        } catch (Exception e) { return 0.0; }
    }

    public double calculateRSI(String symbol) {
        try {
            String sym = symbol.toUpperCase().endsWith("USDT") ? symbol : symbol + "USDT";
            String url = API_URL + "klines?symbol=" + sym + "&interval=1h&limit=50";
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            JSONArray klines = new JSONArray(response.body());

            List<Double> closes = new ArrayList<>();
            for (int i = 0; i < klines.length(); i++) {
                closes.add(klines.getJSONArray(i).getDouble(4));
            }
            return computeRSI(closes, 14);
        } catch (Exception e) { return 50.0; }
    }

    private double computeRSI(List<Double> prices, int period) {
        double up = 0, down = 0;
        for (int i = prices.size() - period; i < prices.size(); i++) {
            double diff = prices.get(i) - prices.get(i-1);
            if (diff > 0) up += diff; else down -= diff;
        }
        double rs = (up / period) / (down / period);
        return 100 - (100 / (1 + rs));
    }
}