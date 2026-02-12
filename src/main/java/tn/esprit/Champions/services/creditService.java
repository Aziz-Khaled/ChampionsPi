package tn.esprit.Champions.services;

import tn.esprit.Champions.models.CreditStatus;
import tn.esprit.Champions.models.credit;
import tn.esprit.Champions.utils.DbConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class creditService implements CRUD<credit> {

    private Connection cnx;

    public creditService() {
        cnx = DbConnection.getInstance().getCnx();
    }

    @Override
    public void insertOne(credit c) throws SQLException {
        String req = "INSERT INTO `credit` (`project_id`, `borrower_id`, `investisseur_id`, `montant`, `devise`, `taux`, `duree`, `description`, `status`, `contrat_id`, `date_demande`, `date_contrat`) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        PreparedStatement pst = cnx.prepareStatement(req);

        pst.setInt(1, c.getProject_id());
        pst.setInt(2, c.getBorrower_id());
        pst.setInt(3, c.getInvestisseur_id());
        pst.setDouble(4, c.getMontant());
        pst.setString(5, c.getDevise());
        pst.setDouble(6, c.getTaux());
        pst.setInt(7, c.getDuree());
        pst.setString(8, c.getDescription());

        // CORRECTION ENUM : Enum -> String
        pst.setString(9, (c.getStatus() != null) ? c.getStatus().name() : null);

        pst.setString(10, c.getContrat_id());
        pst.setTimestamp(11, c.getDate_demande());
        pst.setTimestamp(12, c.getDate_contrat());

        pst.executeUpdate();
        pst.close();
        System.out.println("Succès : Le crédit a été ajouté !");
    }

    @Override
    public void updateOne(credit credit) throws SQLException {
        String req = "UPDATE `credit` SET `project_id` = ?, `montant` = ?, `status` = ? WHERE `id_credit` = ?";
        PreparedStatement pst = cnx.prepareStatement(req);

        pst.setInt(1, credit.getProject_id());
        pst.setDouble(2, credit.getMontant());

        // CORRECTION ENUM : Enum -> String
        pst.setString(3, (credit.getStatus() != null) ? credit.getStatus().name() : null);

        pst.setInt(4, credit.getId());

        pst.executeUpdate();
        pst.close();
        System.out.println("Crédit mis à jour avec succès !");
    }

    @Override
    public void deleteOne(credit credit) throws SQLException {
        // Sécurisation avec PreparedStatement au lieu de la concaténation
        String req = "DELETE FROM credit WHERE id_credit = ?";
        PreparedStatement pst = cnx.prepareStatement(req);
        pst.setInt(1, credit.getId());
        pst.executeUpdate();
        pst.close();
        System.out.println("Crédit supprimé !");
    }

    @Override
    public List<credit> SelectAll() throws SQLException {
        List<credit> credits = new ArrayList<>();
        String req = "SELECT * FROM `credit`";
        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(req);

        while (rs.next()) {
            credit c = new credit();
            c.setId(rs.getInt("id_credit"));
            c.setProject_id(rs.getInt("project_id"));
            c.setBorrower_id(rs.getInt("borrower_id"));
            c.setInvestisseur_id(rs.getInt("investisseur_id"));
            c.setMontant(rs.getDouble("montant"));
            c.setDevise(rs.getString("devise"));
            c.setTaux(rs.getDouble("taux"));
            c.setDuree(rs.getInt("duree"));
            c.setDescription(rs.getString("description"));

            // CORRECTION ENUM : String -> Enum
            String statusDB = rs.getString("status");
            if (statusDB != null) {
                c.setStatus(CreditStatus.valueOf(statusDB));
            }

            c.setContrat_id(rs.getString("contrat_id"));
            c.setDate_demande(rs.getTimestamp("date_demande"));
            c.setDate_contrat(rs.getTimestamp("date_contrat"));

            credits.add(c);
        }
        rs.close();
        st.close();
        return credits;
    }
}