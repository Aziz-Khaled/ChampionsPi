package tn.esprit.Champions.services;

import tn.esprit.Champions.models.Conversion;

import java.sql.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;
import org.json.JSONObject;

public class ConversionService {

    private Connection cnx;

    public ConversionService() {
        // connexion DB ici
    }


    public double getExchangeRate(String from, String to) throws Exception {

        String apiUrl = "https://api.exchangerate-api.com/v4/latest/" + from;

        URL url = new URL(apiUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");

        Scanner sc = new Scanner(url.openStream());
        StringBuilder response = new StringBuilder();

        while (sc.hasNext()) {
            response.append(sc.nextLine());
        }
        sc.close();

        JSONObject json = new JSONObject(response.toString());

        return json.getJSONObject("rates").getDouble(to);
    }
    public void insertConversion(Conversion c) throws SQLException {

        String sql = "INSERT INTO conversion "
                + "(amount_from, currency_from, amount_to, currency_to, exchange_rate) "
                + "VALUES (?, ?, ?, ?, ?)";

        PreparedStatement ps = cnx.prepareStatement(sql);

        ps.setDouble(1, c.getAmountFrom());
        ps.setInt(2, c.getCurrencyFrom());
        ps.setDouble(3, c.getAmountTo());
        ps.setInt(4, c.getCurrencyTo());
        ps.setDouble(5, c.getExchangeRate());

        ps.executeUpdate();
    }
}