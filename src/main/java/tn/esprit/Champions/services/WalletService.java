package tn.esprit.Champions.services;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import tn.esprit.Champions.models.statutWallet;
import tn.esprit.Champions.models.typeWallet;
import tn.esprit.Champions.models.wallet;
import tn.esprit.Champions.utils.DbConnection;
import tn.esprit.Champions.utils.UserSession;

public class WalletService implements CRUD<wallet> {
    private Connection cnx;

    public WalletService() {
        cnx = DbConnection.getInstance().getCnx();
    }
    private String generateRIB() {
        int number = (int)(Math.random() * 90000000) + 10000000;
        return String.valueOf(number);
    }

    private String generateUniqueRIB() throws SQLException {
        String rib;
        boolean exists;

        do {
            rib = generateRIB();

            String sql = "SELECT COUNT(*) FROM wallet WHERE rib = ?";
            PreparedStatement ps = cnx.prepareStatement(sql);
            ps.setString(1, rib);
            ResultSet rs = ps.executeQuery();
            rs.next();

            exists = rs.getInt(1) > 0;

            rs.close();
            ps.close();

        } while (exists);

        return rib;
    }
    public wallet getByRib(String rib) throws SQLException {
        String query = "SELECT * FROM wallet WHERE rib = ?";
        try (PreparedStatement pst = cnx.prepareStatement(query)) {
            pst.setString(1, rib);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    wallet w = new wallet();
                    w.setIdWallet(rs.getInt("id_wallet"));
                    w.setRib(rs.getString("rib"));
                    w.setTypeWallet(typeWallet.valueOf(rs.getString("type_wallet")));
                    w.setStatut(statutWallet.valueOf(rs.getString("statut")));
                    w.setSolde(rs.getDouble("solde"));
                    return w;
                } else {
                    return null; // wallet non trouvé
                }
            }
        }
    }
    public String getRibById(int idWallet) throws SQLException {
        String query = "SELECT rib FROM wallet WHERE id_wallet = ?";
        PreparedStatement pst = cnx.prepareStatement(query);
        pst.setInt(1, idWallet);
        ResultSet rs = pst.executeQuery();
        if (rs.next()) return rs.getString("rib");
        else throw new SQLException("Wallet introuvable pour id " + idWallet);
    }
    public wallet getWalletById(int idWallet) {
        wallet w = null;
        try {
            String query = "SELECT * FROM wallet WHERE id_wallet = ?";
            PreparedStatement pst = cnx.prepareStatement(query);
            pst.setInt(1, idWallet);
            ResultSet rs = pst.executeQuery();

            if (rs.next()) {
                w = new wallet();
                w.setIdWallet(rs.getInt("id_wallet"));
                w.setIdUser(rs.getInt("id_user"));

                String type = rs.getString("type_wallet");
                if (type != null) {
                    w.setTypeWallet(typeWallet.valueOf(type));
                }

                w.setSolde(rs.getDouble("solde"));

                String statut = rs.getString("statut");
                if (statut != null) {
                    w.setStatut(statutWallet.valueOf(statut));
                }
            }

            rs.close();
            pst.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return w;
    }


    public String getNomProprietaire(int userId) {
        String nomComplet = "Inconnu";
        try {
            String query = "SELECT prenom, nom FROM utilisateur WHERE id_user = ?";
            PreparedStatement ps = cnx.prepareStatement(query); // <-- utiliser l'instance cnx
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                nomComplet = rs.getString("prenom") + " " + rs.getString("nom");
            }
            rs.close();
            ps.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return nomComplet;
    }


    @Override
    public void insertOne(wallet wallet) throws SQLException {
        String rib = generateUniqueRIB();
        int userId = UserSession.getLoggedInUser().getId_user();
        wallet.setRib(rib); // on met le RIB dans l'objet

        String query = "INSERT INTO wallet (type_wallet, statut, id_user, rib, date_creation, date_derniere_modification) " +
                "VALUES (?, ?, ?, ?, NOW(), NOW())";
        PreparedStatement pst = cnx.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
        pst.setString(1, wallet.getTypeWallet().name());
        pst.setString(2, wallet.getStatut().name());
        pst.setInt(3, userId);
        pst.setString(4, wallet.getRib());
        pst.executeUpdate();

        ResultSet rs = pst.getGeneratedKeys();
        if (rs.next()) {
            wallet.setIdWallet(rs.getInt(1));
            wallet.setRib(rib);
        }

        rs.close();
        pst.close();

        System.out.println("Wallet inséré avec ID : " + wallet.getIdWallet() + " et RIB : " + wallet.getRib());
    }

    @Override
    public void updateOne(wallet wallet) throws SQLException {
        String query = "UPDATE wallet SET statut=? WHERE id_wallet=?";
        PreparedStatement pst = cnx.prepareStatement(query);
        pst.setString(1, wallet.getStatut().name());
        pst.setInt(2, wallet.getIdWallet());
        pst.executeUpdate();
        pst.close();
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
                rs.close();
                checkStmt.close();
                throw new SQLException("Impossible de supprimer : le wallet contient des soldes non nuls !");
            }
        }
        rs.close();
        checkStmt.close();

        String query = "DELETE FROM wallet WHERE id_wallet=?";
        PreparedStatement pst = cnx.prepareStatement(query);
        pst.setInt(1, wallet.getIdWallet());
        pst.executeUpdate();
        pst.close();
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

            // ⚡ Ajouter cette ligne pour récupérer le RIB
            wallet.setRib(rs.getString("rib"));

            wallets.add(wallet);
        }

        rs.close();
        pst.close();

        return wallets;
    }
    public wallet SelectById(int idWallet) {
        String query = "SELECT * FROM wallet WHERE id_wallet = ?";
        try (PreparedStatement pst = cnx.prepareStatement(query)) {
            pst.setInt(1, idWallet);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                wallet w = new wallet();
                w.setIdWallet(rs.getInt("id_wallet"));
                w.setIdUser(rs.getInt("id_user"));
                w.setRib(rs.getString("rib"));

                String typeStr = rs.getString("type_wallet");
                if (typeStr != null) w.setTypeWallet(typeWallet.valueOf(typeStr));

                String statutStr = rs.getString("statut");
                if (statutStr != null) w.setStatut(statutWallet.valueOf(statutStr));

                w.setSolde(rs.getDouble("solde"));

                // -------- Dates --------
                Timestamp tsCreation = rs.getTimestamp("date_creation");
                if (tsCreation != null) w.setDateCreation(tsCreation.toLocalDateTime());

                Timestamp tsModification = rs.getTimestamp("date_derniere_modification");
                if (tsModification != null) w.setDateDerniereModification(tsModification.toLocalDateTime());

                return w;
            } else {
                return null;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    public Connection getCnx() {
        return cnx;
    }
}