package tn.esprit.Champions.gui;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import tn.esprit.Champions.models.participations;
import tn.esprit.Champions.models.StatutParticipation;
import tn.esprit.Champions.services.ParticipationService;

import java.sql.SQLException;
import java.time.LocalDateTime;

public class ParticipationController {

    @FXML private TextField searchField;
    @FXML private TableView<participations> participationTable;
    @FXML private TableColumn<participations, Integer> colId, colFormation, colUtilisateur;
    @FXML private TableColumn<participations, LocalDateTime> colDate;
    @FXML private TableColumn<participations, StatutParticipation> colStatut;
    @FXML private TableColumn<participations, Boolean> colPresence;
    @FXML private TableColumn<participations, Float> colNote;
    @FXML private TableColumn<participations, Void> colActions;

    private final ParticipationService ps = new ParticipationService();
    private final ObservableList<participations> masterData = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        initTable();
        setupSearch();
        loadData();
    }

    private void initTable() {
        // --- MASQUER L'ID TECHNIQUE ---
        colId.setVisible(false);

        colFormation.setCellValueFactory(new PropertyValueFactory<>("idFormation"));
        colUtilisateur.setCellValueFactory(new PropertyValueFactory<>("idUtilisateur"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("dateInscription"));
        colStatut.setCellValueFactory(new PropertyValueFactory<>("statut"));
        colPresence.setCellValueFactory(new PropertyValueFactory<>("presence"));
        colNote.setCellValueFactory(new PropertyValueFactory<>("note"));
        setupActionButtons();
    }

    private void loadData() {
        try { masterData.setAll(ps.SelectAll()); } catch (SQLException e) { e.printStackTrace(); }
    }

    private void setupSearch() {
        FilteredList<participations> filteredData = new FilteredList<>(masterData, p -> true);
        searchField.textProperty().addListener((obs, old, newVal) -> {
            filteredData.setPredicate(p -> {
                if (newVal == null || newVal.isEmpty()) return true;
                String f = newVal.toLowerCase();
                return String.valueOf(p.getIdUtilisateur()).contains(f) || p.getStatut().name().toLowerCase().contains(f);
            });
        });
        SortedList<participations> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(participationTable.comparatorProperty());
        participationTable.setItems(sortedData);
    }

    private void setupActionButtons() {
        colActions.setCellFactory(param -> new TableCell<>() {
            private final Button editBtn = new Button("Modifier");
            private final Button deleteBtn = new Button("Supprimer");
            private final Button certBtn = new Button("Certifier");
            private final HBox pane = new HBox(editBtn, deleteBtn, certBtn);

            {
                pane.setSpacing(8); pane.setAlignment(Pos.CENTER);

                // Styles de base
                editBtn.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-background-radius: 5; -fx-cursor: hand;");
                deleteBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-background-radius: 5; -fx-cursor: hand;");
                certBtn.setStyle("-fx-background-color: #f1c40f; -fx-text-fill: white; -fx-background-radius: 5; -fx-cursor: hand;");

                // --- ANIMATIONS HOVER ---
                applyHoverEffect(editBtn, "#2980b9");
                applyHoverEffect(deleteBtn, "#c0392b");
                applyHoverEffect(certBtn, "#f39c12");

                editBtn.setOnAction(e -> showForm(getTableView().getItems().get(getIndex()), null));
                deleteBtn.setOnAction(e -> handleDelete(getTableView().getItems().get(getIndex())));

                // Action pour générer un certificat
                certBtn.setOnAction(e -> {
                    participations p = getTableView().getItems().get(getIndex());
                    if (p.getNote() >= 10) {
                        new CertificatController().showCertForm(null, (long) p.getIdParticipation());
                    } else {
                        showError("Condition non remplie", "L'élève doit avoir une note >= 10 pour être certifié.");
                    }
                });
            }

            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : pane);
            }
        });
    }

    private void applyHoverEffect(Button b, String hoverColor) {
        String originalColor = b.getStyle().split(";")[0]; // Récupère le bg color actuel
        b.setOnMouseEntered(e -> {
            b.setStyle(originalColor.replace(originalColor.split(":")[1].trim(), hoverColor) + "; -fx-text-fill: white; -fx-background-radius: 5; -fx-scale-x: 1.05; -fx-scale-y: 1.05;");
        });
        b.setOnMouseExited(e -> {
            b.setStyle(originalColor + "; -fx-text-fill: white; -fx-background-radius: 5; -fx-scale-x: 1; -fx-scale-y: 1;");
        });
    }

    @FXML private void handleAdding() { showForm(null, null); }

    private void handleDelete(participations p) {
        if (new Alert(Alert.AlertType.CONFIRMATION, "Supprimer l'inscription ?").showAndWait().get() == ButtonType.OK) {
            try { ps.deleteOne(p); loadData(); } catch (SQLException e) { e.printStackTrace(); }
        }
    }

    public void showForm(participations existing, Integer defaultFormationId) {
        boolean isEdit = (existing != null);
        Dialog<participations> dialog = new Dialog<>();
        dialog.setTitle(isEdit ? "Modifier Participation" : "Nouvelle Inscription");

        DialogPane dp = dialog.getDialogPane();
        ButtonType saveType = new ButtonType("Enregistrer", ButtonBar.ButtonData.OK_DONE);
        dp.getButtonTypes().addAll(saveType, ButtonType.CANCEL);

        VBox header = new VBox();
        header.setStyle("-fx-background-color: #1a2a3a; -fx-padding: 20;");
        Label title = new Label(isEdit ? "ÉDITION PARTICIPATION" : "INSCRIPTION ÉLÈVE");
        title.setStyle("-fx-text-fill: #00d4ff; -fx-font-size: 18; -fx-font-weight: bold;");
        header.getChildren().add(title);
        dp.setHeader(header);

        GridPane grid = new GridPane();
        grid.setHgap(15); grid.setVgap(10); grid.setPadding(new Insets(20));

        TextField txtFormation = new TextField();
        if (isEdit) txtFormation.setText(String.valueOf(existing.getIdFormation()));
        else if (defaultFormationId != null) {
            txtFormation.setText(String.valueOf(defaultFormationId));
            txtFormation.setEditable(false);
            txtFormation.setStyle("-fx-background-color: #ecf0f1;");
        }

        TextField txtUtilisateur = new TextField();
        Label errFormation = new Label(); errFormation.setStyle("-fx-text-fill: red; -fx-font-size: 10;");
        Label errUtilisateur = new Label(); errUtilisateur.setStyle("-fx-text-fill: red; -fx-font-size: 10;");
        TextField txtNote = new TextField();
        Label errNote = new Label(); errNote.setStyle("-fx-text-fill: red; -fx-font-size: 10;");
        ComboBox<StatutParticipation> cbStatut = new ComboBox<>(FXCollections.observableArrayList(StatutParticipation.values()));
        CheckBox chkPresence = new CheckBox("Présent à la formation");

        if (isEdit) {
            txtUtilisateur.setText(String.valueOf(existing.getIdUtilisateur()));
            txtNote.setText(String.valueOf(existing.getNote()));
            cbStatut.setValue(existing.getStatut());
            chkPresence.setSelected(existing.isPresence());
        } else {
            cbStatut.setValue(StatutParticipation.PAYEE);
        }

        grid.add(new Label("ID Formation *"), 0, 0); grid.add(txtFormation, 0, 1); grid.add(errFormation, 0, 2);
        grid.add(new Label("ID Utilisateur *"), 1, 0); grid.add(txtUtilisateur, 1, 1); grid.add(errUtilisateur, 1, 2);
        grid.add(new Label("Note finale (/20)"), 0, 3); grid.add(txtNote, 0, 4); grid.add(errNote, 0, 5);
        grid.add(new Label("Statut"), 1, 3); grid.add(cbStatut, 1, 4);
        grid.add(chkPresence, 0, 6, 2, 1);

        dp.setContent(grid);
        Node saveBtn = dp.lookupButton(saveType);

        saveBtn.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            if (!(validateInt(txtFormation, errFormation) && validateInt(txtUtilisateur, errUtilisateur) && validateNote(txtNote, errNote))) {
                event.consume();
            }
        });

        dialog.setResultConverter(btn -> {
            if (btn == saveType) {
                participations p = isEdit ? existing : new participations();
                p.setIdFormation(Integer.parseInt(txtFormation.getText()));
                p.setIdUtilisateur(Integer.parseInt(txtUtilisateur.getText()));
                p.setNote(txtNote.getText().isEmpty() ? 0f : Float.parseFloat(txtNote.getText()));
                p.setStatut(cbStatut.getValue());
                p.setPresence(chkPresence.isSelected());
                if (!isEdit) p.setDateInscription(LocalDateTime.now());
                return p;
            }
            return null;
        });

        dialog.showAndWait().ifPresent(p -> {
            try {
                if (isEdit) ps.updateOne(p); else ps.insertOne(p);
                loadData();
            } catch (SQLException e) { showError("Erreur", "Vérifiez que les IDs existent."); }
        });
    }

    private boolean validateInt(TextField t, Label l) {
        try {
            Integer.parseInt(t.getText());
            t.setStyle("-fx-border-color: green;"); l.setText("");
            return true;
        } catch (Exception e) {
            t.setStyle("-fx-border-color: red;"); l.setText("ID invalide");
            return false;
        }
    }

    private boolean validateNote(TextField t, Label l) {
        if (t.getText().isEmpty()) return true;
        try {
            float n = Float.parseFloat(t.getText());
            if (n < 0 || n > 20) throw new Exception();
            t.setStyle("-fx-border-color: green;"); l.setText("");
            return true;
        } catch (Exception e) {
            t.setStyle("-fx-border-color: red;"); l.setText("Note entre 0 et 20");
            return false;
        }
    }

    private void showError(String t, String c) { new Alert(Alert.AlertType.ERROR, c).showAndWait(); }
}