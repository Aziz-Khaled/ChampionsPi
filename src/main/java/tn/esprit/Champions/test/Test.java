/*package tn.esprit.Champions.test;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import tn.esprit.Champions.utils.DbConnection;

public class Test extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        // Initialisation de la connexion DB au démarrage
        DbConnection.getInstance();

        // Chargement de la page de connexion (Module Utilisateur)
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/LoginPage.fxml"));

        Scene scene = new Scene(loader.load());
        stage.setTitle("Fintech App - Champions");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}*/