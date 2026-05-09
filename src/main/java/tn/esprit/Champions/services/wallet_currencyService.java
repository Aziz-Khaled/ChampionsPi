package tn.esprit.Champions.services;

import tn.esprit.Champions.models.wallet_currency;
import tn.esprit.Champions.utils.DbConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class wallet_currencyService implements CRUD<wallet_currency> {
    private static Connection cnx;

    public wallet_currencyService() {
        cnx = DbConnection.getInstance().getCnx();
    }

    public int getCurrencyIdByName(String nom) throws SQLException {
        String query = "SELECT id_currency FROM currency WHERE nom = ?";
        try (PreparedStatement pst = cnx.prepareStatement(query)) {
            pst.setString(1, nom);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id_currency");
                }
            }
        }
        return 0; // si non trouvé
    }

    @Override
    public void insertOne(wallet_currency walletCurrency) throws SQLException {

        // 1. Déterminer l'ID de la devise et s'assurer que le nom est présent
        int idCurrency = walletCurrency.getId_currency();
        String nomCurrency = walletCurrency.getNom_currency();

        if (idCurrency == 0 && nomCurrency != null) {
            idCurrency = getCurrencyIdByName(nomCurrency);
            walletCurrency.setId_currency(idCurrency);
        }

        if (idCurrency == 0) {
            throw new SQLException("Impossible de déterminer l'ID de la devise.");
        }
        if (walletCurrency.getNom_currency() == null) {
            throw new SQLException("Le nom de la devise est obligatoire.");
        }

        // --- LA RÈGLE DE RESTRICTION TRADING A ÉTÉ SUPPRIMÉE ICI ---

        // 4. Vérifier si la currency existe déjà dans ce wallet
        String checkQuery = "SELECT COUNT(*) FROM wallet_currency WHERE id_wallet_id = ? AND id_currency_id = ?";
        try (PreparedStatement checkStmt = cnx.prepareStatement(checkQuery)) {
            checkStmt.setInt(1, walletCurrency.getId_wallet());
            checkStmt.setInt(2, walletCurrency.getId_currency());
            try (ResultSet rs = checkStmt.executeQuery()) {
                if (rs.next() && rs.getInt(1) > 0) {
                    throw new SQLException("Cette currency existe déjà dans ce wallet !");
                }
            }
        }

        // 5. INSERT wallet_currency
        String insertQuery = "INSERT INTO wallet_currency (id_wallet_id, id_currency_id, nom_currency, solde) VALUES (?,?,?,?)";
        try (PreparedStatement pst = cnx.prepareStatement(insertQuery)) {
            pst.setInt(1, walletCurrency.getId_wallet());
            pst.setInt(2, walletCurrency.getId_currency());
            pst.setString(3, walletCurrency.getNom_currency());
            pst.setDouble(4, walletCurrency.getSolde());
            pst.executeUpdate();
            System.out.println("wallet_currency ajouté avec succès : " + walletCurrency.getNom_currency() + " solde : " + walletCurrency.getSolde());
        }

        // 6. Mettre à jour la date_derniere_modification du wallet
        updateWalletModificationDate(walletCurrency.getId_wallet());
    }

    public wallet_currency getWalletCurrencyForUpdate(int walletId, int currencyId) throws SQLException {
        String query = "SELECT * FROM wallet_currency WHERE id_wallet_id = ? AND id_currency_id = ? FOR UPDATE";
        try (PreparedStatement pst = cnx.prepareStatement(query)) {
            pst.setInt(1, walletId);
            pst.setInt(2, currencyId);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    wallet_currency wc = new wallet_currency();
                    wc.setId_wallet_currency(rs.getInt("id_wallet_currency"));
                    wc.setId_wallet(rs.getInt("id_wallet_id"));
                    wc.setId_currency(rs.getInt("id_currency_id"));
                    wc.setSolde(rs.getDouble("solde"));
                    wc.setNom_currency(rs.getString("nom_currency"));
                    return wc;
                }
            }
        }
        return null;
    }

    @Override
    public void updateOne(wallet_currency wc) throws SQLException {
        String query = "UPDATE wallet_currency SET solde = ? WHERE id_wallet_id = ? AND id_currency_id = ?";
        try (PreparedStatement pst = cnx.prepareStatement(query)) {
            pst.setDouble(1, wc.getSolde());
            pst.setInt(2, wc.getId_wallet());
            pst.setInt(3, wc.getId_currency());
            pst.executeUpdate();
        }
        updateWalletModificationDate(wc.getId_wallet());
    }

    @Override
    public void deleteOne(wallet_currency walletCurrency) throws SQLException {
        String selectQuery = "SELECT solde FROM wallet_currency WHERE id_wallet_id = ? AND id_currency_id = ?";
        try (PreparedStatement pst = cnx.prepareStatement(selectQuery)) {
            pst.setInt(1, walletCurrency.getId_wallet());
            pst.setInt(2, walletCurrency.getId_currency());
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    double solde = rs.getDouble("solde");
                    if (solde > 0) {
                        throw new SQLException("Impossible de supprimer : le solde de cette currency n'est pas nul !");
                    }
                } else {
                    throw new SQLException("Aucune currency trouvée pour suppression !");
                }
            }
        }

        String deleteQuery = "DELETE FROM wallet_currency WHERE id_wallet_id = ? AND id_currency_id = ?";
        try (PreparedStatement pst = cnx.prepareStatement(deleteQuery)) {
            pst.setInt(1, walletCurrency.getId_wallet());
            pst.setInt(2, walletCurrency.getId_currency());
            pst.executeUpdate();
            System.out.println("wallet_currency supprimé avec succès : " + walletCurrency.getNom_currency());
        }
        updateWalletModificationDate(walletCurrency.getId_wallet());
    }

    @Override
    public List<wallet_currency> SelectAll() throws SQLException {
        List<wallet_currency> list = new ArrayList<>();
        String query = "SELECT * FROM wallet_currency";
        try (PreparedStatement pst = cnx.prepareStatement(query);
             ResultSet rs = pst.executeQuery()) {
            while (rs.next()) {
                wallet_currency wc = new wallet_currency();
                wc.setId_wallet_currency(rs.getInt("id_wallet_currency"));
                wc.setId_wallet(rs.getInt("id_wallet_id"));
                wc.setId_currency(rs.getInt("id_currency_id"));
                wc.setNom_currency(rs.getString("nom_currency"));
                wc.setSolde(rs.getDouble("solde"));
                list.add(wc);
            }
        }
        return list;
    }

    public static List<wallet_currency> getCurrenciesByWallet(int idWallet) throws SQLException {
        List<wallet_currency> list = new ArrayList<>();
        String query = "SELECT * FROM wallet_currency WHERE id_wallet_id = ?";
        try (PreparedStatement pst = cnx.prepareStatement(query)) {
            pst.setInt(1, idWallet);
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    wallet_currency wc = new wallet_currency();
                    wc.setId_wallet_currency(rs.getInt("id_wallet_currency"));
                    wc.setId_wallet(rs.getInt("id_wallet_id"));
                    wc.setId_currency(rs.getInt("id_currency_id"));
                    wc.setNom_currency(rs.getString("nom_currency"));
                    wc.setSolde(rs.getDouble("solde"));
                    list.add(wc);
                }
            }
        }
        return list;
    }

    public wallet_currency getWalletCurrencyByWalletAndId(int idWallet, int idCurrency) throws SQLException {
        String query = "SELECT * FROM wallet_currency WHERE id_wallet_id = ? AND id_currency_id = ?";
        try (PreparedStatement pst = cnx.prepareStatement(query)) {
            pst.setInt(1, idWallet);
            pst.setInt(2, idCurrency);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    wallet_currency wc = new wallet_currency();
                    wc.setId_wallet_currency(rs.getInt("id_wallet_currency"));
                    wc.setId_wallet(rs.getInt("id_wallet_id"));
                    wc.setId_currency(rs.getInt("id_currency_id"));
                    wc.setNom_currency(rs.getString("nom_currency"));
                    wc.setSolde(rs.getDouble("solde"));
                    return wc;
                }
            }
        }
        return null;
    }

    public double getBalance(int walletId, int currencyId) throws SQLException {
        String query = "SELECT solde FROM wallet_currency WHERE id_wallet_id = ? AND id_currency_id = ?";
        try (PreparedStatement pst = cnx.prepareStatement(query)) {
            pst.setInt(1, walletId);
            pst.setInt(2, currencyId);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble("solde");
                }
            }
        }
        return 0;
    }

    private void updateWalletModificationDate(int walletId) throws SQLException {
        String updateQuery = "UPDATE wallet SET date_derniere_modification = NOW() WHERE id_wallet = ?";
        try (PreparedStatement pst = cnx.prepareStatement(updateQuery)) {
            pst.setInt(1, walletId);
            pst.executeUpdate();
        }
    }


    public int getTradingWalletIdByUser(int userId) throws SQLException {
        String sql = "SELECT id_wallet FROM wallet WHERE id_user = ? AND type_wallet = 'TRADING' LIMIT 1";
        try (PreparedStatement pst = cnx.prepareStatement(sql)) {
            pst.setInt(1, userId);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                return rs.getInt("id_wallet");
            }
        }
        return -1;
    }
}