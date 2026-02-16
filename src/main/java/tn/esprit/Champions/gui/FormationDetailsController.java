package tn.esprit.Champions.gui;

import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.util.Duration;
import tn.esprit.Champions.models.formations;
import tn.esprit.Champions.models.participations;
import tn.esprit.Champions.models.StatutParticipation;
import tn.esprit.Champions.services.ParticipationService;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public class FormationDetailsController {

    @FXML private Label lblTitre, lblDomaine, lblPrix, lblDuree;
    @FXML private Text txtDescription;
    @FXML private VBox rootContainer;
    @FXML private Button btnParticiper;

    private formations selectedFormation;
    private final ParticipationService ps = new ParticipationService();

    @FXML
    public void initialize() {
        // Animation d'entrée douce
        if (rootContainer != null) {
            FadeTransition ft = new FadeTransition(Duration.millis(800), rootContainer);
            ft.setFromValue(0);
            ft.setToValue(1);
            ft.play();
        }
    }

    public void setFormation(formations f) {
        this.selectedFormation = f;

        // Remplissage des données avec style
        lblTitre.setText(f.getTitre());
        lblDomaine.setText(f.getDomaine() != null ? f.getDomaine().toUpperCase() : "FORMATION");
        txtDescription.setText(f.getDescription());
        lblPrix.setText(f.getPrix() + " DT");

        // Calcul de la durée amélioré
        if (f.getDateDebut() != null && f.getDateFin() != null) {
            long days = ChronoUnit.DAYS.between(f.getDateDebut(), f.getDateFin());
            if (days >= 7) {
                lblDuree.setText("⏳ " + (days / 7) + " semaines");
            } else {
                lblDuree.setText("⏳ " + days + " jours (Intensif)");
            }
        } else {
            lblDuree.setText("⏳ Durée flexible");
        }
    }

    @FXML
    private void handleParticipation(ActionEvent event) {
        // Animation de clic sur le bouton
        ScaleTransition st = new ScaleTransition(Duration.millis(100), btnParticiper);
        st.setFromX(1.0); st.setFromY(1.0);
        st.setToX(0.95); st.setToY(0.95);
        st.setAutoReverse(true);
        st.setCycleCount(2);
        st.play();

        try {
            participations p = new participations();
            p.setIdFormation(selectedFormation.getIdFormation());
            p.setIdUtilisateur(1); // À lier à votre session utilisateur
            p.setDateInscription(LocalDateTime.now());
            p.setStatut(StatutParticipation.PAYEE);
            p.setPresence(false);
            p.setNote(0f);

            ps.insertOne(p);

            showStyledAlert(
                    Alert.AlertType.INFORMATION,
                    "Succès !",
                    "Félicitations ! Votre inscription à \"" + selectedFormation.getTitre() + "\" est confirmée."
            );

        } catch (SQLException e) {
            showStyledAlert(
                    Alert.AlertType.ERROR,
                    "Oups !",
                    "Il semble que vous soyez déjà inscrit à cette formation."
            );
        }
    }

    @FXML
    private void handleBack(ActionEvent event) {
        try {
            URL resource = getClass().getResource("/student_dashboard.fxml");
            if (resource == null) return;

            Parent root = FXMLLoader.load(resource);
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            Scene scene = new Scene(root);
            stage.setScene(scene);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void showStyledAlert(Alert.AlertType type, String header, String content) {
        Alert alert = new Alert(type);
        alert.setTitle("ChampionPi");
        alert.setHeaderText(header);
        alert.setContentText(content);

        DialogPane dialogPane = alert.getDialogPane();
        URL cssResource = getClass().getResource("/style.css");
        if (cssResource != null) {
            dialogPane.getStylesheets().add(cssResource.toExternalForm());
            dialogPane.getStyleClass().add("custom-dialog");
        }
        alert.showAndWait();
    }
}