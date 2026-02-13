package tn.esprit.Champions.gui;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import tn.esprit.Champions.models.certificats;
import tn.esprit.Champions.services.CertificatService;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.util.List;

public class StudentCertifController {

    @FXML private VBox certifsListContainer;
    private final CertificatService cs = new CertificatService();

    @FXML
    public void initialize() {
        loadStudentCertifs();
    }

    private void loadStudentCertifs() {
        try {
            List<certificats> list = cs.SelectAll();
            certifsListContainer.getChildren().clear();
            for (certificats c : list) {
                certifsListContainer.getChildren().add(createCertifRow(c));
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private HBox createCertifRow(certificats c) {
        HBox row = new HBox(20);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(20));
        row.setStyle("-fx-background-color: white; -fx-background-radius: 12; " +
                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.05), 5, 0, 0, 2);");

        VBox info = new VBox(5);
        Label title = new Label("Certificat #" + c.getCodeVerification());
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 15; -fx-text-fill: #2D3436;");
        Label date = new Label("Délivré le : " + c.getDateEmission() + " | Mention: " + c.getMention());
        date.setStyle("-fx-text-fill: #636E72;");
        info.getChildren().addAll(title, date);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button btnOpen = new Button("👁 Voir PDF");
        btnOpen.setStyle("-fx-background-color: #F1F2F6; -fx-text-fill: #2D3436; -fx-font-weight: bold; -fx-background-radius: 8; -fx-cursor: hand;");

        btnOpen.setOnAction(e -> {
            try {
                File file = new File(c.getUrlFichier());
                if (file.exists()) Desktop.getDesktop().open(file);
                else new Alert(Alert.AlertType.ERROR, "Fichier PDF introuvable sur le disque.").show();
            } catch (Exception ex) { ex.printStackTrace(); }
        });

        row.getChildren().addAll(info, spacer, btnOpen);
        return row;
    }

    @FXML
    private void handleBack(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/tn/esprit/Champions/gui/student_dashboard.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (IOException e) { e.printStackTrace(); }
    }
    @FXML
    private Button btnNavDashboard; // Assure-toi que l'id correspond au FXML

    @FXML
    private void handleGoToDashboard(ActionEvent event) {
        try {
            // Redirection vers le dashboard principal
            Parent root = FXMLLoader.load(getClass().getResource("/student_dashboard.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            System.err.println("Erreur de navigation vers le Dashboard : " + e.getMessage());
        }
    }

// --- EFFETS VISUELS SANS CSS ---

    @FXML
    private void handleHoverIn() {
        // Devient un peu plus sombre et souligne au survol
        btnNavDashboard.setStyle("-fx-background-color: #f1f1f1; -fx-text-fill: #FE4A49; -fx-font-weight: bold; -fx-background-radius: 5;");
    }

    @FXML
    private void handleHoverOut() {
        // Revient à l'état normal (transparent)
        btnNavDashboard.setStyle("-fx-background-color: transparent; -fx-text-fill: #FE4A49; -fx-font-weight: bold;");
    }
}