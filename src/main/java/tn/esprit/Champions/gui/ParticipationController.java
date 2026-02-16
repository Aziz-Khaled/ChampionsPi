package tn.esprit.Champions.gui;

import javafx.animation.FadeTransition;
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
import javafx.util.Duration;
import tn.esprit.Champions.models.participations;
import tn.esprit.Champions.models.StatutParticipation;
import tn.esprit.Champions.services.ParticipationService;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ParticipationController {

    @FXML private TextField searchField;
    @FXML private TableView<participations> participationTable;
    @FXML private TableColumn<participations, Integer> colId, colUtilisateur;
    @FXML private TableColumn<participations, String> colFormation;
    @FXML private TableColumn<participations, LocalDateTime> colDate;
    @FXML private TableColumn<participations, StatutParticipation> colStatut;
    @FXML private TableColumn<participations, Boolean> colPresence;
    @FXML private TableColumn<participations, Float> colNote;
    @FXML private TableColumn<participations, Void> colActions;

    private final ParticipationService ps = new ParticipationService();
    private final ObservableList<participations> masterData = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        // Appliquer l'animation d'entrée au tableau
        applyFadeAnimation(participationTable);

        initTable();
        setupSearch();
        loadData();
    }

    /**
     * Animation créative : Apparition progressive du tableau
     */
    private void applyFadeAnimation(Node node) {
        FadeTransition fadeIn = new FadeTransition(Duration.millis(800), node);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);
        fadeIn.play();
    }

    private void initTable() {
        colId.setVisible(false);

        // Configuration des colonnes avec les propriétés du modèle
        colFormation.setCellValueFactory(new PropertyValueFactory<>("titreFormation"));
        colUtilisateur.setCellValueFactory(new PropertyValueFactory<>("idUtilisateur"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("dateInscription"));
        colStatut.setCellValueFactory(new PropertyValueFactory<>("statut"));
        colPresence.setCellValueFactory(new PropertyValueFactory<>("presence"));
        colNote.setCellValueFactory(new PropertyValueFactory<>("note"));

        // Formatage créatif de la date
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm");
        colDate.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(LocalDateTime item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.format(formatter));
            }
        });

        // Cell Factory pour les badges de statut (Indigo/Ambre)
        colStatut.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(StatutParticipation item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    getStyleClass().removeAll("status-paid", "status-pending");
                } else {
                    setText(item.name());
                    getStyleClass().removeAll("status-paid", "status-pending");
                    if (item == StatutParticipation.PAYEE) {
                        getStyleClass().add("status-paid");
                    } else {
                        getStyleClass().add("status-pending");
                    }
                }
            }
        });

        setupActionButtons();
    }

    private void loadData() {
        try {
            masterData.setAll(ps.SelectAll());
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void setupSearch() {
        FilteredList<participations> filteredData = new FilteredList<>(masterData, p -> true);
        searchField.textProperty().addListener((obs, old, newVal) -> {
            filteredData.setPredicate(p -> {
                if (newVal == null || newVal.isEmpty()) return true;
                String f = newVal.toLowerCase();
                return String.valueOf(p.getIdUtilisateur()).contains(f) ||
                        p.getStatut().name().toLowerCase().contains(f) ||
                        (p.getTitreFormation() != null && p.getTitreFormation().toLowerCase().contains(f));
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
                pane.setSpacing(10);
                pane.setAlignment(Pos.CENTER);

                // Application des classes CSS créatives
                editBtn.getStyleClass().add("button-primary");
                deleteBtn.getStyleClass().add("button-delete");
                certBtn.getStyleClass().add("button-cert");

                editBtn.setOnAction(e -> showForm(getTableView().getItems().get(getIndex()), null));
                deleteBtn.setOnAction(e -> handleDelete(getTableView().getItems().get(getIndex())));
                certBtn.setOnAction(e -> {
                    participations p = getTableView().getItems().get(getIndex());
                    if (p.getNote() >= 10) {
                        new CertificatController().showCertForm(null, (long) p.getIdParticipation());
                    } else {
                        showError("Note insuffisante", "L'élève doit avoir au moins 10/20.");
                    }
                });
            }

            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : pane);
            }
        });
    }

    @FXML private void handleAdding() { showForm(null, null); }

    private void handleDelete(participations p) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Voulez-vous vraiment supprimer cette inscription ?");
        // Application du CSS à l'alerte
        if (alert.getDialogPane().getStylesheets() != null) {
            alert.getDialogPane().getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
        }
        if (alert.showAndWait().get() == ButtonType.OK) {
            try { ps.deleteOne(p); loadData(); } catch (SQLException e) { e.printStackTrace(); }
        }
    }

    public void showForm(participations existing, Integer defaultFormationId) {
        boolean isEdit = (existing != null);
        Dialog<participations> dialog = new Dialog<>();
        dialog.getDialogPane().getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
        dialog.getDialogPane().getStyleClass().add("custom-dialog");

        dialog.setTitle(isEdit ? "Mise à jour" : "Nouvelle inscription");

        DialogPane dp = dialog.getDialogPane();
        ButtonType saveType = new ButtonType("Valider", ButtonBar.ButtonData.OK_DONE);
        dp.getButtonTypes().addAll(saveType, ButtonType.CANCEL);

        VBox header = new VBox();
        header.getStyleClass().add("dialog-header");
        Label title = new Label(isEdit ? "MODIFIER LA PARTICIPATION" : "AJOUTER UN PARTICIPANT");
        title.getStyleClass().add("dialog-title");
        header.getChildren().add(title);
        dp.setHeader(header);

        GridPane grid = new GridPane();
        grid.setHgap(20); grid.setVgap(15); grid.setPadding(new Insets(25));

        TextField txtFormation = new TextField();
        TextField txtUtilisateur = new TextField();
        TextField txtNote = new TextField();
        ComboBox<StatutParticipation> cbStatut = new ComboBox<>(FXCollections.observableArrayList(StatutParticipation.values()));
        CheckBox chkPresence = new CheckBox("Présence confirmée");

        Label errFormation = new Label(); errFormation.getStyleClass().add("error-label");
        Label errUtilisateur = new Label(); errUtilisateur.getStyleClass().add("error-label");
        Label errNote = new Label(); errNote.getStyleClass().add("error-label");

        if (isEdit) {
            txtFormation.setText(String.valueOf(existing.getIdFormation()));
            txtUtilisateur.setText(String.valueOf(existing.getIdUtilisateur()));
            txtNote.setText(String.valueOf(existing.getNote()));
            cbStatut.setValue(existing.getStatut());
            chkPresence.setSelected(existing.isPresence());
        } else {
            if (defaultFormationId != null) txtFormation.setText(String.valueOf(defaultFormationId));
            cbStatut.setValue(StatutParticipation.PAYEE);
        }

        grid.add(new Label("ID Formation"), 0, 0); grid.add(txtFormation, 0, 1); grid.add(errFormation, 0, 2);
        grid.add(new Label("ID Utilisateur"), 1, 0); grid.add(txtUtilisateur, 1, 1); grid.add(errUtilisateur, 1, 2);
        grid.add(new Label("Note finale"), 0, 3); grid.add(txtNote, 0, 4); grid.add(errNote, 0, 5);
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
            } catch (SQLException e) { showError("Erreur SQL", "Veuillez vérifier les IDs saisis."); }
        });
    }

    private boolean validateInt(TextField t, Label l) {
        try {
            Integer.parseInt(t.getText());
            t.getStyleClass().remove("field-error");
            l.setText("");
            return true;
        } catch (Exception e) {
            if (!t.getStyleClass().contains("field-error")) t.getStyleClass().add("field-error");
            l.setText("Format numérique requis");
            return false;
        }
    }

    private boolean validateNote(TextField t, Label l) {
        if (t.getText().isEmpty()) return true;
        try {
            float n = Float.parseFloat(t.getText());
            if (n < 0 || n > 20) throw new Exception();
            t.getStyleClass().remove("field-error");
            l.setText("");
            return true;
        } catch (Exception e) {
            if (!t.getStyleClass().contains("field-error")) t.getStyleClass().add("field-error");
            l.setText("Note entre 0 et 20");
            return false;
        }
    }

    private void showError(String t, String c) {
        Alert a = new Alert(Alert.AlertType.ERROR, c);
        if (a.getDialogPane().getStylesheets() != null) {
            a.getDialogPane().getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
        }
        a.showAndWait();
    }
}