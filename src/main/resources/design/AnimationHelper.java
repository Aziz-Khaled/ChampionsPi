package tn.esprit.Champions.utils;

import javafx.animation.*;
import javafx.scene.Node;
import javafx.util.Duration;

public class AnimationHelper {

    // Transition de page fluide (Slide + Fade)
    public static void pageTransition(Node node) {
        node.setOpacity(0);
        node.setTranslateY(15);

        FadeTransition ft = new FadeTransition(Duration.millis(500), node);
        ft.setToValue(1.0);

        TranslateTransition tt = new TranslateTransition(Duration.millis(500), node);
        tt.setToY(0);

        new ParallelTransition(ft, tt).play();
    }

    // Effet de pulsation pour les stats
    public static void pulse(Node node) {
        ScaleTransition st = new ScaleTransition(Duration.millis(1000), node);
        st.setFromX(1.0); st.setFromY(1.0);
        st.setToX(1.05); st.setToY(1.05);
        st.setCycleCount(Animation.INDEFINITE);
        st.setAutoReverse(true);
        st.play();
    }
}