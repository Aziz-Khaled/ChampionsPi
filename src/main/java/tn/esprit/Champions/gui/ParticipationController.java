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

    @FXML
    public void initialize() {
        applyFadeAnimation(participationTable);
        initTable();
        setupSearch();
        loadData();
    }

    private void applyFadeAnimation(Node node) {
        FadeTransition fadeIn = new FadeTransition(Duration.millis(800), node);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);
        fadeIn.play();
    }

    private void initTable() {
        // Liaison directe avec les propriétés du modèle (incluant celles de la jointure SQL)
        colFormation.setCellValueFactory(new PropertyValueFactory<>("titreFormation"));
        colUtilisateur.setCellValueFactory(new PropertyValueFactory<>("nomUtilisateur"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("dateInscription"));
        colStatut.setCellValueFactory(new PropertyValueFactory<>("statut"));
        colPresence.setCellValueFactory(new PropertyValueFactory<>("presence"));
        colNote.setCellValueFactory(new PropertyValueFactory<>("note"));

        // Format Date
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        colDate.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(LocalDateTime item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.format(formatter));
            }
        });

        // Cellule Présence (Style icônes)
        colPresence.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Boolean item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(item ? "✅ Présent" : "❌ Absent");
                    setStyle(item ? "-fx-text-fill: #2ecc71; -fx-font-weight: bold;" : "-fx-text-fill: #e74c3c;");
                }
            }
        });

        // Cellule Statut (Style Badges)
        colStatut.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(StatutParticipation item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item.name());
                    String baseStyle = "-fx-background-radius: 10; -fx-padding: 3 8; -fx-alignment: center;";
                    if (item == StatutParticipation.PAYEE) {
                        setStyle(baseStyle + "-fx-background-color: #d4edda; -fx-text-fill: #155724;");
                    } else {
                        setStyle(baseStyle + "-fx-background-color: #fff3cd; -fx-text-fill: #856404;");
                    }
                }
            }
        });

        setupActionButtons();
    }

    private void loadData() {
        try {
            masterData.setAll(ps.SelectAll());
            participationTable.setItems(masterData);
        } catch (SQLException e) {
            showError("Erreur SQL", "Impossible de charger les données : " + e.getMessage());
        }
    }

    private void setupSearch() {
        FilteredList<participations> filteredData = new FilteredList<>(masterData, p -> true);
        searchField.textProperty().addListener((obs, old, newVal) -> {
            filteredData.setPredicate(p -> {
                if (newVal == null || newVal.isEmpty()) return true;
                String lowerFilter = newVal.toLowerCase();

                boolean matchUser = p.getNomUtilisateur() != null && p.getNomUtilisateur().toLowerCase().contains(lowerFilter);
                boolean matchFormation = p.getTitreFormation() != null && p.getTitreFormation().toLowerCase().contains(lowerFilter);
                boolean matchStatut = p.getStatut() != null && p.getStatut().name().toLowerCase().contains(lowerFilter);

                return matchUser || matchFormation || matchStatut;
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
                pane.setSpacing(10);
                pane.setAlignment(Pos.CENTER);
                editBtn.getStyleClass().add("button-edit"); // Possibilité d'utiliser CSS
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
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Supprimer l'inscription de : " + p.getNomUtilisateur() + " ?", ButtonType.YES, ButtonType.NO);
        confirm.setHeaderText("Confirmation de suppression");
        if (confirm.showAndWait().orElse(ButtonType.NO) == ButtonType.YES) {
            try {
                ps.deleteOne(p);
                loadData();
            } catch (SQLException e) {
                showError("Erreur", "La suppression a échoué.");
            }
        }
    }

    public void showForm(participations existing) {
        boolean isEdit = (existing != null);
        Dialog<participations> dialog = new Dialog<>();
        dialog.setTitle(isEdit ? "Modifier la participation" : "Nouvelle Inscription");

        ButtonType saveType = new ButtonType("Valider", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(15); grid.setPadding(new Insets(20));

        TextField txtFormation = new TextField(isEdit ? String.valueOf(existing.getIdFormation()) : "");
        TextField txtUtilisateur = new TextField(isEdit ? String.valueOf(existing.getIdUtilisateur()) : "");
        TextField txtNote = new TextField(isEdit ? String.valueOf(existing.getNote()) : "0.0");
        ComboBox<StatutParticipation> cbStatut = new ComboBox<>(FXCollections.observableArrayList(StatutParticipation.values()));
        cbStatut.setValue(isEdit ? existing.getStatut() : StatutParticipation.PAYEE);
        CheckBox chkPresence = new CheckBox("Présence confirmée");
        if (isEdit) chkPresence.setSelected(existing.isPresence());

        Label errLabel = new Label(); errLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 11px;");

        grid.add(new Label("ID Formation :"), 0, 0); grid.add(txtFormation, 1, 0);
        grid.add(new Label("ID Utilisateur :"), 0, 1); grid.add(txtUtilisateur, 1, 1);
        grid.add(new Label("Note Finale :"), 0, 2); grid.add(txtNote, 1, 2);
        grid.add(errLabel, 1, 3);
        grid.add(new Label("Statut :"), 0, 4); grid.add(cbStatut, 1, 4);
        grid.add(chkPresence, 1, 5);

        dialog.getDialogPane().setContent(grid);

        // Validation lors du clic sur Valider
        final Button btOk = (Button) dialog.getDialogPane().lookupButton(saveType);
        btOk.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            if (!validateNote(txtNote, errLabel)) {
                event.consume();
            }
        });

        dialog.setResultConverter(btn -> {
            if (btn == saveType) {
                try {
                    participations p = isEdit ? existing : new participations();
                    p.setIdFormation(Integer.parseInt(txtFormation.getText()));
                    p.setIdUtilisateur(Integer.parseInt(txtUtilisateur.getText()));
                    p.setNote(Float.parseFloat(txtNote.getText()));
                    p.setStatut(cbStatut.getValue());
                    p.setPresence(chkPresence.isSelected());
                    if (!isEdit) p.setDateInscription(LocalDateTime.now());
                    return p;
                } catch (NumberFormatException e) {
                    return null;
                }
            }
            return null;
        });

        dialog.showAndWait().ifPresent(p -> {
            try {
                if (isEdit) ps.updateOne(p); else ps.insertOne(p);
                loadData();
            } catch (SQLException e) {
                showError("Erreur Database", "Vérifiez l'existence des IDs et la connexion.");
            }
        });
    }

    private boolean validateNote(TextField t, Label l) {
        try {
            float n = Float.parseFloat(t.getText());
            if (n < 0 || n > 20) {
                l.setText("La note doit être entre 0 et 20");
                return false;
            }
            l.setText("");
            return true;
        } catch (NumberFormatException e) {
            l.setText("Format de note invalide");
            return false;
        }
    }

    private void showError(String title, String content) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(content);
        a.showAndWait();
    }

}