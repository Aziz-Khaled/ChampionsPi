package tn.esprit.Champions.test;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import tn.esprit.Champions.services.BlockchainService;
import tn.esprit.Champions.utils.StripeConfig;

public class Test extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        StripeConfig.init();
        BlockchainService blockchainService = new BlockchainService();
        blockchainService.verifyBlockchain();
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/DashboardWalletClient.fxml"));

        Parent root = loader.load();
        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.setTitle("FinTech App");
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}