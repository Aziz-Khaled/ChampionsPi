package tn.esprit.Champions.services;

import tn.esprit.Champions.models.Reclamation;
import tn.esprit.Champions.models.StatutReclamation;
import tn.esprit.Champions.utils.DbConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ReclamationService {

    private Connection cnx;

    public ReclamationService() {
        cnx = DbConnection.getInstance().getCnx();
    }

    /**
     * Pour l'Étudiant : Ajouter une nouvelle réclamation liée à une formation
     */
    public void insertOne(Reclamation r) throws SQLException {
        String sql = "INSERT INTO reclamations (idUtilisateur, idFormation, sujet, description, statut) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, r.getIdUtilisateur());
            ps.setLong(2, r.getIdFormation());
            ps.setString(3, r.getSujet());
            ps.setString(4, r.getDescription());
            ps.setString(5, r.getStatut().name());
            ps.executeUpdate();
        }
    }

    /**
     * Pour le Back Office : Récupérer toutes les réclamations avec Noms et Titres
     */
    public List<Reclamation> selectAll() throws SQLException {
        List<Reclamation> list = new ArrayList<>();

        // REQUÊTE FINALE CORRIGÉE : u.id_user correspond à ta clé primaire dans phpMyAdmin
        String sql = "SELECT r.*, u.nom AS nom_user, f.titre AS titre_form " +
                "FROM reclamations r " +
                "JOIN utilisateur u ON r.idUtilisateur = u.id_user " +
                "JOIN formations f ON r.idFormation = f.idFormation";

        try (Statement st = cnx.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                Reclamation r = new Reclamation();
                r.setIdRec(rs.getInt("idRec"));
                r.setIdUtilisateur(rs.getInt("idUtilisateur"));
                r.setIdFormation(rs.getLong("idFormation"));
                r.setSujet(rs.getString("sujet"));
                r.setDescription(rs.getString("description"));

                // Gestion de la date si nécessaire
                Timestamp ts = rs.getTimestamp("dateEnvoi");
                if (ts != null) {
                    r.setDateEnvoi(ts.toLocalDateTime());
                }

                // Récupération de l'Enum
                r.setStatut(StatutReclamation.valueOf(rs.getString("statut")));

                // REMPLISSAGE DES NOMS POUR L'AFFICHAGE
                r.setNomUtilisateur(rs.getString("nom_user")); // "Sarra" par exemple
                r.setTitreFormation(rs.getString("titre_form"));

                list.add(r);
            }
        }
        return list;
    }

    /**
     * Pour le Back Office : Mettre à jour le statut (Traiter/Rejeter)
     */
    public void updateStatut(int idRec, StatutReclamation nouveauStatut) throws SQLException {
        String sql = "UPDATE reclamations SET statut = ? WHERE idRec = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, nouveauStatut.name());
            ps.setInt(2, idRec);
            ps.executeUpdate();
        }
    }

    /**
     * Pour l'Étudiant : Supprimer une réclamation
     */
    public void deleteOne(int idRec) throws SQLException {
        String sql = "DELETE FROM reclamations WHERE idRec = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, idRec);
            ps.executeUpdate();
        }
    }
}