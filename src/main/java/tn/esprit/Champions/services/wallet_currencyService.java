package tn.esprit.Champions.services;

import tn.esprit.Champions.models.wallet_currency;
import tn.esprit.Champions.utils.DbConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class wallet_currencyService implements CRUD <wallet_currency>
{
    private static Connection cnx;
    public wallet_currencyService()
    {
        cnx = DbConnection.getInstance().getCnx();
    }


    public int getCurrencyIdByName(String nom) throws SQLException {
        String query = "SELECT id_currency FROM currency WHERE nom = ?";
        try (PreparedStatement pst = cnx.prepareStatement(query)) {
            pst.setString(1, nom);
            ResultSet rs = pst.executeQuery();

            if (rs.next()) {
                return rs.getInt("id_currency");
            }
        }
        return 0; // si non trouvé
    }
    @Override
    public void insertOne(wallet_currency walletCurrency) throws SQLException {

        // Récupérer l'id de la currency selon son nom
        int idCurrency = getCurrencyIdByName(walletCurrency.getNom_currency());
        walletCurrency.setId_currency(idCurrency);

        // Vérifier si la currency existe déjà pour ce wallet
        String checkQuery = "SELECT COUNT(*) FROM wallet_currency WHERE id_wallet = ? AND id_currency = ?";
        try (PreparedStatement checkStmt = cnx.prepareStatement(checkQuery)) {
            checkStmt.setInt(1, walletCurrency.getId_wallet());
            checkStmt.setInt(2, walletCurrency.getId_currency());
            ResultSet rs = checkStmt.executeQuery();
            if (rs.next() && rs.getInt(1) > 0) {
                throw new SQLException("Cette currency existe déjà dans ce wallet !");
            }
        }

        // Si pas existante, INSERT
        String insertQuery = "INSERT INTO wallet_currency (id_wallet, id_currency, nom_currency) VALUES (?,?,?)";
        try (PreparedStatement pst = cnx.prepareStatement(insertQuery)) {
            pst.setInt(1, walletCurrency.getId_wallet());
            pst.setInt(2, walletCurrency.getId_currency());
            pst.setString(3, walletCurrency.getNom_currency());
            pst.executeUpdate();
            System.out.println("wallet_currency ajouté avec succès : " + walletCurrency.getNom_currency());
        }
    }

    @Override
    public void updateOne(wallet_currency walletCurrency) throws SQLException {

    }

    @Override
    public void deleteOne(wallet_currency walletCurrency) throws SQLException {
        // Récupérer le solde actuel depuis la DB
        String selectQuery = "SELECT solde FROM wallet_currency WHERE id_wallet = ? AND id_currency = ?";
        try (PreparedStatement pst = cnx.prepareStatement(selectQuery)) {
            pst.setInt(1, walletCurrency.getId_wallet());
            pst.setInt(2, walletCurrency.getId_currency());

            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                double solde = rs.getDouble("solde");
                if (solde > 0) {
                    throw new SQLException("Impossible de supprimer : le solde de cette currency n'est pas nul !");
                }
            } else {
                throw new SQLException("Aucune currency trouvée pour suppression !");
            }
        }

        // Suppression si le solde est nul
        String deleteQuery = "DELETE FROM wallet_currency WHERE id_wallet = ? AND id_currency = ?";
        try (PreparedStatement pst = cnx.prepareStatement(deleteQuery)) {
            pst.setInt(1, walletCurrency.getId_wallet());
            pst.setInt(2, walletCurrency.getId_currency());

            pst.executeUpdate();
            System.out.println("wallet_currency supprimé avec succès : " + walletCurrency.getNom_currency());
        }
    }

    @Override
    public List<wallet_currency> SelectAll() throws SQLException {
        return List.of();
    }


    public static List<wallet_currency> getCurrenciesByWallet(int idWallet) throws SQLException {
        List<wallet_currency> list = new ArrayList<>();

        String query = "SELECT * FROM wallet_currency WHERE id_wallet = ?";
        PreparedStatement pst = cnx.prepareStatement(query);
        pst.setInt(1, idWallet);

        ResultSet rs = pst.executeQuery();

        while (rs.next()) {
            wallet_currency wc = new wallet_currency();
            wc.setId_wallet_currency(rs.getInt("id_wallet_currency"));
            wc.setId_wallet(rs.getInt("id_wallet"));
            wc.setId_currency(rs.getInt("id_currency"));
            wc.setNom_currency(rs.getString("nom_currency"));
            wc.setSolde(rs.getDouble("solde"));

            list.add(wc);
        }

        return list;
    }
    }
