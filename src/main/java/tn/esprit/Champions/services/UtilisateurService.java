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

}
