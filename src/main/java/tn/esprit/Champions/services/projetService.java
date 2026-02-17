package tn.esprit.Champions.services;

import tn.esprit.Champions.models.projetStatus;
import tn.esprit.Champions.utils.DbConnection;
import tn.esprit.Champions.models.projet;
import tn.esprit.Champions.models.Utilisateur;

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
        // Ajout de image_url dans la requête
        String req = "INSERT INTO `projet` (`owner_id`, `title`, `description`, `status`, `target_amount`, `start_date`, `end_date`, `image_url`) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        PreparedStatement pst = cnx.prepareStatement(req);

        if (p.getOwner_id() != null) {
            pst.setInt(1, p.getOwner_id().getId_user());
        } else {
            throw new SQLException("Erreur : Le projet doit avoir un propriétaire (Owner).");
        }

        pst.setString(2, p.getTitle());
        pst.setString(3, p.getDescription());
        pst.setString(4, (p.getStatus() != null) ? p.getStatus().name() : null);
        pst.setDouble(5, p.getTarget_amount());
        pst.setTimestamp(6, p.getStart_date());
        pst.setTimestamp(7, p.getEnd_date());

        // --- NOUVEAU : Insertion de l'URL de l'image ---
        pst.setString(8, p.getImageUrl());

        pst.executeUpdate();
        pst.close();
        System.out.println("Projet inséré avec succès avec image !");
    }

    @Override
    public void updateOne(projet p) throws SQLException {
        // Mise à jour de image_url incluse
        String req = "UPDATE `projet` SET `owner_id`=?, `title`=?, `description`=?, `status`=?, `target_amount`=?, `start_date`=?, `end_date`=?, `image_url`=? WHERE `id_projet`=?";

        PreparedStatement pst = cnx.prepareStatement(req);

        pst.setInt(1, p.getOwner_id().getId_user());
        pst.setString(2, p.getTitle());
        pst.setString(3, p.getDescription());
        pst.setString(4, (p.getStatus() != null) ? p.getStatus().name() : null);
        pst.setDouble(5, p.getTarget_amount());
        pst.setTimestamp(6, p.getStart_date());
        pst.setTimestamp(7, p.getEnd_date());

        // --- NOUVEAU : Mise à jour de l'image ---
        pst.setString(8, p.getImageUrl());

        pst.setInt(9, p.getId_project());

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

            Utilisateur owner = new Utilisateur();
            owner.setId_user(rs.getInt("owner_id"));
            p.setOwner_id(owner);

            p.setTitle(rs.getString("title"));
            p.setDescription(rs.getString("description"));

            String statusFromDB = rs.getString("status");
            if (statusFromDB != null) {
                p.setStatus(projetStatus.valueOf(statusFromDB));
            }

            p.setTarget_amount(rs.getFloat("target_amount"));
            p.setStart_date(rs.getTimestamp("start_date"));
            p.setEnd_date(rs.getTimestamp("end_date"));

            // --- NOUVEAU : Lecture de l'URL de l'image depuis la DB ---
            p.setImageUrl(rs.getString("image_url"));

            listeProjets.add(p);
        }
        rs.close();
        st.close();
        return listeProjets;
    }

    public projet findById(int id) throws SQLException {
        String req = "SELECT * FROM `projet` WHERE `id_projet` = ?";
        PreparedStatement pst = cnx.prepareStatement(req);
        pst.setInt(1, id);
        ResultSet rs = pst.executeQuery();

        if (rs.next()) {
            projet p = new projet();
            p.setId_project(rs.getInt("id_projet"));

            Utilisateur owner = new Utilisateur();
            owner.setId_user(rs.getInt("owner_id"));
            p.setOwner_id(owner);

            p.setTitle(rs.getString("title"));
            p.setDescription(rs.getString("description"));

            String statusFromDB = rs.getString("status");
            if (statusFromDB != null) {
                p.setStatus(projetStatus.valueOf(statusFromDB));
            }

            p.setTarget_amount(rs.getFloat("target_amount"));
            p.setStart_date(rs.getTimestamp("start_date"));
            p.setEnd_date(rs.getTimestamp("end_date"));

            // --- NOUVEAU : Lecture de l'image ---
            p.setImageUrl(rs.getString("image_url"));

            return p;
        }
        return null;
    }

    public int getTotalProjets() {
        int count = 0;
        String query = "SELECT COUNT(*) FROM `projet`";
        try (Statement st = cnx.createStatement();
             ResultSet rs = st.executeQuery(query)) {
            if (rs.next()) count = rs.getInt(1);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return count;
    }
}