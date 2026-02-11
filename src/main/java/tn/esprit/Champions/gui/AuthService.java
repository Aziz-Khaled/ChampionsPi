package tn.esprit.Champions.gui;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import tn.esprit.Champions.models.Role;
import tn.esprit.Champions.models.Status;
import tn.esprit.Champions.models.Utilisateur;
import tn.esprit.Champions.services.UtilisateurService;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.SQLException;

public class AuthService {




    @FXML
    private Button Login_Button;


    @FXML
    private TextField TF_Email;

    @FXML
    private Button signUpButton;

    @FXML
    private Button Identity_Button;

    @FXML
    private TextField TF_Nom;

    @FXML
    private PasswordField TF_Password;

    @FXML
    private TextField TF_Prenom;

    @FXML
    private TextField TF_Telephone;

    @FXML
    private Button personalImage_button;

    @FXML
    private ComboBox<Role> combobox_role;

    private File identityFile;
    private File personalImageFile;

    @FXML
    private void initialize() {
        Identity_Button.setOnAction(e -> chooseIdentityFile());
        personalImage_button.setOnAction(e -> choosePersonalImage());
        signUpButton.setOnAction(e -> signUp());
        Login_Button.setOnAction(e -> openLoginPage());
        combobox_role.getItems().setAll(
                java.util.Arrays.stream(Role.values())
                        .filter(role -> role != Role.ADMIN)  // exclude ADMIN
                        .toList()
        );

    }

    @FXML
    private void chooseIdentityFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Identity File");
        File selectedFile = fileChooser.showOpenDialog(new Stage());
        if (selectedFile != null) {
            identityFile = saveFileToAssets(selectedFile);
        }
    }

    @FXML
    private void choosePersonalImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Personal Image");
        File selectedFile = fileChooser.showOpenDialog(new Stage());
        if (selectedFile != null) {
            personalImageFile = saveFileToAssets(selectedFile);
        }
    }

    private File saveFileToAssets(File file) {
        try {
            Path targetDir = Path.of("src/main/resources/assets");
            if (!Files.exists(targetDir)) Files.createDirectories(targetDir);
            Path target = targetDir.resolve(file.getName());
            Files.copy(file.toPath(), target, StandardCopyOption.REPLACE_EXISTING);
            System.out.println("File saved to: " + target.toAbsolutePath());
            return target.toFile();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public void showAlert (Alert.AlertType type, String title, String message){
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    private void signUp() {

        String nom = TF_Nom.getText();
        String prenom = TF_Prenom.getText();
        String telephone = TF_Telephone.getText();
        String password = TF_Password.getText();
        String email = TF_Email.getText();
        Role selectedRole = combobox_role.getValue();

        if (TF_Nom.getText().isEmpty() ||
                TF_Prenom.getText().isEmpty() ||
                TF_Telephone.getText().isEmpty() ||
                TF_Password.getText().isEmpty() ||
                TF_Email.getText().isEmpty()) {

            showAlert(
                    Alert.AlertType.WARNING,
                    "Champs obligatoires",
                    "Veuillez remplir tous les champs."
            );
            return;
        }


        if (!email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")) {
            showAlert(
                    Alert.AlertType.ERROR,
                    "Email invalide",
                    "Veuillez saisir une adresse email valide."
            );
            return;
        }


        if (!TF_Telephone.getText().matches("\\d{8}")) {
            showAlert(
                    Alert.AlertType.ERROR,
                    "Numéro invalide",
                    "Le numéro de téléphone doit contenir 8 chiffres."
            );
            return;
        }

        // 3. Password validation
        if (TF_Password.getText().length() < 6) {
            showAlert(
                    Alert.AlertType.ERROR,
                    "Mot de passe faible",
                    "Le mot de passe doit contenir au moins 6 caractères."
            );
            return;
        }

        // 4. File selection validation
        if (identityFile == null || personalImageFile == null) {
            showAlert(
                    Alert.AlertType.ERROR,
                    "Fichiers manquants",
                    "Veuillez sélectionner une pièce d'identité et une photo personnelle."
            );
            return;
        }

        showAlert(
                Alert.AlertType.INFORMATION,
                "Succès",
                "Demande d'inscription envoyée avec succès.\nEn attente de validation admin."
        );

        if (selectedRole == null) {
            showAlert(
                    Alert.AlertType.WARNING,
                    "Rôle requis",
                    "Veuillez sélectionner un rôle."
            );
            return;
        }

        Utilisateur newUser = new Utilisateur(
                0, // id_user will be auto-generated by DB
                nom,
                prenom,
                email,
                password,
                telephone,
                identityFile.getName(),
                personalImageFile.getName(),
                Status.PENDING, // Default status
                selectedRole
        );


        try {
            UtilisateurService userService = new UtilisateurService();
            userService.insertOne(newUser);

            showAlert(Alert.AlertType.INFORMATION, "Succès", "Demande d'inscription envoyée avec succès.\nEn attente de validation admin.");


            TF_Nom.clear();
            TF_Prenom.clear();
            TF_Email.clear();
            TF_Telephone.clear();
            TF_Password.clear();
            combobox_role.getSelectionModel().clearSelection();
            identityFile = null;
            personalImageFile = null;

        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de créer le compte.\n" + e.getMessage());
        }
    }
    private void openLoginPage() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/LoginPage.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) Login_Button.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Login Page");
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible d'ouvrir la page de login.");
        }
    }
}
