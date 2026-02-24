package tn.esprit.Champions.services;

import java.net.URI;
import java.net.http.*;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public class TechnicalAnalysisService {

    // Utilisation de l'API publique de Binance
    private static final String BINANCE_URL = "https://api.binance.com/api/v3/klines?symbol=%s&interval=1h&limit=100";

    public double fetchRSI(String symbol) {
        try {
            String formattedSymbol = formatSymbol(symbol);

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(String.format(BINANCE_URL, formattedSymbol)))
                    .header("Accept", "application/json")
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            String body = response.body();

            // Vérification si la réponse commence bien par '[' (JSONArray)
            if (!body.trim().startsWith("[")) {
                JSONObject errorObj = new JSONObject(body);
                System.err.println("Binance API Error: " + errorObj.optString("msg", "Unknown error"));
                return 50.0;
            }

            JSONArray klines = new JSONArray(body);
            List<Double> closePrices = new ArrayList<>();
            for (int i = 0; i < klines.length(); i++) {
                closePrices.add(klines.getJSONArray(i).getDouble(4)); // Index 4 = Close Price
            }

            return calculateRSI(closePrices, 14);
        } catch (Exception e) {
            System.err.println("Erreur Analyse: " + e.getMessage());
            return 50.0;
        }
    }

    private String formatSymbol(String symbol) {
        if (symbol == null || symbol.isEmpty()) return "BTCUSDT";

        String s = symbol.toUpperCase().trim();
        // Nettoyage des noms longs vers symboles courts
        if (s.contains("BITCOIN")) s = "BTC";
        if (s.contains("ETHEREUM")) s = "ETH";

        // Binance nécessite le format SYMBOL + PAIR (ex: BTCUSDT)
        if (!s.endsWith("USDT")) {
            s = s + "USDT";
        }
        return s;
    }

    private double calculateRSI(List<Double> prices, int period) {
        if (prices.size() <= period) return 50.0;

        double gains = 0, losses = 0;
        for (int i = 1; i <= period; i++) {
            double diff = prices.get(i) - prices.get(i - 1);
            if (diff >= 0) gains += diff; else losses -= diff;
        }

        double avgGain = gains / period;
        double avgLoss = losses / period;

        for (int i = period + 1; i < prices.size(); i++) {
            double diff = prices.get(i) - prices.get(i - 1);
            double currentGain = diff >= 0 ? diff : 0;
            double currentLoss = diff < 0 ? -diff : 0;

            avgGain = (avgGain * (period - 1) + currentGain) / period;
            avgLoss = (avgLoss * (period - 1) + currentLoss) / period;
        }

        if (avgLoss == 0) return 100.0;
        return 100.0 - (100.0 / (1.0 + (avgGain / avgLoss)));
    }

    public String getAdvice(double rsi) {
        if (rsi >= 70) return "🔥 SELL (Overbought)";
        if (rsi <= 30) return "🚀 BUY (Oversold)";
        if (rsi > 55) return "📈 BULLISH";
        if (rsi < 45) return "📉 BEARISH";
        return "⚖️ NEUTRAL";
    }
}