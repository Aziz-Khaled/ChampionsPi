package tn.esprit.Champions.services;

import tn.esprit.Champions.models.Role;
import tn.esprit.Champions.models.Status;
import tn.esprit.Champions.models.Utilisateur;
import tn.esprit.Champions.utils.DbConnection;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class UtilisateurService implements CRUD<Utilisateur>{
    @Override
    public void insertOne(Utilisateur utilisateur) throws SQLException {
        String query = """
                INSERT INTO utilisateur
                (nom, prenom, email, mot_de_passe, telephone,
                 role, statut, date_de_creation,
                 piece_identite, user_image)
                VALUES (?, ?, ?, ?, ?, ?, ?, NOW(), ?, ?)
                """;

        try (PreparedStatement ps = DbConnection.getInstance().getCnx().prepareStatement(query)) {
            ps.setString(1, utilisateur.getNom());
            ps.setString(2, utilisateur.getPrenom());
            ps.setString(3, utilisateur.getEmail());
            ps.setString(4, utilisateur.getMot_de_passe());
            ps.setString(5, utilisateur.getTelephone());
            ps.setString(6, utilisateur.getRole().name());
            ps.setString(7, "PENDING");
            ps.setString(8, utilisateur.getPiece_identite());
            ps.setString(9, utilisateur.getUser_image());

            ps.executeUpdate();
        }
    }

    @Override
    public void updateOne(Utilisateur utilisateur) throws SQLException {
        String query = """
                UPDATE utilisateur
                SET nom = ?, prenom = ?, email = ?, mot_de_passe = ?, 
                    telephone = ?, role = ?, statut = ?, 
                    piece_identite = ?, user_image = ?
                WHERE id_user = ?
                """;

        try (PreparedStatement ps = DbConnection.getInstance().getCnx().prepareStatement(query)) {
            ps.setString(1, utilisateur.getNom());
            ps.setString(2, utilisateur.getPrenom());
            ps.setString(3, utilisateur.getEmail());
            ps.setString(4, utilisateur.getMot_de_passe());
            ps.setString(5, utilisateur.getTelephone());
            ps.setString(6, utilisateur.getRole().name());
            ps.setString(7, utilisateur.getStatut().name());
            ps.setString(8, utilisateur.getPiece_identite());
            ps.setString(9, utilisateur.getUser_image());
            ps.setInt(10, utilisateur.getId_user());

            ps.executeUpdate();
        }
    }

    @Override
    public void deleteOne(Utilisateur utilisateur) throws SQLException {

        String query = "DELETE FROM utilisateur WHERE id_user = ?";

        try (PreparedStatement ps =
                     DbConnection.getInstance().getCnx().prepareStatement(query)) {

            ps.setInt(1, utilisateur.getId_user());
            ps.executeUpdate();
        }
    }

    @Override
    public List<Utilisateur> SelectAll() throws SQLException {
        String query = "SELECT * FROM utilisateur WHERE statut = 'PENDING'";
        List<Utilisateur> users = new ArrayList<>();

        try (PreparedStatement ps =
                     DbConnection.getInstance().getCnx().prepareStatement(query)) {

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                users.add(new Utilisateur(
                        rs.getInt("id_user"),
                        rs.getString("nom"),
                        rs.getString("prenom"),
                        rs.getString("email"),
                        rs.getString("mot_de_passe"),
                        rs.getString("telephone"),
                        rs.getString("piece_identite"),
                        rs.getString("user_image"),
                        Status.valueOf(rs.getString("statut")),
                        Role.valueOf(rs.getString("role"))
                ));
            }
        }


        return users;
    }

    public List<Utilisateur> getAllUsers() throws SQLException {

        String query = "SELECT * FROM utilisateur";

        List<Utilisateur> users = new ArrayList<>();

        try (PreparedStatement ps =
                     DbConnection.getInstance().getCnx().prepareStatement(query)) {

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                users.add(new Utilisateur(
                        rs.getInt("id_user"),
                        rs.getString("nom"),
                        rs.getString("prenom"),
                        rs.getString("email"),
                        rs.getString("mot_de_passe"),
                        rs.getString("telephone"),
                        rs.getString("piece_identite"),
                        rs.getString("user_image"),
                        Status.valueOf(rs.getString("statut")),
                        Role.valueOf(rs.getString("role"))
                ));
            }
        }

        return users;
    }

    /**
     * Logic for syncing Auth0/Google users with the local database.
     * Returns the user object (either existing or newly created).
     */
    public Utilisateur handleLocalUserSync(String email, String name) throws SQLException {
        Utilisateur user = getUserByEmail(email);

        if (user == null) {
            // 1. Split name into First and Last name
            String[] nameParts = name.split(" ", 2);
            String firstName = nameParts[0];
            String lastName = (nameParts.length > 1) ? nameParts[1] : "User";

            // 2. Updated Query to include mandatory fields
            String query = """
            INSERT INTO utilisateur 
            (nom, prenom, email, mot_de_passe, telephone, role, statut, 
             piece_identite, user_image, date_de_creation) 
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, NOW())
            """;

            try (PreparedStatement ps = DbConnection.getInstance().getCnx().prepareStatement(query)) {
                ps.setString(1, lastName);
                ps.setString(2, firstName);
                ps.setString(3, email);
                // Random password since they login via Google
                ps.setString(4, "OAUTH_" + UUID.randomUUID().toString().substring(0, 8));

                // 3. Provide default empty strings for mandatory fields to satisfy MySQL
                ps.setString(5, "00000000");      // Default telephone
                ps.setString(6, Role.CLIENT.name());
                ps.setString(7, Status.PENDING.name()); // Or PENDING if you want to verify them
                ps.setString(8, "NOT_PROVIDED");  // Default piece_identite
                ps.setString(9, "default_user.png"); // Default image

                ps.executeUpdate();
            }
            // Return the newly created user
            return getUserByEmail(email);
        }
        return user;
    }

    public Utilisateur getUserByEmail(String email) throws SQLException {
        String query = "SELECT * FROM utilisateur WHERE email = ?";
        try (PreparedStatement ps = tn.esprit.Champions.utils.DbConnection.getInstance().getCnx().prepareStatement(query)) {
            ps.setString(1, email);

            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return new Utilisateur(
                        rs.getInt("id_user"),
                        rs.getString("nom"),
                        rs.getString("prenom"),
                        rs.getString("email"),
                        rs.getString("mot_de_passe"),
                        rs.getString("telephone"),
                        rs.getString("piece_identite"),
                        rs.getString("user_image"),
                        tn.esprit.Champions.models.Status.valueOf(rs.getString("statut")),
                        tn.esprit.Champions.models.Role.valueOf(rs.getString("role"))
                );
            }
            return null;
        }
    }


    public void completeKYC(int userId, String phone, String idFileName, String imgFileName, String role) throws SQLException {
        // We update 5 fields: telephone, piece_identite, user_image, role, and statut
        String query = "UPDATE utilisateur SET telephone = ?, piece_identite = ?, user_image = ?, role = ?, statut = ? WHERE id_user = ?";

        try (PreparedStatement ps = DbConnection.getInstance().getCnx().prepareStatement(query)) {
            ps.setString(1, phone);
            ps.setString(2, idFileName);
            ps.setString(3, imgFileName);
            ps.setString(4, role);
            ps.setString(5, "PENDING"); // Account waits for admin approval after KYC
            ps.setInt(6, userId);

            ps.executeUpdate();
        }
    }

}
