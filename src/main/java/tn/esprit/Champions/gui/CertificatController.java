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
import tn.esprit.Champions.models.certificats;
import tn.esprit.Champions.models.MentionCertificat;
import tn.esprit.Champions.services.CertificatService;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class CertificatController {

    @FXML private TableView<certificats> certTable;
    @FXML private TableColumn<certificats, String> colEleve;
    @FXML private TableColumn<certificats, String> colFormation;
    @FXML private TableColumn<certificats, MentionCertificat> colMention;
    @FXML private TableColumn<certificats, String> colCode;
    @FXML private TableColumn<certificats, LocalDate> colDate;
    @FXML private TableColumn<certificats, Void> colActions;

    @FXML private Label lblTotalCertifs, lblMonthCertifs, lblTopMention;

    private final CertificatService cs = new CertificatService();
    private final ObservableList<certificats> masterData = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        // Liaison avec les attributs du modèle (y compris les transients)
        colEleve.setCellValueFactory(new PropertyValueFactory<>("nomEtudiant"));
        colFormation.setCellValueFactory(new PropertyValueFactory<>("nomFormation"));
        colMention.setCellValueFactory(new PropertyValueFactory<>("mention"));
        colCode.setCellValueFactory(new PropertyValueFactory<>("codeVerification"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("dateEmission"));

        setupActionsColumn();
        loadData();
    }

    private void setupActionsColumn() {
        colActions.setCellFactory(param -> new TableCell<>() {
            private final Button btn = new Button("👁 Voir PDF");
            {
                btn.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-background-radius: 5; -fx-cursor: hand;");
                btn.setOnAction(event -> {
                    certificats c = getTableView().getItems().get(getIndex());
                    openFile(c.getUrlFichier());
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btn);
            }
        });
    }

    private void openFile(String path) {
        try {
            File file = new File(path);
            if (file.exists()) {
                Desktop.getDesktop().open(file);
            } else {
                new Alert(Alert.AlertType.ERROR, "Fichier introuvable : " + path).show();
            }
        } catch (IOException e) { e.printStackTrace(); }
    }

    public void loadData() {
        try {
            // Force le rafraîchissement complet de la liste
            masterData.setAll(cs.SelectAll());
            certTable.setItems(masterData);
            certTable.refresh();
            updateStats();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void updateStats() {
        int total = masterData.size();
        lblTotalCertifs.setText(String.valueOf(total));

        // Calcul pour ce mois
        long countMonth = masterData.stream()
                .filter(c -> c.getDateEmission().getMonth() == LocalDate.now().getMonth()
                        && c.getDateEmission().getYear() == LocalDate.now().getYear())
                .count();
        lblMonthCertifs.setText(String.valueOf(countMonth));

        // --- CALCUL RÉEL DE LA MENTION DOMINANTE ---
        if (total > 0) {
            // On groupe par mention et on compte les occurrences
            Map<MentionCertificat, Long> counts = masterData.stream()
                    .collect(Collectors.groupingBy(certificats::getMention, Collectors.counting()));

            // On cherche la mention qui a le maximum de votes
            MentionCertificat dominante = counts.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse(MentionCertificat.VALIDE);

            lblTopMention.setText(dominante.toString());

            // Petit bonus : Changer la couleur selon la mention dominante
            if (dominante == MentionCertificat.EXCELLENT) {
                lblTopMention.setStyle("-fx-font-size: 26; -fx-font-weight: bold; -fx-text-fill: #f1c40f;"); // Or
            } else {
                lblTopMention.setStyle("-fx-font-size: 26; -fx-font-weight: bold; -fx-text-fill: #27ae60;"); // Vert
            }
        } else {
            lblTopMention.setText("-");
        }
    }

    private MentionCertificat calculerMention(float note) {
        if (note >= 16) return MentionCertificat.EXCELLENT;
        // On s'adapte à votre enum qui contient VALIDE et EXCELLENT
        return MentionCertificat.VALIDE;
    }

    @FXML
    private void handleAddManual() {
        Dialog<certificats> dialog = new Dialog<>();
        dialog.setTitle("🎓 Génération Certificat");
        ButtonType genType = new ButtonType("Générer & Enregistrer", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(genType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(15); grid.setVgap(15); grid.setPadding(new Insets(25));

        ComboBox<Map<String, Object>> cbEleves = new ComboBox<>();
        cbEleves.setPrefWidth(300);

        // Custom cell pour afficher le nom et la note
        cbEleves.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(Map<String, Object> item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : (String) item.get("displayText"));
            }
        });
        cbEleves.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(Map<String, Object> item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : (String) item.get("displayText"));
            }
        });

        try {
            cbEleves.setItems(FXCollections.observableArrayList(cs.getEligibleParticipations()));
        } catch (SQLException e) { e.printStackTrace(); }

        grid.add(new Label("Candidat éligible :"), 0, 0);
        grid.add(cbEleves, 1, 0);
        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(btn -> {
            if (btn == genType && cbEleves.getValue() != null) {
                Map<String, Object> sel = cbEleves.getValue();
                certificats c = new certificats();
                c.setIdParticipation((Long) sel.get("id"));
                c.setDateEmission(LocalDate.now());
                c.setMention(calculerMention((float) sel.get("note")));
                c.setCodeVerification(UUID.randomUUID().toString().substring(0, 8).toUpperCase());
                c.setUrlFichier("certifs/Certif_" + c.getCodeVerification() + ".pdf");
                c.setNomEtudiant((String) sel.get("nomComplet"));
                c.setNomFormation((String) sel.get("formation"));
                return c;
            }
            return null;
        });

        dialog.showAndWait().ifPresent(c -> {
            try {
                genererPDF(c);
                cs.insertOne(c);
                loadData(); // Rafraîchit le tableau
                openFile(c.getUrlFichier());
            } catch (Exception e) { e.printStackTrace(); }
        });
    }

    private void genererPDF(certificats c) throws IOException {
        new File("certifs").mkdirs();
        PdfWriter writer = new PdfWriter(c.getUrlFichier());
        PdfDocument pdf = new PdfDocument(writer);
        Document doc = new Document(pdf);

        doc.add(new Paragraph("CHAMPIONS ACADEMY").setFontSize(10).setCharacterSpacing(2));
        doc.add(new Paragraph("\n\nCERTIFICAT DE RÉUSSITE").setBold().setFontSize(28).setTextAlignment(TextAlignment.CENTER));
        doc.add(new Paragraph("Décerné à").setItalic().setTextAlignment(TextAlignment.CENTER));
        doc.add(new Paragraph(c.getNomEtudiant()).setBold().setFontSize(24).setTextAlignment(TextAlignment.CENTER).setUnderline());
        doc.add(new Paragraph("Pour la validation de la formation :").setTextAlignment(TextAlignment.CENTER));
        doc.add(new Paragraph(c.getNomFormation()).setBold().setFontSize(18).setTextAlignment(TextAlignment.CENTER));
        doc.add(new Paragraph("Mention : " + c.getMention()).setBold().setMarginTop(20).setTextAlignment(TextAlignment.CENTER));

        try {
            String qrApi = "https://api.qrserver.com/v1/create-qr-code/?size=100x100&data=" + c.getCodeVerification();
            Image qr = new Image(ImageDataFactory.create(new URL(qrApi))).setHorizontalAlignment(HorizontalAlignment.CENTER).setMarginTop(30);
            doc.add(qr);
            doc.add(new Paragraph("Authentification : " + c.getCodeVerification()).setFontSize(8).setTextAlignment(TextAlignment.CENTER));
        } catch (Exception e) {
            doc.add(new Paragraph("\n[QR indisponible]").setFontSize(8).setTextAlignment(TextAlignment.CENTER));
        }
        doc.close();
    }

    @FXML private void handleDelete() {
        certificats s = certTable.getSelectionModel().getSelectedItem();
        if (s != null) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Supprimer ce certificat ?", ButtonType.YES, ButtonType.NO);
            if (alert.showAndWait().get() == ButtonType.YES) {
                try {
                    cs.deleteOne(s);
                    File f = new File(s.getUrlFichier());
                    if(f.exists()) f.delete();
                    loadData();
                } catch (SQLException e) { e.printStackTrace(); }
            }
        }
    }
}