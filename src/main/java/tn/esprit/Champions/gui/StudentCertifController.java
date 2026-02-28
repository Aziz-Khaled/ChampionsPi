package tn.esprit.Champions.gui;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.properties.TextAlignment;
import javafx.animation.FadeTransition;
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
import javafx.util.Duration;
import tn.esprit.Champions.models.certificats;
import tn.esprit.Champions.services.CertificatService;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;

public class StudentCertifController {

    @FXML private VBox certifsListContainer;

    private final CertificatService cs = new CertificatService();

    @FXML
    public void initialize() {
        FadeTransition ft = new FadeTransition(Duration.millis(800), certifsListContainer);
        ft.setFromValue(0);
        ft.setToValue(1);
        ft.play();

        loadStudentCertifs();
    }

    private void loadStudentCertifs() {
        try {
            List<certificats> list = cs.SelectAll();
            certifsListContainer.getChildren().clear();
            certifsListContainer.setSpacing(15);

            for (certificats c : list) {
                certifsListContainer.getChildren().add(createCertifCard(c));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private HBox createCertifCard(certificats c) {
        HBox card = new HBox(20);
        card.setAlignment(Pos.CENTER_LEFT);
        card.getStyleClass().add("certif-card");
        card.setPadding(new Insets(20));

        VBox info = new VBox(5);
        Label title = new Label("Certificat de Réussite");
        title.getStyleClass().add("certif-title");
        Label code = new Label("Code de vérification : " + c.getCodeVerification());
        code.setStyle("-fx-text-fill: #64748b;");
        info.getChildren().addAll(title, code);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button btnDownload = new Button("📥 Télécharger PDF");
        btnDownload.getStyleClass().add("btn-view-pdf");

        // Action de téléchargement
        btnDownload.setOnAction(e -> generateAndOpenPDF(c, btnDownload));

        card.getChildren().addAll(info, spacer, btnDownload);
        return card;
    }

    private void generateAndOpenPDF(certificats c, Button btn) {
        String dest = System.getProperty("user.home") + "/Downloads/Certificat_" + c.getCodeVerification() + ".pdf";

        try {
            // 1. Création du PDF avec iText
            PdfWriter writer = new PdfWriter(dest);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf);

            document.add(new Paragraph("CHAMPIONS ACADEMY").setFontSize(24).setBold().setTextAlignment(TextAlignment.CENTER));
            document.add(new Paragraph("CERTIFICAT DE RÉUSSITE").setFontSize(18).setTextAlignment(TextAlignment.CENTER));
            document.add(new Paragraph("\nCe certificat est décerné à :").setTextAlignment(TextAlignment.CENTER));
            document.add(new Paragraph("L'Étudiant(e) Rana J").setFontSize(22).setBold().setItalic().setTextAlignment(TextAlignment.CENTER));
            document.add(new Paragraph("\nPour avoir complété avec succès la formation associée au code : " + c.getCodeVerification()));
            document.add(new Paragraph("Délivré le : " + c.getDateEmission()));

            document.close();

            // 2. Feedback Visuel
            btn.setText("✅ Téléchargé");
            btn.setStyle("-fx-background-color: #10b981; -fx-text-fill: white;");

            // 3. Alerte stylisée
            showStyledAlert("Téléchargement réussi", "Le certificat a été enregistré dans vos Téléchargements.");

            // 4. Ouverture automatique du fichier
            File file = new File(dest);
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(file);
            }

        } catch (IOException e) {
            showStyledAlert("Erreur", "Impossible de générer le PDF : " + e.getMessage());
        }
    }

    private void showStyledAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Système Champions");
        alert.setHeaderText(title);
        alert.setContentText(content);

        DialogPane dialogPane = alert.getDialogPane();
        URL css = getClass().getResource("/style.css");
        if (css != null) {
            dialogPane.getStylesheets().add(css.toExternalForm());
            dialogPane.getStyleClass().add("custom-dialog");
        }
        alert.showAndWait();
    }

    @FXML
    private void handleGoToDashboard(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/student_dashboard.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}