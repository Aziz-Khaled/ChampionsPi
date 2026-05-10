package tn.esprit.Champions.test;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import tn.esprit.Champions.services.BlockchainService;
import tn.esprit.Champions.utils.DbConnection;
import tn.esprit.Champions.utils.StripeConfig;

public class Test extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/invest/Marketplace.fxml"));
        StripeConfig.init();
        BlockchainService blockchainService = new BlockchainService();
        blockchainService.verifyBlockchain();
        Scene scene = new Scene(loader.load());
        stage.setTitle("Fintech App");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {

        DbConnection.getInstance() ;

        System.setProperty("prism.order", "sw");
        System.setProperty("prism.vsync", "false");
        launch(args);
    }
}