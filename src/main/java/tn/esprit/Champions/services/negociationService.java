package tn.esprit.Champions.services;

import tn.esprit.Champions.models.Negociation;
import tn.esprit.Champions.utils.DbConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class negociationService implements CRUD<Negociation> {

    private Connection cnx;

    public negociationService() {
        cnx = DbConnection.getInstance().getCnx();
    }

    @Override
    public void insertOne(Negociation n) throws SQLException {
        // Ajout du status par défaut 'PROPOSED' lors de l'insertion
        String req = "INSERT INTO `negociation` (`credit_id`, `investor_id`, `montant`, `taux_propose`, `status`) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement pst = cnx.prepareStatement(req)) {
            pst.setInt(1, n.getCredit_id());
            pst.setInt(2, n.getInvestor_id());
            pst.setDouble(3, n.getMontant());
            pst.setDouble(4, n.getTaux_propose());
            pst.setString(5, (n.getStatus() == null) ? "PROPOSED" : n.getStatus());

            pst.executeUpdate();
            System.out.println("Négociation ajoutée avec succès !");
        }
    }

    @Override
    public void updateOne(Negociation n) throws SQLException {
        String req = "UPDATE `negociation` SET `credit_id` = ?, `investor_id` = ?, `montant` = ?, `taux_propose` = ?, `status` = ? WHERE `id_negociation` = ?";
        try (PreparedStatement pst = cnx.prepareStatement(req)) {
            pst.setInt(1, n.getCredit_id());
            pst.setInt(2, n.getInvestor_id());
            pst.setDouble(3, n.getMontant());
            pst.setDouble(4, n.getTaux_propose());
            pst.setString(5, n.getStatus());
            pst.setInt(6, n.getId_negociation());

            pst.executeUpdate();
            System.out.println("Négociation mise à jour !");
        }
    }

    @Override
    public void deleteOne(Negociation n) throws SQLException {
        String req = "DELETE FROM `negociation` WHERE `id_negociation` = ?";
        try (PreparedStatement pst = cnx.prepareStatement(req)) {
            pst.setInt(1, n.getId_negociation());
            pst.executeUpdate();
            System.out.println("Négociation supprimée !");
        }
    }

    public void accepterNegociation(int id) throws SQLException {
        String req = "UPDATE `negociation` SET `status` = 'ACCEPTED' WHERE `id_negociation` = ?";
        try (PreparedStatement pst = cnx.prepareStatement(req)) {
            pst.setInt(1, id);
            pst.executeUpdate();
        }
    }

    public void refuserNegociation(int id) throws SQLException {
        String req = "UPDATE `negociation` SET `status` = 'REJECTED' WHERE `id_negociation` = ?";
        try (PreparedStatement pst = cnx.prepareStatement(req)) {
            pst.setInt(1, id);
            pst.executeUpdate();
        }
    }

    public List<Negociation> getOffersByCredit(int creditId) throws SQLException {
        List<Negociation> list = new ArrayList<>();
        String req = "SELECT * FROM `negociation` WHERE `credit_id` = ?";
        try (PreparedStatement pst = cnx.prepareStatement(req)) {
            pst.setInt(1, creditId);
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToNegociation(rs));
                }
            }
        }
        return list;
    }

    @Override
    public List<Negociation> SelectAll() throws SQLException {
        List<Negociation> negociations = new ArrayList<>();
        String req = "SELECT * FROM `negociation`";
        try (Statement st = cnx.createStatement();
             ResultSet rs = st.executeQuery(req)) {
            while (rs.next()) {
                negociations.add(mapResultSetToNegociation(rs));
            }
        }
        return negociations;
    }

    /**
     * Méthode utilitaire pour transformer une ligne SQL en objet Java
     * C'est ici que le "remplissage" se fait pour chaque colonne.
     */
    private Negociation mapResultSetToNegociation(ResultSet rs) throws SQLException {
        Negociation n = new Negociation();
        n.setId_negociation(rs.getInt("id_negociation"));
        n.setCredit_id(rs.getInt("credit_id"));
        n.setInvestor_id(rs.getInt("investor_id"));
        n.setMontant(rs.getDouble("montant"));
        n.setTaux_propose(rs.getDouble("taux_propose"));
        n.setStatus(rs.getString("status")); // Récupération du status depuis SQL
        return n;
    }
}