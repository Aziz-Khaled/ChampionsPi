package tn.esprit.Champions.test;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

// 1. On enlève le "extends Application" de la classe principale
public class TestFX {

    // 2. On crée une classe interne qui, elle, gère l'application
    public static class ActualApp extends Application {
        @Override
        public void start(Stage primaryStage) throws Exception {
            // Ton code de chargement FXML
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/MainDashboard.fxml"));            Scene scene = new Scene(loader.load());

            primaryStage.setTitle("Fintech - Gestion des Crédits");
            primaryStage.setScene(scene);
            primaryStage.show();
        }
    }

    // 3. Le main lance la classe interne
    public static void main(String[] args) {
        Application.launch(ActualApp.class, args);
    }
}