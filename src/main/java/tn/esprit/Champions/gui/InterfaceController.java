package tn.esprit.Champions.gui;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.chart.PieChart;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import tn.esprit.Champions.models.formations;
import tn.esprit.Champions.models.StatutFormation;
import tn.esprit.Champions.services.FormationService;
import tn.esprit.Champions.services.AiFormationService; // Import du nouveau service

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.stream.Collectors;

public class InterfaceController {

    @FXML private TextField searchField;
    @FXML private Button btnAdd;
    @FXML private TableView<formations> tableView;
    @FXML private TableColumn<formations, String> titreColumn;
    @FXML private TableColumn<formations, String> domaineColumn;
    @FXML private TableColumn<formations, Double> prixColumn;
    @FXML private TableColumn<formations, LocalDate> dateDebutColumn;
    @FXML private TableColumn<formations, StatutFormation> statutColumn;
    @FXML private TableColumn<formations, Void> actionsColumn;

    private final FormationService formationService = new FormationService();
    // Utilisation du nouveau AiFormationService (Option A)
    private final AiFormationService AiFormationService = new AiFormationService();
    private final ObservableList<formations> masterData = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        initTableView();
        setupSearchAndSort();
        loadTableData();

        btnAdd.setTooltip(new Tooltip("Créer une nouvelle formation"));
        searchField.setPromptText("Rechercher un titre ou un domaine...");
    }

    private void initTableView() {
        titreColumn.setCellValueFactory(new PropertyValueFactory<>("titre"));
        domaineColumn.setCellValueFactory(new PropertyValueFactory<>("domaine"));
        prixColumn.setCellValueFactory(new PropertyValueFactory<>("prix"));
        dateDebutColumn.setCellValueFactory(new PropertyValueFactory<>("dateDebut"));
        statutColumn.setCellValueFactory(new PropertyValueFactory<>("statut"));
        setupActionsColumn();
    }

    private void loadTableData() {
        try {
            masterData.setAll(formationService.SelectAll());
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void showStats() {
        PieChart pieChart = new PieChart();
        pieChart.setTitle("Répartition par Domaine");

        masterData.stream()
                .collect(Collectors.groupingBy(formations::getDomaine, Collectors.counting()))
                .forEach((domaine, count) -> {
                    pieChart.getData().add(new PieChart.Data(domaine + " (" + count + ")", count));
                });

        Stage stage = new Stage();
        stage.setTitle("Statistiques Formations");
        stage.setScene(new Scene(new StackPane(pieChart), 600, 450));
        stage.show();
    }

    private void setupSearchAndSort() {
        FilteredList<formations> filteredData = new FilteredList<>(masterData, p -> true);
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            filteredData.setPredicate(f -> {
                if (newVal == null || newVal.isEmpty()) return true;
                String filter = newVal.toLowerCase();
                return f.getTitre().toLowerCase().contains(filter) || f.getDomaine().toLowerCase().contains(filter);
            });
        });
        SortedList<formations> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(tableView.comparatorProperty());
        tableView.setItems(sortedData);
    }

    private void setupActionsColumn() {
        actionsColumn.setCellFactory(param -> new TableCell<>() {
            private final Button btnEdit = new Button("Modifier");
            private final Button btnDelete = new Button("Supprimer");
            private final HBox pane = new HBox(btnEdit, btnDelete);
            {
                pane.setSpacing(10); pane.setAlignment(Pos.CENTER);
                btnEdit.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-background-radius: 5;");
                btnDelete.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-background-radius: 5;");

                btnEdit.setOnAction(e -> showFormationForm(getTableView().getItems().get(getIndex())));
                btnDelete.setOnAction(e -> handleDelete(getTableView().getItems().get(getIndex())));
            }
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : pane);
            }
        });
    }

    @FXML private void handleAdding() { showFormationForm(null); }

    private void handleDelete(formations f) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Supprimer : " + f.getTitre() + " ?", ButtonType.YES, ButtonType.NO);
        if (alert.showAndWait().get() == ButtonType.YES) {
            try {
                formationService.deleteOne(f);
                loadTableData();
            } catch (SQLException e) {
                e.printStackTrace();
                showError("Erreur", "Suppression impossible.");
            }
        }
    }

    private void showFormationForm(formations existing) {
        boolean isEdit = (existing != null);
        Dialog<formations> dialog = new Dialog<>();
        dialog.setTitle(isEdit ? "Modification" : "Nouveau Programme");

        ButtonType saveBtnType = new ButtonType(isEdit ? "Mettre à jour" : "Enregistrer", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtnType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(20); grid.setVgap(10); grid.setPadding(new Insets(20));

        TextField txtTitre = new TextField(isEdit ? existing.getTitre() : "");
        TextField txtDomaine = new TextField(isEdit ? existing.getDomaine() : "");
        TextField txtPrix = new TextField(isEdit ? String.valueOf(existing.getPrix()) : "");
        DatePicker dpDebut = new DatePicker(isEdit ? existing.getDateDebut() : LocalDate.now());
        TextArea txtDesc = new TextArea(isEdit ? existing.getDescription() : "");
        txtDesc.setPrefRowCount(3);

        // --- BOUTON GEMINI IA ---
        Button btnIA = new Button("✨ Générer avec Gemini");
        btnIA.setStyle("-fx-background-color: #8e44ad; -fx-text-fill: white; -fx-font-weight: bold;");

        btnIA.setOnAction(e -> {
            if(txtTitre.getText().trim().isEmpty()) {
                showError("Info manquante", "Veuillez saisir un titre pour guider Gemini.");
                return;
            }
            btnIA.setDisable(true);
            btnIA.setText("⏳ IA en cours...");

            // Appel au nouveau AiFormationService
            String prompt = "Rédige une description de 40 mots pour une formation nommée : " + txtTitre.getText();
            try {
                String result = AiFormationService.askGemini(prompt);
                txtDesc.setText(result);
            } catch (Exception ex) {
                txtDesc.setText("Erreur lors de la génération.");
            } finally {
                btnIA.setDisable(false);
                btnIA.setText("✨ Générer avec Gemini");
            }
        });

        ComboBox<StatutFormation> cbStatut = new ComboBox<>(FXCollections.observableArrayList(StatutFormation.values()));
        cbStatut.setValue(isEdit ? existing.getStatut() : StatutFormation.OUVERTE);

        grid.add(new Label("Titre:"), 0, 0); grid.add(txtTitre, 1, 0);
        grid.add(new Label("Domaine:"), 0, 1); grid.add(txtDomaine, 1, 1);
        grid.add(new Label("Prix:"), 0, 2); grid.add(txtPrix, 1, 2);
        grid.add(new Label("Date:"), 0, 3); grid.add(dpDebut, 1, 3);
        grid.add(new Label("Description:"), 0, 4); grid.add(txtDesc, 1, 4);
        grid.add(btnIA, 1, 5);
        grid.add(new Label("Statut:"), 0, 6); grid.add(cbStatut, 1, 6);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(btn -> {
            if (btn == saveBtnType) {
                try {
                    formations f = isEdit ? existing : new formations();
                    f.setTitre(txtTitre.getText());
                    f.setDomaine(txtDomaine.getText());
                    f.setPrix(Double.parseDouble(txtPrix.getText()));
                    f.setDateDebut(dpDebut.getValue());
                    f.setStatut(cbStatut.getValue());
                    f.setDescription(txtDesc.getText());
                    return f;
                } catch (NumberFormatException e) {
                    showError("Erreur de format", "Le prix doit être un nombre valide.");
                    return null;
                }
            }
            return null;
        });

        dialog.showAndWait().ifPresent(f -> {
            try {
                if (isEdit) {
                    formationService.updateOne(f);
                } else {
                    if (formationService.existsByTitre(f.getTitre())) {
                        showError("Doublon", "Une formation avec ce titre existe déjà.");
                        return;
                    }
                    formationService.insertOne(f);
                }
                loadTableData();
            } catch (SQLException e) {
                e.printStackTrace();
                showError("Erreur", "L'enregistrement a échoué : " + e.getMessage());
            }
        });
    }

    private void showError(String t, String c) {
        Alert a = new Alert(Alert.AlertType.ERROR, c);
        a.setTitle(t); a.showAndWait();
    }
}