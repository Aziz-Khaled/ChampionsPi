package tn.esprit.Champions.services;

import tn.esprit.Champions.models.wallet_currency;
import tn.esprit.Champions.utils.DbConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class wallet_currencyService implements CRUD <wallet_currency>
{
    private Connection cnx;
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

        // Vérification de sécurité
        if (idCurrency == 0) {
            throw new SQLException("Currency introuvable : " + walletCurrency.getNom_currency());
        }

        walletCurrency.setId_currency(idCurrency);

        // INSERT sans le solde, il prendra automatiquement la valeur par défaut (0.00)
        String query = "INSERT INTO wallet_currency (id_wallet, id_currency, nom_currency) VALUES (?,?,?)";

        try (PreparedStatement pst = cnx.prepareStatement(query)) {
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

    }

    @Override
    public List<wallet_currency> SelectAll() throws SQLException {
        return List.of();
    }
}
