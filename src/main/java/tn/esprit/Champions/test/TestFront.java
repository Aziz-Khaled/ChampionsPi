package tn.esprit.Champions.test;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class TestFront extends Application {
    @Override
    public void start(Stage primaryStage) throws Exception {
        // Charge le Dashboard Étudiant
        //Parent root = FXMLLoader.load(getClass().getResource("/tn/esprit.Champions.gui/student_dashboard.fxml"));
        // Option A : Si le fichier est exactement dans ce dossier de ressources
        Parent root = FXMLLoader.load(getClass().getResource("/student_dashboard.fxml"));

        primaryStage.setTitle("Champions - Espace Étudiant");
        primaryStage.setScene(new Scene(root));
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}