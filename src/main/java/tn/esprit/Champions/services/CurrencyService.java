package tn.esprit.Champions.services;

import tn.esprit.Champions.models.currency;
import tn.esprit.Champions.models.typeCurrency;
import tn.esprit.Champions.models.typeWallet;
import tn.esprit.Champions.utils.DbConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;

public class CurrencyService implements CRUD<currency>
{
    private Connection cnx;
    public CurrencyService()
    {
        cnx = DbConnection.getInstance().getCnx();
    }


//currency unique
    @Override
    public void insertOne(currency currency) throws SQLException {
        // Vérifier si la currency existe déjà par code ou nom
        String checkQuery = "SELECT id_currency FROM currency WHERE code = ? OR nom = ?";
        PreparedStatement checkStmt = cnx.prepareStatement(checkQuery);
        checkStmt.setString(1, currency.getCode());
        checkStmt.setString(2, currency.getNom());
        ResultSet rs = checkStmt.executeQuery();

        if (rs.next()) {
            throw new SQLException("Cette currency existe déjà : " + currency.getNom() + " ou code : " + currency.getCode());
        }

        // Insertion dans la table
        String query = "INSERT INTO currency(code, nom, type_currency, is_trading) VALUES (?, ?, ?, ?)";
        PreparedStatement pst = cnx.prepareStatement(query);
        pst.setString(1, currency.getCode());
        pst.setString(2, currency.getNom());
        pst.setString(3, currency.getType_currency().name().toLowerCase()); // fiat ou crypto
        pst.setBoolean(4, currency.isIs_trading());

        pst.executeUpdate();

        System.out.println("Currency ajoutée : " + currency.getNom() + " (" + currency.getCode() + ")");
    }


    @Override
    public void updateOne(currency currency) throws SQLException {

        if (currency.getType_currency() == typeCurrency.crypto) {
            String query = "UPDATE `currency` SET is_trading = ? WHERE id_currency = ?";
            PreparedStatement pst = cnx.prepareStatement(query);
            pst.setInt(1, currency.isIs_trading() ? 1 : 0); // 1 si true, 0 si false
            pst.setInt(2, currency.getId_currency());
            pst.executeUpdate();
        }

    }

    @Override
    public void deleteOne(currency currency) throws SQLException {
        // Vérifier si la currency est utilisée dans wallet_currency avec un solde != 0
        String checkQuery = "SELECT COUNT(*) FROM wallet_currency WHERE id_currency = ? AND solde != 0";
        PreparedStatement checkPst = cnx.prepareStatement(checkQuery);
        checkPst.setInt(1, currency.getId_currency());
        ResultSet rs = checkPst.executeQuery();
        if (rs.next() && rs.getInt(1) > 0) {
            throw new SQLException("Impossible de supprimer cette currency : elle est utilisée dans un wallet avec un solde non nul !");
        }

        // Si ok, suppression
        String deleteQuery = "DELETE FROM currency WHERE id_currency = ?";
        PreparedStatement deletePst = cnx.prepareStatement(deleteQuery);
        deletePst.setInt(1, currency.getId_currency());
        deletePst.executeUpdate();
    }

    @Override
    public List<currency> SelectAll() throws SQLException {
        List<currency> list = new ArrayList<>();
        String query = "SELECT * FROM currency";
        Connection cnx = DbConnection.getInstance().getCnx();
        PreparedStatement ps = cnx.prepareStatement(query);
        ResultSet rs = ps.executeQuery();

        while (rs.next()) {
            currency c = new currency();
            c.setId_currency(rs.getInt("id_currency"));
            c.setCode(rs.getString("code"));
            c.setNom(rs.getString("nom"));
            c.setType_currency(typeCurrency.valueOf(rs.getString("type_currency"))); // enum
            c.setIs_trading(rs.getInt("is_trading") == 1); // ✅ conversion 0/1 -> boolean
            list.add(c);
        }
        return list;
    }
    public String getCurrencyNameById(int idCurrency) throws SQLException {
        String sql = "SELECT nom FROM currency WHERE id_currency = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, idCurrency);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("nom");
                }
            }
        }
        return "N/A";
    }
}

