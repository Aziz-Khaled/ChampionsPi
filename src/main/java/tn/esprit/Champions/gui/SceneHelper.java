package tn.esprit.Champions.gui;

import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.TranslateTransition;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.scene.Node;

import java.io.IOException;

public class SceneHelper {

    public static void transitionTo(String fxmlPath, Node triggerNode, String title) {
        try {
            Stage stage = (Stage) triggerNode.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(SceneHelper.class.getResource(fxmlPath));
            Parent nextRoot = loader.load();


            nextRoot.setOpacity(0);
            nextRoot.setTranslateX(15);


            Scene scene = stage.getScene();
            if (scene == null) {
                scene = new Scene(nextRoot);
                stage.setScene(scene);
            } else {
                scene.setRoot(nextRoot);
            }
            stage.setTitle(title);


            FadeTransition fadeIn = new FadeTransition(Duration.millis(400), nextRoot);
            fadeIn.setFromValue(0);
            fadeIn.setToValue(1);

            TranslateTransition slideIn = new TranslateTransition(Duration.millis(400), nextRoot);
            slideIn.setFromX(15);
            slideIn.setToX(0);

            ParallelTransition entrance = new ParallelTransition(fadeIn, slideIn);
            entrance.play();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}