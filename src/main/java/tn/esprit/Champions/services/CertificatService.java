package tn.esprit.Champions.services;

import tn.esprit.Champions.models.certificats;
import tn.esprit.Champions.models.MentionCertificat;
import tn.esprit.Champions.utils.DbConnection;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CertificatService {
    private Connection cnx;

    public CertificatService() {
        cnx = DbConnection.getInstance().getCnx();
    }

    public List<Map<String, Object>> getEligibleParticipations() throws SQLException {
        List<Map<String, Object>> list = new ArrayList<>();
        // Sélectionne uniquement les participations avec note >= 10 n'ayant pas encore de certificat
        String sql = "SELECT p.idParticipation, u.nom, u.prenom, f.titre, p.note " +
                "FROM participations p " +
                "JOIN utilisateur u ON p.idUtilisateur = u.id_user " +
                "JOIN formations f ON p.idFormation = f.idFormation " +
                "LEFT JOIN certificats c ON p.idParticipation = c.idParticipation " +
                "WHERE p.note >= 10 AND c.idParticipation IS NULL";

        try (Statement st = cnx.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                Map<String, Object> item = new HashMap<>();
                item.put("id", rs.getLong("idParticipation"));
                String nomComplet = rs.getString("nom") + " " + rs.getString("prenom");
                item.put("nomComplet", nomComplet);
                item.put("formation", rs.getString("titre"));
                item.put("note", rs.getFloat("note"));
                item.put("displayText", nomComplet + " - " + rs.getString("titre") + " (" + rs.getFloat("note") + "/20)");
                list.add(item);
            }
        }
        return list;
    }

    public List<certificats> SelectAll() throws SQLException {
        List<certificats> list = new ArrayList<>();
        // Utilisation de LEFT JOIN pour éviter que des lignes disparaissent si une liaison est fragile
        String sql = "SELECT c.*, u.nom, u.prenom, f.titre " +
                "FROM certificats c " +
                "LEFT JOIN participations p ON c.idParticipation = p.idParticipation " +
                "LEFT JOIN utilisateur u ON p.idUtilisateur = u.id_user " +
                "LEFT JOIN formations f ON p.idFormation = f.idFormation " +
                "ORDER BY c.dateEmission DESC";

        try (Statement st = cnx.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                certificats c = new certificats();
                c.setIdCertificat(rs.getInt("idCertificat"));
                c.setIdParticipation(rs.getLong("idParticipation"));
                c.setDateEmission(rs.getDate("dateEmission").toLocalDate());
                c.setCodeVerification(rs.getString("codeVerification"));
                c.setMention(MentionCertificat.valueOf(rs.getString("mention")));
                c.setUrlFichier(rs.getString("urlFichier"));

                // On vérifie si les données jointes existent pour éviter les NullPointerException
                String nom = rs.getString("nom") != null ? rs.getString("nom") : "Inconnu";
                String prenom = rs.getString("prenom") != null ? rs.getString("prenom") : "";
                c.setNomEtudiant(nom + " " + prenom);
                c.setNomFormation(rs.getString("titre") != null ? rs.getString("titre") : "N/A");

                list.add(c);
            }
        }
        return list;
    }

    public void insertOne(certificats c) throws SQLException {
        String sql = "INSERT INTO certificats (idParticipation, dateEmission, codeVerification, mention, urlFichier) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, c.getIdParticipation());
            ps.setDate(2, Date.valueOf(c.getDateEmission()));
            ps.setString(3, c.getCodeVerification());
            ps.setString(4, c.getMention().name());
            ps.setString(5, c.getUrlFichier());
            ps.executeUpdate();

            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) c.setIdCertificat(rs.getInt(1));
        }
    }

    public void deleteOne(certificats c) throws SQLException {
        String sql = "DELETE FROM certificats WHERE idCertificat=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, c.getIdCertificat());
            ps.executeUpdate();
        }
    }
}