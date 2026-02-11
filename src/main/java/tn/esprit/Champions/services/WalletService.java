package tn.esprit.Champions.services;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import tn.esprit.Champions.models.typeWallet;
import tn.esprit.Champions.models.wallet;
import tn.esprit.Champions.utils.DbConnection;

public class WalletService implements CRUD <wallet>
{
    private Connection cnx;
    public WalletService()
    {
        cnx = DbConnection.getInstance().getCnx();
    }

    @Override
    public void insertOne(wallet wallet) throws SQLException {
        String query = "INSERT INTO wallet (type_wallet, statut, id_user) VALUES (?, ?, ?)";

        PreparedStatement pst = cnx.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
        pst.setString(1, wallet.getTypeWallet().name());
        pst.setString(2, wallet.getStatut().name());
        pst.setInt(3, wallet.getIdUser());  // mieux utiliser setInt pour un int

        pst.executeUpdate();

        // Récupérer l'ID auto-généré
        ResultSet rs = pst.getGeneratedKeys();
        if (rs.next()) {
            wallet.setIdWallet(rs.getInt(1));
        }

        System.out.println("Wallet inséré avec ID : " + wallet.getIdWallet());
    }

    @Override
    public void updateOne(wallet wallet) throws SQLException {
        String query ="UPDATE `wallet` SET `type_wallet`=?,`solde`=?,`statut`=? WHERE id_wallet=?";
        PreparedStatement pst = cnx.prepareStatement(query);
        pst.setString(1, wallet.getTypeWallet().name());
        pst.setDouble(2,wallet.getSolde());
        pst.setString(3, String.valueOf(wallet.getStatut()));
        pst.setInt(4, wallet.getIdWallet());
        pst.executeUpdate();

    }

    @Override
    public void deleteOne(wallet wallet) throws SQLException {
        String query ="DELETE FROM `wallet` WHERE id_wallet=?";
        PreparedStatement pst = cnx.prepareStatement(query);
        pst.setInt(1, wallet.getIdWallet());
        pst.executeUpdate();

    }

    @Override
    public List<wallet> SelectAll() throws SQLException {
        List<wallet> wallets = new ArrayList<>();
        String query ="SELECT * FROM `wallet`";
        PreparedStatement pst = cnx.prepareStatement(query);
        ResultSet rs = pst.executeQuery();
        while (rs.next()) {
            wallet wallet = new wallet();
            wallet.setTypeWallet(typeWallet.valueOf(rs.getString("type_wallet")));
            wallet.setSolde(rs.getDouble("solde"));
            wallets.add(wallet);
        }
        return wallets;
    }
}
