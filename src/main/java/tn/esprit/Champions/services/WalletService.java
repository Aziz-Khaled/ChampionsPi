package tn.esprit.Champions.services;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
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
        String query =" INSERT INTO `wallet`( `type_wallet`, `categorie`, `statut`)"
        + "VALUES (?,?,?)";
        PreparedStatement pst = cnx.prepareStatement(query);
        pst.setString(1, wallet.getTypeWallet().name());
        pst.setString(2, wallet.getCategorie());
        pst.setString(3, String.valueOf(wallet.getStatut()));
        pst.executeUpdate();
    }

    @Override
    public void updateOne(wallet wallet) throws SQLException {
        String query ="UPDATE `wallet` SET `type_wallet`=?,`categorie`=?,`solde`=?,`statut`=? WHERE id_wallet=?";
        PreparedStatement pst = cnx.prepareStatement(query);
        pst.setString(1, wallet.getTypeWallet().name());
        pst.setString(2, wallet.getCategorie());
        pst.setDouble(3,wallet.getSolde());
        pst.setString(4, String.valueOf(wallet.getStatut()));
        pst.setInt(5, wallet.getIdWallet());
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
            wallet.setIdWallet(rs.getInt("id_wallet"));
            wallet.setTypeWallet(typeWallet.valueOf(rs.getString("type_wallet")));
            wallet.setCategorie(rs.getString("categorie"));
            wallet.setSolde(rs.getDouble("solde"));
            wallets.add(wallet);
        }
        return wallets;
    }
}
