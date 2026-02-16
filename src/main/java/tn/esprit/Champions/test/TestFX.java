package tn.esprit.Champions.test;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

// 1. On enlève le "extends Application" de la classe principale
public class TestFX {

    // 2. On crée une classe interne qui, elle, gère l'application
    public static class ActualApp extends Application {

        @Override
        public void start(Stage stage) throws Exception {
            Parent root = FXMLLoader.load(getClass().getResource("/MainDashboard.fxml"));
            Scene scene = new Scene(root);

            // ✅ Charger le CSS une seule fois pour toute la scène
            String css = getClass().getResource("/css/style.css").toExternalForm();
            scene.getStylesheets().add(css);

            stage.setScene(scene);
            stage.show();
        }
    }

    // 3. Le main lance la classe interne
    public static void main(String[] args) {
        Application.launch(ActualApp.class, args);
    }
}