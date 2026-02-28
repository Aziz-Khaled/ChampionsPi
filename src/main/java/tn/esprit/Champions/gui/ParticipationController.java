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
import javafx.util.Duration;
import tn.esprit.Champions.models.participations;
import tn.esprit.Champions.models.StatutParticipation;
import tn.esprit.Champions.services.ParticipationService;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ParticipationController {

    @FXML private TextField searchField;
    @FXML private TableView<participations> participationTable;
    @FXML private TableColumn<participations, String> colUtilisateur, colFormation;
    @FXML private TableColumn<participations, LocalDateTime> colDate;
    @FXML private TableColumn<participations, StatutParticipation> colStatut;
    @FXML private TableColumn<participations, Boolean> colPresence;
    @FXML private TableColumn<participations, Float> colNote;
    @FXML private TableColumn<participations, Void> colActions;

    private final ParticipationService ps = new ParticipationService();
    private final ObservableList<participations> masterData = FXCollections.observableArrayList();
    private FilteredList<participations> filteredData;

    /**
     * Classe utilitaire pour lier l'ID et le Nom dans les ComboBox
     */
    private static class ComboItem {
        int id;
        String label;
        ComboItem(int id, String label) { this.id = id; this.label = label; }
        @Override public String toString() { return label; }
    }

    @FXML
    public void initialize() {
        applyFadeAnimation(participationTable);
        initTable();
        setupSearch();
        loadData();
    }

    private void applyFadeAnimation(Node node) {
        FadeTransition fadeIn = new FadeTransition(Duration.millis(800), node);
        fadeIn.setFromValue(0.0); fadeIn.setToValue(1.0);
        fadeIn.play();
    }

    private void initTable() {
        colFormation.setCellValueFactory(new PropertyValueFactory<>("titreFormation"));
        colUtilisateur.setCellValueFactory(new PropertyValueFactory<>("nomUtilisateur"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("dateInscription"));
        colStatut.setCellValueFactory(new PropertyValueFactory<>("statut"));
        colPresence.setCellValueFactory(new PropertyValueFactory<>("presence"));
        colNote.setCellValueFactory(new PropertyValueFactory<>("note"));

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        colDate.setCellFactory(column -> new TableCell<>() {
            @Override protected void updateItem(LocalDateTime item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.format(formatter));
            }
        });

        colPresence.setCellFactory(column -> new TableCell<>() {
            @Override protected void updateItem(Boolean item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) setText(null);
                else {
                    setText(item ? "✅ Présent" : "❌ Absent");
                    setStyle(item ? "-fx-text-fill: #2ecc71; -fx-font-weight: bold;" : "-fx-text-fill: #e74c3c;");
                }
            }
        });

        colStatut.setCellFactory(column -> new TableCell<>() {
            @Override protected void updateItem(StatutParticipation item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); }
                else {
                    setText(item.name());
                    String base = "-fx-background-radius: 12; -fx-padding: 4 10; -fx-alignment: center; -fx-font-weight: bold;";
                    if(item == StatutParticipation.PAYEE)
                        setStyle(base + "-fx-background-color: #d4edda; -fx-text-fill: #155724;");
                    else
                        setStyle(base + "-fx-background-color: #f8d7da; -fx-text-fill: #721c24;");
                }
            }
        });

        setupActionButtons();
    }

    private void loadData() {
        try {
            masterData.setAll(ps.SelectAll());
            participationTable.sort();
        } catch (SQLException e) { showError("Erreur SQL", e.getMessage()); }
    }

    private void setupSearch() {
        filteredData = new FilteredList<>(masterData, p -> true);
        searchField.textProperty().addListener((obs, old, newVal) -> {
            filteredData.setPredicate(p -> {
                if (newVal == null || newVal.isEmpty()) return true;
                String lower = newVal.toLowerCase();
                return (p.getNomUtilisateur() != null && p.getNomUtilisateur().toLowerCase().contains(lower)) ||
                        (p.getTitreFormation() != null && p.getTitreFormation().toLowerCase().contains(lower));
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
            private final HBox pane = new HBox(editBtn, deleteBtn);
            {
                pane.setSpacing(10); pane.setAlignment(Pos.CENTER);
                editBtn.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-cursor: hand;");
                deleteBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-cursor: hand;");
                editBtn.setOnAction(e -> showForm(getTableView().getItems().get(getIndex())));
                deleteBtn.setOnAction(e -> handleDelete(getTableView().getItems().get(getIndex())));
            }
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : pane);
            }
        });
    }

    @FXML private void handleAdding() { showForm(null); }

    private void handleDelete(participations p) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Supprimer l'inscription ?", ButtonType.YES, ButtonType.NO);
        if (confirm.showAndWait().orElse(ButtonType.NO) == ButtonType.YES) {
            try { ps.deleteOne(p); loadData(); }
            catch (SQLException e) { e.printStackTrace();
                showError("Erreur", "Action impossible."); }
        }
    }

    public void showForm(participations existing) {
        boolean isEdit = (existing != null);
        Dialog<participations> dialog = new Dialog<>();
        dialog.setTitle(isEdit ? "Modifier Participation" : "Nouvelle Inscription");

        DialogPane dp = dialog.getDialogPane();
        ButtonType saveType = new ButtonType(isEdit ? "Modifier" : "Enregistrer", ButtonBar.ButtonData.OK_DONE);
        dp.getButtonTypes().addAll(saveType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(15); grid.setVgap(15); grid.setPadding(new Insets(25));

        // Utilisation de ComboItem au lieu de String
        ComboBox<ComboItem> cbFormation = new ComboBox<>();
        ComboBox<ComboItem> cbUtilisateur = new ComboBox<>();
        TextField txtNote = new TextField(isEdit ? String.valueOf(existing.getNote()) : "0.0");
        ComboBox<StatutParticipation> cbStatut = new ComboBox<>(FXCollections.observableArrayList(StatutParticipation.values()));
        CheckBox chkPresence = new CheckBox("Présent");

        try {
            // Chargement des listes d'objets ComboItem
            List<ComboItem> formItems = new ArrayList<>();
            ps.getFormationsMap().forEach((titre, id) -> formItems.add(new ComboItem(id, titre)));
            cbFormation.setItems(FXCollections.observableArrayList(formItems));

            List<ComboItem> userItems = new ArrayList<>();
            ps.getUsersMap().forEach((nom, id) -> userItems.add(new ComboItem(id, nom)));
            cbUtilisateur.setItems(FXCollections.observableArrayList(userItems));

            if (isEdit) {
                // Sélection par ID pour éviter les erreurs de texte
                cbFormation.getItems().stream()
                        .filter(i -> i.id == existing.getIdFormation())
                        .findFirst().ifPresent(cbFormation::setValue);

                cbUtilisateur.getItems().stream()
                        .filter(i -> i.id == existing.getIdUtilisateur())
                        .findFirst().ifPresent(cbUtilisateur::setValue);

                cbStatut.setValue(existing.getStatut());
                chkPresence.setSelected(existing.isPresence());
            } else {
                cbStatut.setValue(StatutParticipation.PAYEE);
            }
        } catch (SQLException e) {
            showError("Erreur", "Impossible de charger les données.");
            return;
        }

        grid.add(new Label("📚 Formation :"), 0, 0); grid.add(cbFormation, 1, 0);
        grid.add(new Label("👤 Apprenant :"), 0, 1); grid.add(cbUtilisateur, 1, 1);
        grid.add(new Label("⭐ Note (/20) :"), 0, 2); grid.add(txtNote, 1, 2);
        grid.add(new Label("💰 Statut :"), 0, 3); grid.add(cbStatut, 1, 3);
        grid.add(chkPresence, 1, 4);

        dp.setContent(grid);

        dialog.setResultConverter(btn -> {
            if (btn == saveType) {
                try {
                    ComboItem fItem = cbFormation.getValue();
                    ComboItem uItem = cbUtilisateur.getValue();

                    if (fItem == null || uItem == null) {
                        showError("Erreur", "Veuillez remplir tous les champs.");
                        return null;
                    }

                    participations p = isEdit ? existing : new participations();

                    // On récupère l'ID directement de l'objet sélectionné
                    p.setIdFormation(fItem.id);
                    p.setIdUtilisateur(uItem.id);

                    p.setNote(Float.parseFloat(txtNote.getText()));
                    p.setPresence(chkPresence.isSelected());
                    p.setStatut(cbStatut.getValue());

                    if (!isEdit) p.setDateInscription(LocalDateTime.now());
                    return p;
                } catch (Exception e) {
                    return null;
                }
            }
            return null;
        });

        dialog.showAndWait().ifPresent(p -> {
            try {
                if (isEdit) ps.updateOne(p); else ps.insertOne(p);
                loadData();
            } catch (SQLException e) { showError("Erreur DB", e.getMessage()); }
        });
    }

    private void showError(String t, String c) {
        Alert a = new Alert(Alert.AlertType.ERROR, c);
        a.setTitle(t); a.setHeaderText(null); a.showAndWait();
    }
}