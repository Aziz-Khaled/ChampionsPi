package tn.esprit.Champions.services;

import tn.esprit.Champions.models.participations;
import tn.esprit.Champions.models.StatutParticipation;
import tn.esprit.Champions.utils.DbConnection;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ParticipationService {
    private Connection cnx;

    public ParticipationService() {
        cnx = DbConnection.getInstance().getCnx();
    }

    /**
     * Récupère toutes les participations avec les noms des formations et utilisateurs.
     */
    public List<participations> SelectAll() throws SQLException {
        List<participations> list = new ArrayList<>();
        // Jointure pour récupérer les libellés au lieu des simples IDs
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
                p.setTitreFormation(rs.getString("titre"));
                p.setNomUtilisateur(rs.getString("nom"));
                list.add(p);
            }
        }
        return list;
    }

    /**
     * Retourne une Map (Titre -> ID) des formations.
     * Le .trim() est crucial ici pour nettoyer les chaînes venant de la DB.
     */
    public Map<String, Integer> getFormationsMap() throws SQLException {
        Map<String, Integer> map = new HashMap<>();
        String req = "SELECT idFormation, titre FROM formations";
        try (Statement st = cnx.createStatement(); ResultSet rs = st.executeQuery(req)) {
            while (rs.next()) {
                String titre = rs.getString("titre");
                if (titre != null) {
                    map.put(titre.trim(), rs.getInt("idFormation"));
                }
            }
        }
        return map;
    }

    /**
     * Retourne une Map (Nom complet -> ID) des utilisateurs.
     */
    public Map<String, Integer> getUsersMap() throws SQLException {
        Map<String, Integer> map = new HashMap<>();
        String req = "SELECT id_user, nom, prenom FROM utilisateur";
        try (Statement st = cnx.createStatement(); ResultSet rs = st.executeQuery(req)) {
            while (rs.next()) {
                String nom = rs.getString("nom") != null ? rs.getString("nom").trim() : "";
                String prenom = rs.getString("prenom") != null ? rs.getString("prenom").trim() : "";
                String nomComplet = (nom + " " + prenom).trim();
                map.put(nomComplet, rs.getInt("id_user"));
            }
        }
        return map;
    }

    /**
     * Insère une nouvelle participation.
     */
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

    /**
     * Met à jour une participation existante.
     */
    public void updateOne(participations p) throws SQLException {
        String sql = "UPDATE participations SET idFormation=?, idUtilisateur=?, statut=?, presence=?, note=? WHERE idParticipation=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, p.getIdFormation());
            ps.setInt(2, p.getIdUtilisateur());
            ps.setString(3, p.getStatut().name());
            ps.setBoolean(4, p.isPresence());
            ps.setFloat(5, (p.getNote() != null) ? p.getNote() : 0.0f);
            ps.setInt(6, p.getIdParticipation());
            ps.executeUpdate();
        }
    }

    /**
     * Supprime une participation.
     */
    public void deleteOne(participations p) throws SQLException {
        String sql = "DELETE FROM participations WHERE idParticipation=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, p.getIdParticipation());
            ps.executeUpdate();
        }
    }
}