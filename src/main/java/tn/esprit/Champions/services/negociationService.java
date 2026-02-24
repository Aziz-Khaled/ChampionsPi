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
        String req = "INSERT INTO `negociation` (`credit_id`, `investor_id`, `montant`, `taux_propose`) VALUES (?, ?, ?, ?)";
        PreparedStatement pst = cnx.prepareStatement(req);

        pst.setInt(1, n.getCredit_id());
        pst.setInt(2, n.getInvestor_id());
        pst.setDouble(3, n.getMontant());
        pst.setDouble(4, n.getTaux_propose());

        pst.executeUpdate();
        pst.close();
        System.out.println("Négociation ajoutée avec succès !");
    }

    @Override
    public void updateOne(Negociation n) throws SQLException {
        String req = "UPDATE `negociation` SET `credit_id` = ?, `investor_id` = ?, `montant` = ?, `taux_propose` = ? WHERE `id_negociation` = ?";
        PreparedStatement pst = cnx.prepareStatement(req);

        pst.setInt(1, n.getCredit_id());
        pst.setInt(2, n.getInvestor_id());
        pst.setDouble(3, n.getMontant());
        pst.setDouble(4, n.getTaux_propose());
        pst.setInt(5, n.getId_negociation());

        pst.executeUpdate();
        pst.close();
        System.out.println("Négociation mise à jour !");
    }

    @Override
    public void deleteOne(Negociation n) throws SQLException {
        String req = "DELETE FROM `negociation` WHERE `id_negociation` = ?";
        PreparedStatement pst = cnx.prepareStatement(req);
        pst.setInt(1, n.getId_negociation());
        pst.executeUpdate();
        pst.close();
        System.out.println("Négociation supprimée !");
    }
    // Méthode pour accepter un deal
    public void accepterNegociation(int id) throws SQLException {
        String req = "UPDATE `negociation` SET `status` = 'ACCEPTED' WHERE `id_negociation` = ?";
        PreparedStatement pst = cnx.prepareStatement(req);
        pst.setInt(1, id);
        pst.executeUpdate();
    }

    // Méthode pour refuser un deal
    public void refuserNegociation(int id) throws SQLException {
        String req = "UPDATE `negociation` SET `status` = 'REJECTED' WHERE `id_negociation` = ?";
        PreparedStatement pst = cnx.prepareStatement(req);
        pst.setInt(1, id);
        pst.executeUpdate();
    }
    // Dans negociationService.java
    public List<Negociation> getOffersByCredit(int creditId) throws SQLException {
        List<Negociation> list = new ArrayList<>();
        String req = "SELECT * FROM `negociation` WHERE `credit_id` = ?";
        PreparedStatement pst = cnx.prepareStatement(req);
        pst.setInt(1, creditId);
        ResultSet rs = pst.executeQuery();

        while (rs.next()) {
            Negociation n = new Negociation(
                    rs.getInt("id_negociation"),
                    rs.getInt("credit_id"),
                    rs.getInt("investor_id"),
                    rs.getDouble("montant"),
                    rs.getDouble("taux_propose")
            );
            list.add(n);
        }
        return list;
    }

    @Override
    public List<Negociation> SelectAll() throws SQLException {
        List<Negociation> negociations = new ArrayList<>();
        String req = "SELECT * FROM `negociation`";
        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(req);

        while (rs.next()) {
            Negociation n = new Negociation();
            n.setId_negociation(rs.getInt("id_negociation"));
            n.setCredit_id(rs.getInt("credit_id"));
            n.setInvestor_id(rs.getInt("investor_id"));
            n.setMontant(rs.getDouble("montant"));
            n.setTaux_propose(rs.getDouble("taux_propose"));

            negociations.add(n);
        }
        rs.close();
        st.close();
        return negociations;
    }
}