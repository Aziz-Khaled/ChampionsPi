package tn.esprit.Champions.gui;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import tn.esprit.Champions.models.certificats;
import tn.esprit.Champions.models.MentionCertificat;
import tn.esprit.Champions.services.CertificatService;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.UUID;

public class CertificatController {

    @FXML private TextField searchField;
    @FXML private TableView<certificats> certificatTable;
    @FXML private TableColumn<certificats, Integer> colId;
    @FXML private TableColumn<certificats, Long> colParticipation;
    @FXML private TableColumn<certificats, LocalDate> colDate;
    @FXML private TableColumn<certificats, String> colCode, colUrl;
    @FXML private TableColumn<certificats, MentionCertificat> colMention;
    @FXML private TableColumn<certificats, Void> colActions;

    private final CertificatService cs = new CertificatService();
    private final ObservableList<certificats> masterData = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        colId.setCellValueFactory(new PropertyValueFactory<>("idCertificat"));
        colParticipation.setCellValueFactory(new PropertyValueFactory<>("idParticipation"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("dateEmission"));
        colCode.setCellValueFactory(new PropertyValueFactory<>("codeVerification"));
        colMention.setCellValueFactory(new PropertyValueFactory<>("mention"));
        colUrl.setCellValueFactory(new PropertyValueFactory<>("urlFichier"));

        setupActions();
        loadData();
    }

    private void loadData() {
        try { masterData.setAll(cs.SelectAll()); certificatTable.setItems(masterData); }
        catch (SQLException e) { e.printStackTrace(); }
    }

    @FXML
    private void handleAdding() {
        showForm(null);
    }

    private void showForm(certificats existing) {
        boolean isEdit = (existing != null);
        Dialog<certificats> dialog = new Dialog<>();
        dialog.setTitle(isEdit ? "Modifier Certificat" : "Générer Certificat");

        ButtonType saveType = new ButtonType("Enregistrer", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10); grid.setPadding(new Insets(20));

        TextField txtPart = new TextField();
        txtPart.setPromptText("ID Participation");

        ComboBox<MentionCertificat> cbMention = new ComboBox<>(FXCollections.observableArrayList(MentionCertificat.values()));
        cbMention.setValue(MentionCertificat.VALIDE);

        TextField txtUrl = new TextField();
        txtUrl.setPromptText("Lien du certificat (PDF)");

        if (isEdit) {
            txtPart.setText(String.valueOf(existing.getIdParticipation()));
            cbMention.setValue(existing.getMention());
            txtUrl.setText(existing.getUrlFichier());
        }

        grid.add(new Label("Participation ID:"), 0, 0); grid.add(txtPart, 1, 0);
        grid.add(new Label("Mention:"), 0, 1); grid.add(cbMention, 1, 1);
        grid.add(new Label("URL PDF:"), 0, 2); grid.add(txtUrl, 1, 2);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(btn -> {
            if (btn == saveType) {
                certificats c = isEdit ? existing : new certificats();
                c.setIdParticipation(Long.parseLong(txtPart.getText()));
                c.setMention(cbMention.getValue());
                c.setUrlFichier(txtUrl.getText());
                if (!isEdit) {
                    c.setDateEmission(LocalDate.now());
                    // Génération auto d'un code unique "FIN-XXXX"
                    c.setCodeVerification("FIN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
                }
                return c;
            }
            return null;
        });

        dialog.showAndWait().ifPresent(c -> {
            try {
                if (isEdit) cs.updateOne(c); else cs.insertOne(c);
                loadData();
            } catch (SQLException e) {
                new Alert(Alert.AlertType.ERROR, "Erreur lors de l'enregistrement").show();
            }
        });
    }

    private void setupActions() {
        // Similaire aux précédents pour Supprimer/Modifier
    }
}