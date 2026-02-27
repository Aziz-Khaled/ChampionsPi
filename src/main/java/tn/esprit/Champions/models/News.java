package tn.esprit.Champions.models;

public class News {
    private String title;
    private String sentiment; // bullish, bearish ou neutral
    private String url;
    private String publishedAt;

    public News(String title, String sentiment, String url, String publishedAt) {
        this.title = title;
        this.sentiment = sentiment;
        this.url = url;
        this.publishedAt = publishedAt;
    }

    // Getters
    public String getTitle() { return title; }
    public String getSentiment() { return sentiment; }
    public String getUrl() { return url; }
    public String getPublishedAt() { return publishedAt; }
}