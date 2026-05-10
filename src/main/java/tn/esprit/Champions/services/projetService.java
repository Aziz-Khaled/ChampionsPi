package tn.esprit.Champions.services;

import tn.esprit.Champions.models.Utilisateur;
import tn.esprit.Champions.models.projet;
import tn.esprit.Champions.models.projetStatus;
import tn.esprit.Champions.utils.DbConnection;
import tn.esprit.Champions.utils.UserSession;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class projetService implements CRUD<projet> {
    private Connection cnx;

    public projetService() {
        cnx = DbConnection.getInstance().getCnx();
    }

    private String detecterSecteur(String titre, String description) {
        String texte = ((titre != null ? titre : "") + " " + (description != null ? description : "")).toLowerCase();

        if (texte.contains("agri") || texte.contains("ferme") || texte.contains("culture"))
            return "Agriculture";
        if (texte.contains("tech") || texte.contains("ai") || texte.contains("logiciel") || texte.contains("hub"))
            return "Technologie";
        if (texte.contains("solaire") || texte.contains("energie") || texte.contains("electrique"))
            return "Énergie";
        if (texte.contains("santé") || texte.contains("medical") || texte.contains("clinique")
                || texte.contains("diagnostic"))
            return "Santé";
        if (texte.contains("coworking") || texte.contains("immobilier") || texte.contains("bureau"))
            return "Immobilier";

        return "Autre";
    }

    @Override
    public void insertOne(projet p) throws SQLException {
        // 1. On vérifie si un secteur est déjà présent (détecté par l'IA dans le
        // contrôleur)
        // S'il est nul ou vide, seulement là on utilise la détection manuelle locale.
        String secteurFinal = (p.getSecteur() != null && !p.getSecteur().isEmpty())
                ? p.getSecteur()
                : detecterSecteur(p.getTitle(), p.getDescription());

        int userId = 1;
        if (UserSession.getLoggedInUser() != null) {
            userId = UserSession.getLoggedInUser().getId_user();
        } else if (p.getOwner_id() != null) {
            userId = p.getOwner_id().getId_user();
        }
        
        String req = "INSERT INTO `projet` (`owner_id`, `title`, `description`, `status`, `target_amount`, `start_date`, `end_date`, `image_url`, `secteur`) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement pst = cnx.prepareStatement(req)) {
            pst.setInt(1, userId);
            pst.setString(2, p.getTitle());
            pst.setString(3, p.getDescription());
            pst.setString(4, (p.getStatus() != null) ? p.getStatus().name() : "ACTIVE");
            pst.setDouble(5, p.getTarget_amount());
            pst.setDate(6, p.getStart_date() != null ? java.sql.Date.valueOf(p.getStart_date()) : null);
            pst.setDate(7, p.getEnd_date() != null ? java.sql.Date.valueOf(p.getEnd_date()) : null);
            pst.setString(8, p.getImage_url());
            pst.setString(9, secteurFinal); // On utilise la valeur consolidée

            pst.executeUpdate();
        }
    }

    @Override
    public void updateOne(projet p) throws SQLException {
        // Recalcul du secteur en cas de changement de titre/description
        String secteurDetecte = detecterSecteur(p.getTitle(), p.getDescription());

        String req = "UPDATE `projet` SET `owner_id`=?, `title`=?, `description`=?, `status`=?, `target_amount`=?, `start_date`=?, `end_date`=?, `image_url`=?, `secteur`=? WHERE `id_projet`=?";

        try (PreparedStatement pst = cnx.prepareStatement(req)) {
            pst.setInt(1, p.getOwner_id().getId_user());
            pst.setString(2, p.getTitle());
            pst.setString(3, p.getDescription());
            pst.setString(4, (p.getStatus() != null) ? p.getStatus().name() : null);
            pst.setDouble(5, p.getTarget_amount());
            pst.setDate(6, p.getStart_date() != null ? java.sql.Date.valueOf(p.getStart_date()) : null);
            pst.setDate(7, p.getEnd_date() != null ? java.sql.Date.valueOf(p.getEnd_date()) : null);
            pst.setString(8, p.getImage_url());
            pst.setString(9, secteurDetecte);
            pst.setInt(10, p.getId_projet());

            pst.executeUpdate();
        }
    }

    @Override
    public void deleteOne(projet p) throws SQLException {
        String req = "DELETE FROM `projet` WHERE `id_projet` = ?";
        try (PreparedStatement pst = cnx.prepareStatement(req)) {
            pst.setInt(1, p.getId_projet());
            pst.executeUpdate();
        }
    }

    @Override
    public List<projet> SelectAll() throws SQLException {
        List<projet> listeProjets = new ArrayList<>();
        String req = "SELECT * FROM `projet`";
        try (Statement st = cnx.createStatement(); ResultSet rs = st.executeQuery(req)) {
            while (rs.next()) {
                listeProjets.add(mapperResultSetToProjet(rs));
            }
        }
        return listeProjets;
    }

    public projet findById(int id) throws SQLException {
        String req = "SELECT * FROM `projet` WHERE `id_projet` = ?";
        try (PreparedStatement pst = cnx.prepareStatement(req)) {
            pst.setInt(1, id);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    return mapperResultSetToProjet(rs);
                }
            }
        }
        return null;
    }

    // Méthode interne pour éviter la répétition de code et ne rien oublier
    private projet mapperResultSetToProjet(ResultSet rs) throws SQLException {
        projet p = new projet();
        p.setId_projet(rs.getInt("id_projet"));

        Utilisateur owner = new Utilisateur();
        owner.setId_user(rs.getInt("owner_id"));
        p.setOwner_id(owner);

        p.setTitle(rs.getString("title"));
        p.setDescription(rs.getString("description"));

        String statusFromDB = rs.getString("status");
        if (statusFromDB != null) {
            p.setStatus(projetStatus.valueOf(statusFromDB));
        }

        p.setTarget_amount(rs.getDouble("target_amount"));
        p.setStart_date(rs.getDate("start_date") != null ? rs.getDate("start_date").toLocalDate() : null);
        p.setEnd_date(rs.getDate("end_date") != null ? rs.getDate("end_date").toLocalDate() : null);
        p.setImage_url(rs.getString("image_url"));

        // --- LA LIGNE CRUCIALE POUR TON FILTRAGE ---
        p.setSecteur(rs.getString("secteur"));

        return p;
    }

    public int getTotalProjets() {
        String query = "SELECT COUNT(*) FROM `projet`";
        try (Statement st = cnx.createStatement(); ResultSet rs = st.executeQuery(query)) {
            if (rs.next())
                return rs.getInt(1);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }
}