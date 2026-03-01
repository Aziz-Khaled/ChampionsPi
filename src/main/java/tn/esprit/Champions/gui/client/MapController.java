package tn.esprit.Champions.gui.client;

import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import netscape.javascript.JSObject;

import java.net.URL;

public class MapController {

    @FXML
    private WebView mapWebView;
    @FXML
    private Label selectedAddressLabel;
    @FXML
    private Button confirmButton;

    private String selectedAddress;
    private boolean confirmed = false;

    @FXML
    public void initialize() {
        WebEngine engine = mapWebView.getEngine();

        // Handle JS to Java communication
        engine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {
                JSObject window = (JSObject) engine.executeScript("window");
                window.setMember("javaConnector", new MapBridge());
            }
        });

        URL url = getClass().getResource("/html/map.html");
        if (url != null) {
            engine.load(url.toExternalForm());
        } else {
            selectedAddressLabel.setText("Erreur: map.html introuvable.");
        }
    }

    // Bridge class must be public for JS access
    public class MapBridge {
        public void onAddressSelected(String address) {
            javafx.application.Platform.runLater(() -> {
                selectedAddress = address;
                selectedAddressLabel.setText(address);
                confirmButton.setDisable(false);
            });
        }
    }

    @FXML
    private void handleConfirm() {
        this.confirmed = true;
        closeStage();
    }

    @FXML
    private void handleCancel() {
        this.confirmed = false;
        closeStage();
    }

    private void closeStage() {
        Stage stage = (Stage) mapWebView.getScene().getWindow();
        stage.close();
    }

    public String getSelectedAddress() {
        return selectedAddress;
    }

    public boolean isConfirmed() {
        return confirmed;
    }
}
