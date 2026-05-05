package tn.esprit.Champions.services;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.WebSocket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;
import org.json.JSONArray;
import org.json.JSONObject;

public class MarketApiService {
    private static final String REST_URL = "https://api.binance.com/api/v3/";
    private static final String WS_URL = "wss://stream.binance.com:9443/ws/";

    /**
     * Récupère le prix ponctuel via l'API REST (Utile pour l'initialisation)
     */
    public double fetchPrice(String symbol) {
        try {
            String sym = formatSymbolForRest(symbol);
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(REST_URL + "ticker/price?symbol=" + sym))
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            JSONObject json = new JSONObject(response.body());
            return json.getDouble("price");
        } catch (Exception e) {
            System.err.println("Erreur fetchPrice : " + e.getMessage());
            return 0.0;
        }
    }

    /**
     * Ouvre une connexion WebSocket corrigée pour le flux en temps réel
     */
    public void startPriceStream(String symbol, Consumer<Double> onPriceReceived) {
        // Correction : Binance WebSocket exige des minuscules sans slash (ex: btcusdt)
        String cleanSymbol = symbol.replace("/", "").toLowerCase();
        if (!cleanSymbol.endsWith("usdt")) {
            cleanSymbol += "usdt";
        }

        String streamUrl = WS_URL + cleanSymbol + "@ticker";
        System.out.println("Tentative de connexion WebSocket : " + streamUrl);

        HttpClient.newHttpClient().newWebSocketBuilder()
                .buildAsync(URI.create(streamUrl), new WebSocket.Listener() {

                    @Override
                    public void onOpen(WebSocket webSocket) {
                        System.out.println("WebSocket ouvert avec succès pour " + symbol);
                        // IMPORTANT : Demander le premier message
                        webSocket.request(1);
                    }

                    @Override
                    public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
                        try {
                            JSONObject json = new JSONObject(data.toString());

                            // Correction : Binance envoie les prix en String dans le JSON
                            // Le champ "c" correspond au "Last Price"
                            double price = Double.parseDouble(json.getString("c"));

                            // Envoyer le prix au consommateur (le Dashboard)
                            onPriceReceived.accept(price);

                        } catch (Exception e) {
                            System.err.println("Erreur parsing JSON WebSocket : " + e.getMessage());
                        }

                        // IMPORTANT : Demander le message suivant sinon le flux s'arrête
                        webSocket.request(1);
                        return WebSocket.Listener.super.onText(webSocket, data, last);
                    }

                    @Override
                    public void onError(WebSocket webSocket, Throwable error) {
                        System.err.println("Erreur WebSocket sur " + symbol + " : " + error.getMessage());
                    }

                    @Override
                    public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
                        System.out.println("WebSocket fermé : " + reason);
                        return WebSocket.Listener.super.onClose(webSocket, statusCode, reason);
                    }
                });
    }

    /**
     * Calcule le RSI via l'API REST (Klines/Candlesticks)
     */
    public double calculateRSI(String symbol) {
        try {
            String sym = formatSymbolForRest(symbol);
            // On récupère les 50 dernières bougies d'une heure
            String url = REST_URL + "klines?symbol=" + sym + "&interval=1h&limit=50";

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            JSONArray klines = new JSONArray(response.body());
            List<Double> closes = new ArrayList<>();

            for (int i = 0; i < klines.length(); i++) {
                // L'index 4 est le prix de clôture dans le tableau kline de Binance
                closes.add(klines.getJSONArray(i).getDouble(4));
            }

            return computeRSI(closes, 14);
        } catch (Exception e) {
            System.err.println("Erreur calcul RSI : " + e.getMessage());
            return 50.0;
        }
    }

    private double computeRSI(List<Double> prices, int period) {
        if (prices.size() < period + 1) return 50.0;
        double up = 0, down = 0;
        for (int i = prices.size() - period; i < prices.size(); i++) {
            double diff = prices.get(i) - prices.get(i - 1);
            if (diff > 0) up += diff; else down -= diff;
        }
        if (down == 0) return 100.0;
        double rs = (up / period) / (down / period);
        return 100 - (100 / (1 + rs));
    }

    private String formatSymbolForRest(String symbol) {
        String s = symbol.replace("/", "").toUpperCase();
        return s.endsWith("USDT") ? s : s + "USDT";
    }
}