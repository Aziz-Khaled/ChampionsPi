package tn.esprit.Champions.gui;

import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.properties.HorizontalAlignment;
import com.itextpdf.layout.properties.TextAlignment;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.stage.FileChooser;
import tn.esprit.Champions.models.certificats;
import tn.esprit.Champions.models.MentionCertificat;
import tn.esprit.Champions.services.CertificatService;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.UUID;

public class CertificatController {

    @FXML private TableView<certificats> certTable;
    @FXML private TableColumn<certificats, Integer> colId;
    @FXML private TableColumn<certificats, Long> colPart;
    @FXML private TableColumn<certificats, String> colCode, colUrl;
    @FXML private TableColumn<certificats, MentionCertificat> colMention;
    @FXML private TableColumn<certificats, LocalDate> colDate;

    @FXML private Label lblTotalCertifs, lblMonthCertifs, lblTopMention;

    private final CertificatService cs = new CertificatService();
    private final ObservableList<certificats> masterData = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        if (certTable != null) {
            colId.setVisible(false); // Masquer l'ID technique
            colPart.setCellValueFactory(new PropertyValueFactory<>("idParticipation"));
            colMention.setCellValueFactory(new PropertyValueFactory<>("mention"));
            colCode.setCellValueFactory(new PropertyValueFactory<>("codeVerification"));
            colDate.setCellValueFactory(new PropertyValueFactory<>("dateEmission"));
            colUrl.setCellValueFactory(new PropertyValueFactory<>("urlFichier"));
        }
        loadData();
    }

    public void loadData() {
        try {
            masterData.setAll(cs.SelectAll());
            if (certTable != null) certTable.setItems(masterData);
            updateStats();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void updateStats() {
        if (lblTotalCertifs == null) return;
        int total = masterData.size();
        long thisMonth = masterData.stream()
                .filter(c -> c.getDateEmission() != null &&
                        c.getDateEmission().getMonth() == LocalDate.now().getMonth())
                .count();

        lblTotalCertifs.setText(String.valueOf(total));
        lblMonthCertifs.setText(String.valueOf(thisMonth));
        lblTopMention.setText(total > 0 ? "VALIDE" : "-");
    }

    public void showCertForm(certificats existing, Long partId) {
        Dialog<certificats> dialog = new Dialog<>();
        dialog.setTitle("Nouveau Certificat Officiel");
        ButtonType saveType = new ButtonType("Générer PDF", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(15); grid.setVgap(15); grid.setPadding(new Insets(20));

        TextField txtPart = new TextField(partId != null ? String.valueOf(partId) : "");
        txtPart.setPromptText("Ex: 102");

        DatePicker pickerDate = new DatePicker(LocalDate.now());
        ComboBox<MentionCertificat> cbMention = new ComboBox<>(FXCollections.observableArrayList(MentionCertificat.values()));
        cbMention.setValue(MentionCertificat.VALIDE);

        TextField txtUrl = new TextField("certifs/cert_" + System.currentTimeMillis() + ".pdf");
        Button btnBrowse = new Button("📁 Parcourir");

        btnBrowse.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
            File f = fc.showSaveDialog(null);
            if (f != null) txtUrl.setText(f.getAbsolutePath());
        });

        grid.add(new Label("ID Participation:"), 0, 0); grid.add(txtPart, 1, 0);
        grid.add(new Label("Date d'émission:"), 0, 1);  grid.add(pickerDate, 1, 1);
        grid.add(new Label("Mention:"), 0, 2);         grid.add(cbMention, 1, 2);
        grid.add(new Label("Sauvegarder sous:"), 0, 3); grid.add(txtUrl, 1, 3);
        grid.add(btnBrowse, 2, 3);

        dialog.getDialogPane().setContent(grid);

        // --- SÉCURITÉ : Validation du bouton OK ---
        final Button btOk = (Button) dialog.getDialogPane().lookupButton(saveType);
        btOk.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            if (txtPart.getText().trim().isEmpty() || !txtPart.getText().matches("\\d+")) {
                new Alert(Alert.AlertType.WARNING, "Veuillez saisir un ID Participation valide (nombre).").show();
                event.consume();
            }
        });

        dialog.setResultConverter(btn -> {
            if (btn == saveType) {
                certificats c = new certificats();
                c.setIdParticipation(Long.parseLong(txtPart.getText().trim()));
                c.setDateEmission(pickerDate.getValue());
                c.setMention(cbMention.getValue());
                c.setUrlFichier(txtUrl.getText());
                c.setCodeVerification(UUID.randomUUID().toString().substring(0, 8).toUpperCase());
                return c;
            }
            return null;
        });

        dialog.showAndWait().ifPresent(c -> {
            try {
                genererPDF(c);
                cs.insertOne(c);
                loadData();
            } catch (Exception e) {
                new Alert(Alert.AlertType.ERROR, "Erreur lors de l'enregistrement. Vérifiez vos données.").show();
                e.printStackTrace();
            }
        });
    }

    private void genererPDF(certificats c) throws IOException {
        File folder = new File("certifs");
        if (!folder.exists()) folder.mkdirs();

        PdfWriter writer = new PdfWriter(c.getUrlFichier());
        PdfDocument pdf = new PdfDocument(writer);
        Document doc = new Document(pdf);

        doc.add(new Paragraph("CERTIFICAT DE RÉUSSITE").setBold().setFontSize(24).setTextAlignment(TextAlignment.CENTER));
        doc.add(new Paragraph("Champions Academy").setItalic().setTextAlignment(TextAlignment.CENTER));
        doc.add(new Paragraph("\n\nCe document atteste que l'étudiant lié à la participation n°" + c.getIdParticipation()));
        doc.add(new Paragraph("a validé sa formation avec la mention : " + c.getMention()));
        doc.add(new Paragraph("Fait le : " + c.getDateEmission()));
        doc.add(new Paragraph("Code authentification : " + c.getCodeVerification()).setFontSize(10));

        // API QR CODE
        try {
            String apiQR = "https://api.qrserver.com/v1/create-qr-code/?size=120x120&data=VERIFY-" + c.getCodeVerification();
            Image qrCode = new Image(ImageDataFactory.create(new URL(apiQR)));
            qrCode.setHorizontalAlignment(HorizontalAlignment.CENTER);
            doc.add(new Paragraph("\n"));
            doc.add(qrCode);
            doc.add(new Paragraph("Scanner pour vérification").setFontSize(8).setTextAlignment(TextAlignment.CENTER));
        } catch (Exception e) {
            doc.add(new Paragraph("\n(QR Code indisponible)"));
        }
        doc.close();
    }

    @FXML private void handleAddManual() { showCertForm(null, null); }

    @FXML private void handleDelete() {
        certificats s = certTable.getSelectionModel().getSelectedItem();
        if (s != null && new Alert(Alert.AlertType.CONFIRMATION, "Supprimer ce certificat ?").showAndWait().get() == ButtonType.OK) {
            try { cs.deleteOne(s); new File(s.getUrlFichier()).delete(); loadData(); } catch (SQLException e) { e.printStackTrace(); }
        }
    }
}