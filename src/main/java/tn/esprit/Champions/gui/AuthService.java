package tn.esprit.Champions.gui;
import tn.esprit.Champions.utils.JwtUtils;
import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.TranslateTransition;
import javafx.fxml.FXML;
import org.mindrot.jbcrypt.BCrypt;
import javafx.scene.control.*;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;
import tn.esprit.Champions.models.Role;
import tn.esprit.Champions.models.Status;
import tn.esprit.Champions.models.Utilisateur;
import tn.esprit.Champions.services.UtilisateurService;
import tn.esprit.Champions.utils.UserSession;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.SQLException;



public class AuthService {


    @FXML private VBox step1Container, step2Container;
    @FXML private Region prog1, prog2;
    @FXML private Label stepDescription;

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

    private boolean validateStep1() {

        String nom = TF_Nom.getText().trim();
        String prenom = TF_Prenom.getText().trim();
        String email = TF_Email.getText().trim();
        String telephone = TF_Telephone.getText().trim();
        String password = TF_Password.getText();

        if (nom.isEmpty() || prenom.isEmpty() || email.isEmpty()
                || telephone.isEmpty() || password.isEmpty()) {

            showAlert(Alert.AlertType.WARNING,
                    "Champs obligatoires",
                    "Veuillez remplir tous les champs.");
            return false;
        }

        if (!email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")) {
            showAlert(Alert.AlertType.ERROR,
                    "Email invalide",
                    "Veuillez saisir une adresse email valide.");
            return false;
        }

        if (!telephone.matches("\\d{8}")) {
            showAlert(Alert.AlertType.ERROR,
                    "Numéro invalide",
                    "Le numéro doit contenir 8 chiffres.");
            return false;
        }

        if (password.length() < 6) {
            showAlert(Alert.AlertType.ERROR,
                    "Mot de passe faible",
                    "Minimum 6 caractères.");
            return false;
        }

        return true;
    }
    private void signUp() {

        String nom = TF_Nom.getText().trim();
        String prenom = TF_Prenom.getText().trim();
        String email = TF_Email.getText().trim();
        String telephone = TF_Telephone.getText().trim();
        String password = TF_Password.getText();
        Role selectedRole = combobox_role.getValue();


        if (nom.isEmpty() || prenom.isEmpty() || email.isEmpty() ||
                telephone.isEmpty() || password.isEmpty() || selectedRole == null) {

            showAlert(Alert.AlertType.WARNING, "Champs obligatoires",
                    "Veuillez remplir tous les champs et sélectionner un rôle.");
            return;
        }

        if (!email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")) {
            showAlert(Alert.AlertType.ERROR, "Email invalide", "Veuillez saisir une adresse email valide.");
            return;
        }

        if (!telephone.matches("\\d{8}")) {
            showAlert(Alert.AlertType.ERROR, "Numéro invalide", "Le numéro de téléphone doit contenir 8 chiffres.");
            return;
        }

        if (password.length() < 6) {
            showAlert(Alert.AlertType.ERROR, "Mot de passe faible", "Le mot de passe doit contenir au moins 6 caractères.");
            return;
        }

        if (identityFile == null || personalImageFile == null) {
            showAlert(Alert.AlertType.ERROR, "Fichiers manquants", "Veuillez sélectionner une pièce d'identité et une photo personnelle.");
            return;
        }


        String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());


        Utilisateur newUser = new Utilisateur(
                0,
                nom,
                prenom,
                email,
                hashedPassword,
                telephone,
                identityFile.getName(),
                personalImageFile.getName(),
                Status.PENDING,
                selectedRole
        );


        try {
            UtilisateurService userService = new UtilisateurService();


            userService.insertOne(newUser);


            Utilisateur dbUser = userService.getUserByEmail(newUser.getEmail());

            if (dbUser != null) {

                newUser.setId_user(dbUser.getId_user());


                String token = JwtUtils.generateToken(newUser);
                System.out.println("Generated JWT for new user: " + token);

                UserSession.setLoggedInUser(newUser, token);
            }

            showAlert(Alert.AlertType.INFORMATION, "Succès",
                    "Demande d'inscription envoyée avec succès.\nEn attente de validation admin.");

            clearForm();

        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de créer le compte.\n" + e.getMessage());
        }
    }


    private void clearForm() {
        TF_Nom.clear();
        TF_Prenom.clear();
        TF_Email.clear();
        TF_Telephone.clear();
        TF_Password.clear();
        combobox_role.getSelectionModel().clearSelection();
        identityFile = null;
        personalImageFile = null;
    }
    @FXML
    private void openLoginPage() {

        SceneHelper.transitionTo("/LoginPage.fxml", Login_Button.getScene().getRoot(), "Login - Champions");
    }

    @FXML
    private void nextStep() {

        if (!validateStep1()) return;
        animateStepChange(step1Container, step2Container, true);
    }

    @FXML
    private void prevStep() {
        animateStepChange(step2Container, step1Container, false);
    }

    private void animateStepChange(VBox out, VBox in, boolean isNext) {

        FadeTransition fadeOut = new FadeTransition(Duration.millis(250), out);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);

        fadeOut.setOnFinished(e -> {
            out.setVisible(false);
            out.setManaged(false);

            in.setVisible(true);
            in.setManaged(true);
            in.setOpacity(0);


            if (isNext) {
                prog2.setStyle("-fx-background-color: #3b82f6;");
                stepDescription.setText("Step 2: Account Verification");
            } else {
                prog2.setStyle("-fx-background-color: #e2e8f0;");
                stepDescription.setText("Step 1: Personal Details");
            }


            FadeTransition fadeIn = new FadeTransition(Duration.millis(250), in);
            fadeIn.setFromValue(0.0);
            fadeIn.setToValue(1.0);

            TranslateTransition slide = new TranslateTransition(Duration.millis(250), in);
            slide.setFromX(isNext ? 20 : -20);
            slide.setToX(0);

            new ParallelTransition(fadeIn, slide).play();
        });

        fadeOut.play();
    }
}
