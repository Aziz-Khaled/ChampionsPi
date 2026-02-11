package tn.esprit.Champions.gui;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import tn.esprit.Champions.models.Utilisateur;
import tn.esprit.Champions.services.UtilisateurService;

import java.awt.*;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class LoginService {


    @FXML
    private Button btn_Login;

    @FXML
    private TextField txt_Email;

    @FXML
    private TextField txt_Password;

    private UtilisateurService userService = new UtilisateurService();

    @FXML
    private void initialize() {
        btn_Login.setOnAction(e -> login());
    }

    private void login() {
        String email = txt_Email.getText();
        String password = txt_Password.getText();

        if (email.isEmpty() || password.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Champs manquants", "Veuillez remplir tous les champs.");
            return;
        }

        try {
            // Check user in DB
            Utilisateur user = getUserByEmailAndPassword(email, password);

            if (user != null) {
                // Check if user is pending
                if (user.getStatut() == tn.esprit.Champions.models.Status.PENDING) {
                    showAlert(Alert.AlertType.WARNING, "Compte en attente",
                            "Votre compte est en attente de validation par l'administrateur.\nVeuillez patienter.");
                    return; // stop login here
                }

                // If status is approved, continue
                showAlert(Alert.AlertType.INFORMATION, "Succès", "Bienvenue, " + user.getNom() + " !");

                // Role-based redirection
                if (user.getRole() == tn.esprit.Champions.models.Role.ADMIN) {
                    openPage("/AdminPanel.fxml", "Admin Panel");
                } else {
                    openPage("/ClientPanel.fxml", "Client Panel");
                }

            } else {
                showAlert(Alert.AlertType.ERROR, "Erreur", "Email ou mot de passe incorrect.");
            }

        } catch (SQLException ex) {
            ex.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur DB", ex.getMessage());
        }
    }


    private Utilisateur getUserByEmailAndPassword(String email, String password) throws SQLException {
        String query = "SELECT * FROM utilisateur WHERE email = ? AND mot_de_passe = ?";
        try (PreparedStatement ps = tn.esprit.Champions.utils.DbConnection.getInstance().getCnx().prepareStatement(query)) {
            ps.setString(1, email);
            ps.setString(2, password);

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
            } else {
                return null;
            }
        }
    }

    private void showAlert(Alert.AlertType type, String title, String message){
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // Utility method to open a new FXML page
    private void openPage(String fxmlPath, String title) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource(fxmlPath));
            javafx.scene.Parent root = loader.load();
            javafx.stage.Stage stage = (javafx.stage.Stage) btn_Login.getScene().getWindow();
            stage.setScene(new javafx.scene.Scene(root));
            stage.setTitle(title);
            stage.show();
        } catch (java.io.IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible d'ouvrir la page : " + title);
        }
    }

}
