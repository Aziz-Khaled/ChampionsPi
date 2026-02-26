package tn.esprit.Champions.services;

import tn.esprit.Champions.models.participations;
import tn.esprit.Champions.models.StatutParticipation;
import tn.esprit.Champions.utils.DbConnection;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ParticipationService {
    private Connection cnx;

    public ParticipationService() {
        cnx = DbConnection.getInstance().getCnx();
    }

    public List<participations> SelectAll() throws SQLException {
        List<participations> list = new ArrayList<>();
        // Jointure corrigée : table 'utilisateur' et clé 'id_user'
        String sql = "SELECT p.*, f.titre, u.nom FROM participations p " +
                "JOIN formations f ON p.idFormation = f.idFormation " +
                "JOIN utilisateur u ON p.idUtilisateur = u.id_user";

        try (Statement st = cnx.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                participations p = new participations();
                p.setIdParticipation(rs.getInt("idParticipation"));
                p.setIdFormation(rs.getInt("idFormation"));
                p.setIdUtilisateur(rs.getInt("idUtilisateur"));
                p.setDateInscription(rs.getTimestamp("dateInscription").toLocalDateTime());
                p.setStatut(StatutParticipation.valueOf(rs.getString("statut")));
                p.setPresence(rs.getBoolean("presence"));
                p.setNote(rs.getFloat("note"));

                // Champs pour l'affichage TableView
                p.setTitreFormation(rs.getString("titre"));
                p.setNomUtilisateur(rs.getString("nom"));
                list.add(p);
            }
        }
        return list;
    }

    public void insertOne(participations p) throws SQLException {
        String sql = "INSERT INTO participations (idFormation, idUtilisateur, dateInscription, statut, presence, note) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, p.getIdFormation());
            ps.setInt(2, p.getIdUtilisateur());
            ps.setTimestamp(3, Timestamp.valueOf(p.getDateInscription()));
            ps.setString(4, p.getStatut().name());
            ps.setBoolean(5, p.isPresence());
            ps.setFloat(6, p.getNote() != null ? p.getNote() : 0.0f);
            ps.executeUpdate();
        }
    }

    public void updateOne(participations p) throws SQLException {
        String sql = "UPDATE participations SET idFormation=?, idUtilisateur=?, statut=?, presence=?, note=? WHERE idParticipation=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, p.getIdFormation());
            ps.setInt(2, p.getIdUtilisateur());
            ps.setString(3, p.getStatut().name());
            ps.setBoolean(4, p.isPresence());
            ps.setFloat(5, p.getNote() != null ? p.getNote() : 0.0f);
            ps.setInt(6, p.getIdParticipation());
            ps.executeUpdate();
        }
    }

    public void deleteOne(participations p) throws SQLException {
        String sql = "DELETE FROM participations WHERE idParticipation=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, p.getIdParticipation());
            ps.executeUpdate();
        }
    }
}