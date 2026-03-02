package tn.esprit.Champions.services;

import tn.esprit.Champions.models.formations;
import tn.esprit.Champions.models.StatutFormation;
import tn.esprit.Champions.utils.DbConnection;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class FormationService {
    private Connection cnx;

    public FormationService() {
        cnx = DbConnection.getInstance().getCnx();
    }

    // Sécurité : Vérifier si le titre existe déjà
    public boolean existsByTitre(String titre) throws SQLException {
        String sql = "SELECT count(*) FROM formations WHERE titre = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, titre);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1) > 0;
            }
        }
        return false;
    }

    // Version finale avec image_path
    public void insertOne(formations f) throws SQLException {
        String sql = "INSERT INTO formations (titre, description, domaine, dateDebut, dateFin, prix, capaciteMax, statut, user_id, image_path) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, f.getTitre());
            ps.setString(2, f.getDescription());
            ps.setString(3, f.getDomaine());
            ps.setDate(4, Date.valueOf(f.getDateDebut()));
            ps.setDate(5, Date.valueOf(f.getDateFin() != null ? f.getDateFin() : f.getDateDebut().plusDays(30)));
            ps.setDouble(6, f.getPrix());
            ps.setInt(7, f.getCapaciteMax());
            ps.setString(8, f.getStatut().name());
            ps.setLong(9, 1); // ID de l'admin par défaut
            ps.setString(10, f.getImagePath()); // Gestion de l'image
            ps.executeUpdate();
        }
    }

    // Mise à jour incluant potentiellement l'image
    public void updateOne(formations f) throws SQLException {
        String sql = "UPDATE formations SET titre=?, description=?, domaine=?, dateDebut=?, prix=?, statut=?, image_path=? WHERE idFormation=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, f.getTitre());
            ps.setString(2, f.getDescription());
            ps.setString(3, f.getDomaine());
            ps.setDate(4, Date.valueOf(f.getDateDebut()));
            ps.setDouble(5, f.getPrix());
            ps.setString(6, f.getStatut().name());
            ps.setString(7, f.getImagePath());
            ps.setInt(8, f.getIdFormation());
            ps.executeUpdate();
        }
    }

    public void deleteOne(formations f) throws SQLException {
        String sql = "DELETE FROM formations WHERE idFormation=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, f.getIdFormation());
            ps.executeUpdate();
        }
    }

    // Sélection complète avec image_path
    public List<formations> SelectAll() throws SQLException {
        List<formations> list = new ArrayList<>();
        String sql = "SELECT * FROM formations";
        try (Statement st = cnx.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                formations f = new formations();
                f.setIdFormation(rs.getInt("idFormation"));
                f.setTitre(rs.getString("titre"));
                f.setDescription(rs.getString("description"));
                f.setDomaine(rs.getString("domaine"));
                f.setDateDebut(rs.getDate("dateDebut").toLocalDate());
                f.setPrix(rs.getDouble("prix"));
                f.setStatut(StatutFormation.valueOf(rs.getString("statut")));

                // Récupération du chemin de l'image pour l'affichage (style GomyCode)
                f.setImagePath(rs.getString("image_path"));

                list.add(f);
            }
        }
        return list;
    }

    public void updateRating(int id, int note) throws SQLException {
        String sql = "UPDATE formations SET rating = ? WHERE idFormation = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setDouble(1, (double) note);
            ps.setInt(2, id);
            ps.executeUpdate();
        }
    }
}