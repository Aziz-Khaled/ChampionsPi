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
        // 1. Initialisation des services requis par tes autres modules
        try {
            StripeConfig.init();
            BlockchainService blockchainService = new BlockchainService();
            blockchainService.verifyBlockchain();
        } catch (Exception e) {
            System.err.println("Erreur lors de l'initialisation des services: " + e.getMessage());
        }

        // 2. Chargement de l'interface principale (TradingDashboard)
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/DashboardWalletClient.fxml"));

        Parent root = loader.load();
        Scene scene = new Scene(root);

        // 3. Configuration de la fenêtre
        stage.setTitle("Champions Fintech - Trading Terminal");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}