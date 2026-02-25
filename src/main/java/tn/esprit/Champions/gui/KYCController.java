package tn.esprit.Champions.gui;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import tn.esprit.Champions.models.Role;
import tn.esprit.Champions.models.Utilisateur;
import tn.esprit.Champions.services.UtilisateurService;
import tn.esprit.Champions.utils.UserSession;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.SQLException;

public class KYCController {

    @FXML private TextField TF_Telephone;
    @FXML private ComboBox<Role> combobox_role;
    @FXML private Label label_id_status, label_photo_status;
    @FXML private Button btn_submit;

    private File identityFile;
    private File personalImageFile;
    private UtilisateurService userService = new UtilisateurService();

    @FXML
    private void initialize() {
        // Standard Role population excluding Admin
        combobox_role.getItems().setAll(
                java.util.Arrays.stream(Role.values())
                        .filter(role -> role != Role.ADMIN)
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
            if (identityFile != null) label_id_status.setText(identityFile.getName());
        }
    }

    @FXML
    private void choosePersonalImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Personal Image");
        File selectedFile = fileChooser.showOpenDialog(new Stage());
        if (selectedFile != null) {
            personalImageFile = saveFileToAssets(selectedFile);
            if (personalImageFile != null) label_photo_status.setText(personalImageFile.getName());
        }
    }

    private File saveFileToAssets(File file) {
        try {
            Path targetDir = Path.of("src/main/resources/assets");
            if (!Files.exists(targetDir)) Files.createDirectories(targetDir);
            Path target = targetDir.resolve(file.getName());
            Files.copy(file.toPath(), target, StandardCopyOption.REPLACE_EXISTING);
            return target.toFile();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    @FXML
    private void submitKYC() {
        String telephone = TF_Telephone.getText().trim();
        Role selectedRole = combobox_role.getValue();

        // Validation logic
        if (telephone.isEmpty() || !telephone.matches("\\d{8}")) {
            showAlert(Alert.AlertType.ERROR, "Invalid Input", "Please enter a valid 8-digit phone number.");
            return;
        }
        if (selectedRole == null || identityFile == null || personalImageFile == null) {
            showAlert(Alert.AlertType.ERROR, "Missing Data", "All fields and documents are required.");
            return;
        }

        try {
            Utilisateur currentUser = UserSession.getLoggedInUser();

            // Sync with DB
            userService.completeKYC(
                    currentUser.getId_user(),
                    telephone,
                    identityFile.getName(),
                    personalImageFile.getName(),
                    selectedRole.name()
            );

            showAlert(Alert.AlertType.INFORMATION, "Success", "KYC Details saved. Please wait for Admin approval.");
            logout(); // Send back to login to wait for PENDING -> ACTIVE

        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Database Error", "Failed to update profile: " + e.getMessage());
        }
    }

    public void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @FXML
    private void logout() {
        UserSession.clearSession();
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/LoginPage.fxml"));
            javafx.scene.Parent root = loader.load();
            javafx.stage.Stage stage = (javafx.stage.Stage) btn_submit.getScene().getWindow();
            stage.setScene(new javafx.scene.Scene(root));
            stage.show();
        } catch (IOException ex) { ex.printStackTrace(); }
    }

}