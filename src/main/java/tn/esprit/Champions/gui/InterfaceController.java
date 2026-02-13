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
import tn.esprit.Champions.models.formations;
import tn.esprit.Champions.models.StatutFormation;
import tn.esprit.Champions.services.FormationService;

import java.sql.SQLException;
import java.time.LocalDate;

public class InterfaceController {

    @FXML private TextField searchField;
    @FXML private Button btnAdd;
    @FXML private TableView<formations> tableView;
    @FXML private TableColumn<formations, Integer> idColumn;
    @FXML private TableColumn<formations, String> titreColumn;
    @FXML private TableColumn<formations, String> domaineColumn;
    @FXML private TableColumn<formations, Double> prixColumn;
    @FXML private TableColumn<formations, LocalDate> dateDebutColumn;
    @FXML private TableColumn<formations, StatutFormation> statutColumn;
    @FXML private TableColumn<formations, Void> actionsColumn;

    private final FormationService formationService = new FormationService();
    private final ObservableList<formations> masterData = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        initTableView();
        setupSearchAndSort();
        loadTableData();

        btnAdd.setTooltip(new Tooltip("Cliquer pour créer une nouvelle offre de formation"));
        searchField.setTooltip(new Tooltip("Filtrer par titre ou domaine en temps réel"));
    }

    private void initTableView() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("idFormation"));
        titreColumn.setCellValueFactory(new PropertyValueFactory<>("titre"));
        domaineColumn.setCellValueFactory(new PropertyValueFactory<>("domaine"));
        prixColumn.setCellValueFactory(new PropertyValueFactory<>("prix"));
        dateDebutColumn.setCellValueFactory(new PropertyValueFactory<>("dateDebut"));
        statutColumn.setCellValueFactory(new PropertyValueFactory<>("statut"));
        setupActionsColumn();
    }

    private void loadTableData() {
        try { masterData.setAll(formationService.SelectAll()); }
        catch (SQLException e) { e.printStackTrace(); }
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
            private final Button btnInscrire = new Button("Inscrire"); // NOUVEAU
            private final HBox pane = new HBox(btnEdit, btnDelete, btnInscrire);
            {
                pane.setSpacing(10); pane.setAlignment(Pos.CENTER);
                btnEdit.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-background-radius: 5;");
                btnDelete.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-background-radius: 5;");
                btnInscrire.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-background-radius: 5; -fx-font-weight: bold;");

                btnEdit.setOnAction(e -> showFormationForm(getTableView().getItems().get(getIndex())));
                btnDelete.setOnAction(e -> handleDelete(getTableView().getItems().get(getIndex())));

                // ACTION : Ouvre le formulaire de participation avec l'ID pré-rempli
                btnInscrire.setOnAction(e -> {
                    formations selected = getTableView().getItems().get(getIndex());
                    ParticipationController pc = new ParticipationController();
                    pc.showForm(null, selected.getIdFormation());
                });
            }
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : pane);
            }
        });
    }

    @FXML private void handleAdding() { showFormationForm(null); }

    private void handleDelete(formations f) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Voulez-vous vraiment supprimer la formation : " + f.getTitre() + " ?", ButtonType.YES, ButtonType.NO);
        alert.setTitle("Confirmation de suppression");
        if (alert.showAndWait().get() == ButtonType.YES) {
            try { formationService.deleteOne(f); loadTableData(); }
            catch (SQLException e) { showError("Erreur", "Impossible de supprimer."); }
        }
    }

    private void showFormationForm(formations existing) {
        boolean isEdit = (existing != null);
        Dialog<formations> dialog = new Dialog<>();
        dialog.setTitle(isEdit ? "Mise à jour" : "Nouveau Programme");

        DialogPane dialogPane = dialog.getDialogPane();
        ButtonType saveBtnType = new ButtonType(isEdit ? "Mettre à jour" : "Enregistrer", ButtonBar.ButtonData.OK_DONE);
        dialogPane.getButtonTypes().addAll(saveBtnType, ButtonType.CANCEL);

        VBox header = new VBox();
        header.setStyle("-fx-background-color: #1a2a3a; -fx-padding: 20;");
        Label title = new Label(isEdit ? "MODIFIER LA FORMATION" : "AJOUTER UNE FORMATION");
        title.setStyle("-fx-text-fill: #00d4ff; -fx-font-size: 18; -fx-font-weight: bold;");
        header.getChildren().add(title);
        dialogPane.setHeader(header);

        GridPane grid = new GridPane();
        grid.setHgap(20); grid.setVgap(5); grid.setPadding(new Insets(20, 40, 20, 40));

        TextField txtTitre = new TextField();
        Label errTitre = new Label(); errTitre.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 10;");
        TextField txtDomaine = new TextField();
        Label errDomaine = new Label(); errDomaine.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 10;");
        TextField txtPrix = new TextField();
        Label errPrix = new Label(); errPrix.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 10;");
        DatePicker dpDebut = new DatePicker(LocalDate.now());
        Label errDate = new Label(); errDate.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 10;");

        ComboBox<StatutFormation> cbStatut = new ComboBox<>(FXCollections.observableArrayList(StatutFormation.values()));
        cbStatut.setValue(StatutFormation.OUVERTE); cbStatut.setMaxWidth(Double.MAX_VALUE);

        if (isEdit) {
            txtTitre.setText(existing.getTitre()); txtDomaine.setText(existing.getDomaine());
            txtPrix.setText(String.valueOf(existing.getPrix())); dpDebut.setValue(existing.getDateDebut());
            cbStatut.setValue(existing.getStatut());
        }

        grid.add(createLabel("Titre du programme *"), 0, 0); grid.add(txtTitre, 0, 1); grid.add(errTitre, 0, 2);
        grid.add(createLabel("Domaine d'études *"), 1, 0); grid.add(txtDomaine, 1, 1); grid.add(errDomaine, 1, 2);
        grid.add(createLabel("Tarif (DT) *"), 0, 3); grid.add(txtPrix, 0, 4); grid.add(errPrix, 0, 5);
        grid.add(createLabel("Date de lancement *"), 1, 3); grid.add(dpDebut, 1, 4); grid.add(errDate, 1, 5);
        grid.add(createLabel("Statut de visibilité"), 0, 6, 2, 1); grid.add(cbStatut, 0, 7, 2, 1);

        dialogPane.setContent(grid);
        Node saveBtn = dialogPane.lookupButton(saveBtnType);
        saveBtn.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");

        saveBtn.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            if (!(validateTitre(txtTitre, errTitre) & validateDomaine(txtDomaine, errDomaine) &
                    validatePrix(txtPrix, errPrix) & validateDate(dpDebut, errDate))) {
                event.consume();
            }
        });

        dialog.setResultConverter(btn -> {
            if (btn == saveBtnType) {
                formations f = isEdit ? existing : new formations();
                f.setTitre(txtTitre.getText()); f.setDomaine(txtDomaine.getText());
                f.setPrix(Double.parseDouble(txtPrix.getText())); f.setDateDebut(dpDebut.getValue());
                f.setStatut(cbStatut.getValue());
                if(!isEdit) {
                    f.setDescription("Formation Fintech");
                    f.setDateFin(f.getDateDebut().plusDays(30));
                    f.setCapaciteMax(20);
                }
                return f;
            }
            return null;
        });

        dialog.showAndWait().ifPresent(f -> {
            try { if (isEdit) formationService.updateOne(f); else formationService.insertOne(f); loadTableData(); }
            catch (SQLException e) { showError("Erreur BDD", "Une erreur est survenue lors de l'enregistrement."); }
        });
    }

    private boolean validateTitre(TextField t, Label l) {
        if (t.getText().trim().length() < 3) return applyError(t, l, "Minimum 3 caractères requis");
        return applySuccess(t, l);
    }

    private boolean validateDomaine(TextField t, Label l) {
        if (t.getText().trim().isEmpty()) return applyError(t, l, "Le domaine est requis");
        return applySuccess(t, l);
    }

    private boolean validatePrix(TextField t, Label l) {
        try {
            double p = Double.parseDouble(t.getText());
            if (p <= 0) return applyError(t, l, "Le prix doit être positif");
            return applySuccess(t, l);
        } catch (Exception e) { return applyError(t, l, "Format numérique attendu"); }
    }

    private boolean validateDate(DatePicker d, Label l) {
        if (d.getValue() == null || d.getValue().isBefore(LocalDate.now()))
            return applyError(d, l, "Date invalide ou passée");
        return applySuccess(d, l);
    }

    private boolean applyError(Control c, Label l, String m) {
        c.setStyle("-fx-border-color: #e74c3c; -fx-border-radius: 5; -fx-border-width: 2;");
        l.setText(m); return false;
    }

    private boolean applySuccess(Control c, Label l) {
        c.setStyle("-fx-border-color: #27ae60; -fx-border-radius: 5; -fx-border-width: 2;");
        l.setText(""); return true;
    }

    private Label createLabel(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-text-fill: #34495e; -fx-font-weight: bold; -fx-font-size: 12;");
        return l;
    }

    private void showError(String t, String c) {
        Alert a = new Alert(Alert.AlertType.ERROR, c);
        a.setTitle(t); a.showAndWait();
    }
}