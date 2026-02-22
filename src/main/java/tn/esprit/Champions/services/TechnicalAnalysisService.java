package tn.esprit.Champions.services;

import java.net.URI;
import java.net.http.*;
import org.json.JSONObject;

public class TechnicalAnalysisService {
    private static final String API_KEY = "demo";
    private static final String BASE_URL = "https://www.alphavantage.co/query?function=RSI&symbol=%s&interval=daily&time_period=14&series_type=close&apikey=" + API_KEY;

    public double fetchRSI(String symbol) {
        try {
            String querySymbol = symbol.equalsIgnoreCase("bitcoin") ? "BTCUSD" : symbol.toUpperCase();
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(String.format(BASE_URL, querySymbol))).build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            JSONObject json = new JSONObject(response.body());
            JSONObject technicalData = json.getJSONObject("Technical Analysis: RSI");
            String lastDate = technicalData.keys().next();
            return technicalData.getJSONObject(lastDate).getDouble("RSI");
        } catch (Exception e) {
            return 50.0; // Neutre si erreur
        }
    }

    public String getAdvice(double rsi) {
        if (rsi >= 70) return "SELL (Overbought)";
        if (rsi <= 30) return "BUY (Oversold)";
        return "HOLD (Neutral)";
    }
}