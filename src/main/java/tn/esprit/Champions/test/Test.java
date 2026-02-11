package tn.esprit.Champions.test;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import tn.esprit.Champions.utils.DbConnection;

public class Test extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/LandingPage.fxml"));

        Scene scene = new Scene(loader.load());
        stage.setTitle("Fintech App");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {

        DbConnection.getInstance() ;

        launch(args);
    }
}

