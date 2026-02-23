package tn.esprit.Champions.gui;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import tn.esprit.Champions.models.Utilisateur;
import tn.esprit.Champions.services.UtilisateurService;
import tn.esprit.Champions.utils.UserSession;
import java.io.File;

public class KYCController {
    @FXML private TextField TF_Telephone;
    @FXML private Label label_id_status, label_photo_status;

    private File idFile;
    private File photoFile;
    private UtilisateurService userService = new UtilisateurService();

    @FXML
    private void uploadID() {
        idFile = selectFile("Select National ID");
        if (idFile != null) label_id_status.setText(idFile.getName());
    }

    @FXML
    private void uploadPhoto() {
        photoFile = selectFile("Select Selfie Photo");
        if (photoFile != null) label_photo_status.setText(photoFile.getName());
    }

    private File selectFile(String title) {
        FileChooser fc = new FileChooser();
        fc.setTitle(title);
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images/PDF", "*.png", "*.jpg", "*.pdf"));
        return fc.showOpenDialog(TF_Telephone.getScene().getWindow());
    }

    @FXML
    private void submitKYC() {
        if (TF_Telephone.getText().isEmpty() || idFile == null || photoFile == null) {
            showAlert("Error", "Please fill all fields and upload required documents.");
            return;
        }

        try {
            Utilisateur currentUser = UserSession.getLoggedInUser();
            // Store file paths in DB (You should ideally move files to a 'uploads' folder first)
            userService.completeKYC(
                    currentUser.getId_user(),
                    TF_Telephone.getText(),
                    idFile.getAbsolutePath(),
                    photoFile.getAbsolutePath()
            );

            showAlert("Success", "KYC Submitted! An admin will verify your account shortly.");
            // Return to login or a "Pending" screen
            logout();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setContentText(content);
        alert.show();
    }

    @FXML
    private void logout() {
        UserSession.setLoggedInUser(null);
        // Add code to switch scene back to /Login.fxml
    }
}