package tn.esprit.Champions.services;

import tn.esprit.Champions.models.Conversion;
import tn.esprit.Champions.utils.DbConnection;
import java.sql.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;
import org.json.JSONObject;

public class ConversionService {

    private Connection cnx;

    public ConversionService() {
        this.cnx = DbConnection.getInstance().getCnx();
    }


    public double getExchangeRate(String fromCurrencyCode, String toCurrencyCode) throws Exception {

        String from = fromCurrencyCode.trim().toUpperCase();
        String to = toCurrencyCode.trim().toUpperCase();


        if (from.equals(to)) return 1.0;


        String apiUrl = "https://min-api.cryptocompare.com/data/price?fsym=" + from + "&tsyms=" + to;

        URL url = new URL(apiUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(5000); // 5 secondes timeout

        int responseCode = conn.getResponseCode();
        if (responseCode != 200) {
            throw new Exception("Erreur API (Code: " + responseCode + "). Vérifiez votre connexion internet.");
        }

        // Lecture du flux JSON
        StringBuilder response = new StringBuilder();
        try (Scanner sc = new Scanner(conn.getInputStream())) {
            while (sc.hasNext()) {
                response.append(sc.nextLine());
            }
        }

        JSONObject json = new JSONObject(response.toString());


        if (json.has(to)) {
            return json.getDouble(to);
        } else if (json.has("Response") && json.getString("Response").equals("Error")) {
            throw new Exception("API Error: " + json.getString("Message"));
        } else {
            throw new Exception("Impossible de trouver le taux pour " + to);
        }
    }

    /**
     * Insère une nouvelle conversion et retourne l'ID auto-généré
     */
    public int insertConversion(Conversion c) throws SQLException {
        String sql = "INSERT INTO conversion (amount_from, currency_from, amount_to, currency_to, exchange_rate) VALUES (?, ?, ?, ?, ?)";

        try (PreparedStatement ps = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setDouble(1, c.getAmountFrom());
            ps.setInt(2, c.getCurrencyFrom());
            ps.setDouble(3, c.getAmountTo());
            ps.setInt(4, c.getCurrencyTo());
            ps.setDouble(5, c.getExchangeRate());

            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1); // Retourne l'ID inséré
                }
            }
        }
        return -1;
    }


    public Conversion getById(int id) throws SQLException {
        String sql = "SELECT * FROM conversion WHERE id_conversion = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Conversion c = new Conversion();
                    c.setIdConversion(rs.getInt("id_conversion"));
                    c.setAmountFrom(rs.getDouble("amount_from"));
                    c.setCurrencyFrom(rs.getInt("currency_from"));
                    c.setAmountTo(rs.getDouble("amount_to"));
                    c.setCurrencyTo(rs.getInt("currency_to"));
                    c.setExchangeRate(rs.getDouble("exchange_rate"));
                    return c;
                }
            }
        }
        return null;
    }
}