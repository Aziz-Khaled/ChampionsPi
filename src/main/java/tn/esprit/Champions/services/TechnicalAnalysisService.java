package tn.esprit.Champions.services;

import java.net.URI;
import java.net.http.*;
import org.json.JSONObject;
import java.util.Iterator;

public class TechnicalAnalysisService {

    // URL réelle (Alpha Vantage). Note: 'demo' fonctionne pour IBM, sinon utilisez votre clé gratuite.
    private static final String API_KEY = "demo";
    private static final String BASE_URL = "https://www.alphavantage.co/query?function=RSI&symbol=%s&interval=daily&time_period=14&series_type=close&apikey=" + API_KEY;

    public double fetchRSI(String symbol) {
        try {
            // Adaptation du symbole (ex: BTC -> BTCUSDT pour Alpha Vantage)
            String querySymbol = symbol.equalsIgnoreCase("bitcoin") ? "BTCUSD" : symbol.toUpperCase();
            String finalUrl = String.format(BASE_URL, querySymbol);

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(finalUrl)).build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            JSONObject json = new JSONObject(response.body());

            if (json.has("Technical Analysis: RSI")) {
                JSONObject technicalData = json.getJSONObject("Technical Analysis: RSI");
                // On prend la date la plus récente (le premier élément)
                Iterator<String> keys = technicalData.keys();
                if (keys.hasNext()) {
                    String lastDate = keys.next();
                    return technicalData.getJSONObject(lastDate).getDouble("RSI");
                }
            }
            return 50.0; // Neutre si l'API ne répond pas
        } catch (Exception e) {
            return 50.0;
        }
    }

    public String getAdvice(double rsi) {
        if (rsi >= 70) return "🔥 SURACHETÉ (Vendre)";
        if (rsi <= 30) return "❄️ SURVENDU (Acheter)";
        return "⚖️ NEUTRE";
    }
}