package tn.esprit.Champions.services;

import tn.esprit.Champions.models.projetStatus;
import tn.esprit.Champions.utils.DbConnection;
import tn.esprit.Champions.models.projet;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class projetService implements CRUD<projet> {
    private Connection cnx;

    public projetService() {
        cnx = DbConnection.getInstance().getCnx();
    }

    @Override
    public void insertOne(projet p) throws SQLException {
        String req = "INSERT INTO `projet` (`owner_id`, `title`, `description`, `status`, `target_amount`, `start_date`, `end_date`) VALUES (?, ?, ?, ?, ?, ?, ?)";

        PreparedStatement pst = cnx.prepareStatement(req);
        pst.setInt(1, p.getOwner_id());
        pst.setString(2, p.getTitle());
        pst.setString(3, p.getDescription());

        // Correction Enum -> String
        pst.setString(4, (p.getStatus() != null) ? p.getStatus().name() : null);

        pst.setDouble(5, p.getTarget_amount());
        pst.setTimestamp(6, p.getStart_date());
        pst.setTimestamp(7, p.getEnd_date());

        pst.executeUpdate();
        pst.close();
        System.out.println("Projet inséré avec succès !");
    }

    @Override
    public void updateOne(projet p) throws SQLException {
        String req = "UPDATE `projet` SET `owner_id`=?, `title`=?, `description`=?, `status`=?, `target_amount`=?, `start_date`=?, `end_date`=? WHERE `id_projet`=?";

        PreparedStatement pst = cnx.prepareStatement(req);
        pst.setInt(1, p.getOwner_id());
        pst.setString(2, p.getTitle());
        pst.setString(3, p.getDescription());

        // Correction Enum -> String
        pst.setString(4, (p.getStatus() != null) ? p.getStatus().name() : null);

        pst.setDouble(5, p.getTarget_amount());
        pst.setTimestamp(6, p.getStart_date());
        pst.setTimestamp(7, p.getEnd_date());
        pst.setInt(8, p.getId_project());

        pst.executeUpdate();
        pst.close();
        System.out.println("Projet mis à jour avec succès !");
    }

    @Override
    public void deleteOne(projet p) throws SQLException {
        String req = "DELETE FROM `projet` WHERE `id_projet` = ?";

        PreparedStatement pst = cnx.prepareStatement(req);
        pst.setInt(1, p.getId_project());

        pst.executeUpdate();
        pst.close();
        System.out.println("Projet supprimé !");
    }

    @Override
    public List<projet> SelectAll() throws SQLException {
        List<projet> listeProjets = new ArrayList<>();
        String req = "SELECT * FROM `projet`";

        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(req);

        while (rs.next()) {
            projet p = new projet();
            p.setId_project(rs.getInt("id_projet"));
            p.setOwner_id(rs.getInt("owner_id"));
            p.setTitle(rs.getString("title"));
            p.setDescription(rs.getString("description"));

            // Correction String -> Enum
            String statusFromDB = rs.getString("status");
            if (statusFromDB != null) {
                p.setStatus(projetStatus.valueOf(statusFromDB));
            }

            p.setTarget_amount(rs.getFloat("target_amount"));
            p.setStart_date(rs.getTimestamp("start_date"));
            p.setEnd_date(rs.getTimestamp("end_date"));

            listeProjets.add(p);
        }

        rs.close();
        st.close();
        return listeProjets;
    }
}