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
        String checkQuery = "SELECT id_currency FROM currency WHERE nom = ?";
        PreparedStatement checkStmt = cnx.prepareStatement(checkQuery);
        checkStmt.setString(1, currency.getNom());
        ResultSet rs = checkStmt.executeQuery();

        if (rs.next()) {
            throw new SQLException("Cette currency existe déjà : " + currency.getNom());
        }

        String query = "INSERT INTO `currency`(`nom`, `type_currency`) VALUES (?,?)";
        PreparedStatement pst = cnx.prepareStatement(query);
        pst.setString(1, currency.getNom());
        pst.setString(2, currency.getType_currency().name());
        pst.executeUpdate();

        System.out.println("Currency ajoutée : " + currency.getNom());
    }



    @Override
    public void updateOne(currency currency) throws SQLException {
        // Vérification doublon
        String checkQuery = "SELECT id_currency FROM currency WHERE nom = ? AND id_currency != ?";
        PreparedStatement checkStmt = cnx.prepareStatement(checkQuery);
        checkStmt.setString(1, currency.getNom());
        checkStmt.setInt(2, currency.getId_currency());
        ResultSet rs = checkStmt.executeQuery();
        if (rs.next()) {
            throw new SQLException("Une autre currency existe déjà avec ce nom : " + currency.getNom());
        }

        // Update currency
        String query = "UPDATE currency SET nom = ?, type_currency = ? WHERE id_currency = ?";
        PreparedStatement pst = cnx.prepareStatement(query);
        pst.setString(1, currency.getNom());
        pst.setString(2, currency.getType_currency().name());
        pst.setInt(3, currency.getId_currency());
        pst.executeUpdate();

        // Update wallet_currency pour correspondre au nouveau nom
        String updateWallet = "UPDATE wallet_currency SET nom_currency = ? WHERE id_currency = ?";
        PreparedStatement pstWallet = cnx.prepareStatement(updateWallet);
        pstWallet.setString(1, currency.getNom());
        pstWallet.setInt(2, currency.getId_currency());
        pstWallet.executeUpdate();

        System.out.println("Currency mise à jour : " + currency.getNom());
    }

    @Override
    public void deleteOne(currency currency) throws SQLException {
        String query ="DELETE FROM `currency` WHERE id_currency =?";
        PreparedStatement pst = cnx.prepareStatement(query);
        pst.setInt(1, currency.getId_currency());
        pst.executeUpdate();

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
}

