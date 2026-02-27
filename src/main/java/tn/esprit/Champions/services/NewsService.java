package tn.esprit.Champions.services;

import org.json.JSONArray;
import org.json.JSONObject;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import tn.esprit.Champions.models.News;

public class NewsService {
    // Inscris-toi sur newsapi.org pour avoir ta propre clé gratuite
    private static final String API_KEY = "e0b09ace06f0446aa8471262382a9bd8";

    public List<News> getLatestNews(String symbol) {
        List<News> newsList = new ArrayList<>();
        try {
            String cleanSymbol = symbol.toUpperCase().replace("USDT", "");

            // On cherche les news liées à la crypto (ex: Bitcoin)
            String url = "https://newsapi.org/v2/everything?q=" + cleanSymbol +
                    "&apiKey=" + API_KEY + "&pageSize=10&language=en";

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("User-Agent", "Java-Crypto-App")
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JSONObject json = new JSONObject(response.body());
                JSONArray articles = json.getJSONArray("articles");

                for (int i = 0; i < articles.length(); i++) {
                    JSONObject art = articles.getJSONObject(i);
                    newsList.add(new News(
                            art.getString("title"),
                            "neutral",
                            art.getString("url"),
                            art.getString("publishedAt")
                    ));
                }
            } else {
                System.err.println("Erreur NewsAPI : Code " + response.statusCode());
            }
        } catch (Exception e) {
            System.err.println("Exception : " + e.getMessage());
        }
        return newsList;
    }
}