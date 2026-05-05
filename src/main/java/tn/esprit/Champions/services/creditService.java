package tn.esprit.Champions.services;

import tn.esprit.Champions.models.CreditStatus;
import tn.esprit.Champions.models.Utilisateur;
import tn.esprit.Champions.models.credit;
import tn.esprit.Champions.utils.DbConnection;
import tn.esprit.Champions.utils.UserSession;

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

        int userId = UserSession.getLoggedInUser().getId_user();
        String req = "INSERT INTO `credit` (`project_id`, `borrower_id`, `montant`, `devise`, `taux`, `duree`, `description`, `status`, `date_demande`) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        PreparedStatement pst = cnx.prepareStatement(req);

        pst.setInt(1, c.getProject_id());

        // CORRECTION : On extrait l'ID de l'objet Utilisateur
        pst.setInt(2, userId);

        pst.setDouble(3, c.getMontant());
        pst.setString(4, c.getDevise());
        pst.setDouble(5, c.getTaux());
        pst.setInt(6, c.getDuree());
        pst.setString(7, c.getDescription());
        pst.setString(8, (c.getStatus() != null) ? c.getStatus().name() : "PENDING");
        pst.setTimestamp(9, new Timestamp(System.currentTimeMillis()));

        pst.executeUpdate();
        pst.close();
        System.out.println("Succès : Le crédit a été ajouté !");
    }

    @Override
    public void updateOne(credit credit) throws SQLException {
        String req = "UPDATE `credit` SET `project_id` = ?, `montant` = ?, `devise` = ?, " +
                "`taux` = ?, `duree` = ?, `description` = ?, `status` = ? " +
                "WHERE `id_credit` = ?";

        PreparedStatement pst = cnx.prepareStatement(req);

        pst.setInt(1, credit.getProject_id());
        pst.setDouble(2, credit.getMontant());
        pst.setString(3, credit.getDevise());
        pst.setDouble(4, credit.getTaux());
        pst.setInt(5, credit.getDuree());
        pst.setString(6, credit.getDescription());
        pst.setString(7, (credit.getStatus() != null) ? credit.getStatus().name() : "OPEN");
        pst.setInt(8, credit.getId());

        pst.executeUpdate();
        pst.close();
        System.out.println("Crédit mis à jour avec succès !");
    }

    @Override
    public void deleteOne(credit credit) throws SQLException {
        String req = "DELETE FROM credit WHERE id_credit = ?";
        PreparedStatement pst = cnx.prepareStatement(req);
        pst.setInt(1, credit.getId());
        pst.executeUpdate();
        pst.close();
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

            // CORRECTION : On crée des objets Utilisateur et on leur donne l'ID lu en DB
            Utilisateur borrower = new Utilisateur();
            borrower.setId_user(rs.getInt("borrower_id"));
            c.setBorrower_id(borrower);

            Utilisateur investisseur = new Utilisateur();
            investisseur.setId_user(rs.getInt("investisseur_id"));
            c.setInvestisseur_id(investisseur);

            c.setMontant(rs.getDouble("montant"));
            c.setDevise(rs.getString("devise"));
            c.setTaux(rs.getDouble("taux"));
            c.setDuree(rs.getInt("duree"));
            c.setDescription(rs.getString("description"));

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

    public int getTotalCredits() {
        int count = 0;
        String query = "SELECT COUNT(*) FROM `credit`";
        try (Statement st = cnx.createStatement();
             ResultSet rs = st.executeQuery(query)) {
            if (rs.next()) count = rs.getInt(1);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return count;
    }

    public List<Utilisateur> getAllUsersForCombo() {
        List<Utilisateur> list = new ArrayList<>();
        String req = "SELECT id_user, nom, prenom FROM utilisateur";
        try {
            Statement st = cnx.createStatement();
            ResultSet rs = st.executeQuery(req);
            while (rs.next()) {
                // Utilisation d'un constructeur simplifié ou de setters
                Utilisateur u = new Utilisateur();
                u.setId_user(rs.getInt("id_user"));
                u.setNom(rs.getString("nom"));
                u.setPrenom(rs.getString("prenom"));
                list.add(u);
            }
            rs.close();
            st.close();
        } catch (SQLException e) {
            System.out.println("Erreur lecture utilisateurs : " + e.getMessage());
        }
        return list;
    }
}