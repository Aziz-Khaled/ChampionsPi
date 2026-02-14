package tn.esprit.Champions.services;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import tn.esprit.Champions.models.statutWallet;
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
        pst.setInt(3, wallet.getIdUser());

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
        // On ne met à jour que le statut
        String query = "UPDATE `wallet` SET `statut`=? WHERE id_wallet=?";
        PreparedStatement pst = cnx.prepareStatement(query);
        pst.setString(1, String.valueOf(wallet.getStatut()));
        pst.setInt(2, wallet.getIdWallet());
        pst.executeUpdate();
    }

    @Override
    public void deleteOne(wallet wallet) throws SQLException {

        String checkQuery = "SELECT SUM(solde) AS total_solde FROM wallet_currency WHERE id_wallet=?";
        PreparedStatement checkStmt = cnx.prepareStatement(checkQuery);
        checkStmt.setInt(1, wallet.getIdWallet());
        ResultSet rs = checkStmt.executeQuery();

        if (rs.next()) {
            double totalSolde = rs.getDouble("total_solde");
            if (totalSolde > 0) {
                throw new SQLException("Impossible de supprimer : le wallet contient des soldes non nuls !");
            }
        }


        String query = "DELETE FROM `wallet` WHERE id_wallet=?";
        PreparedStatement pst = cnx.prepareStatement(query);
        pst.setInt(1, wallet.getIdWallet());
        pst.executeUpdate();
    }

    @Override
    public List<wallet> SelectAll() throws SQLException {
        List<wallet> wallets = new ArrayList<>();
        String query = "SELECT * FROM wallet";
        PreparedStatement pst = cnx.prepareStatement(query);
        ResultSet rs = pst.executeQuery();

        while (rs.next()) {
            wallet wallet = new wallet();

            wallet.setIdWallet(rs.getInt("id_wallet"));
            wallet.setIdUser(rs.getInt("id_user"));

            String type = rs.getString("type_wallet");
            if (type != null) {
                wallet.setTypeWallet(typeWallet.valueOf(type));
            }

            wallet.setSolde(rs.getDouble("solde"));

            String statut = rs.getString("statut");
            if (statut != null) {
                wallet.setStatut(statutWallet.valueOf(statut));
            }

            wallets.add(wallet);
        }
        return wallets;
    }
}
