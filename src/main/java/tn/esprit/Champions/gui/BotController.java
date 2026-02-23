package tn.esprit.Champions.gui;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.util.Duration;
import tn.esprit.Champions.models.OrderMode;
import tn.esprit.Champions.models.Status;
import tn.esprit.Champions.models.Trade;
import tn.esprit.Champions.models.TradeType;
import tn.esprit.Champions.services.MarketApiService;
import tn.esprit.Champions.services.TradeService;

import java.sql.SQLException;
import java.util.List;

public class BotController {

    @FXML private ListView<String> listLogs;
    @FXML private Label lblBotStatus;

    private TradeService tradeService = new TradeService();
    private MarketApiService marketApi = new MarketApiService();
    private Timeline botTimeline;

    @FXML
    public void initialize() {
        startAutomationEngine();
    }

    private void startAutomationEngine() {
        botTimeline = new Timeline(new KeyFrame(Duration.seconds(5), event -> {
            try {
                // Utilisation de la méthode spécifique pour plus d'efficacité
                List<Trade> pendingTrades = tradeService.SelectAll().stream()
                        .filter(t -> t.getStatus() == Status.PENDING && t.getOrderMode() == OrderMode.LIMIT)
                        .toList();

                if (pendingTrades.isEmpty()) {
                    lblBotStatus.setText("Statut : En veille (Aucun ordre)");
                    return;
                }

                lblBotStatus.setText("Statut : Surveillance de " + pendingTrades.size() + " ordres...");

                for (Trade trade : pendingTrades) {
                    double currentPrice = marketApi.fetchPrice("BTC");
                    boolean shouldExecute = false;

                    if (trade.getTradeType() == TradeType.BUY && currentPrice <= trade.getPrice()) {
                        shouldExecute = true;
                    } else if (trade.getTradeType() == TradeType.SELL && currentPrice >= trade.getPrice()) {
                        shouldExecute = true;
                    }

                    if (shouldExecute) {
                        trade.setStatus(Status.COMPLETED);
                        trade.setExecutedAt(java.time.LocalDateTime.now());
                        tradeService.updateOne(trade);

                        listLogs.getItems().add(0, "✅ EXÉCUTÉ : " + trade.getTradeType() + " à " + currentPrice);
                        System.out.println("🤖 Bot : Ordre " + trade.getId() + " validé en BDD.");
                    }
                }
            } catch (SQLException e) {
                listLogs.getItems().add(0, "❌ Erreur BDD : " + e.getMessage());
            }
        }));

        botTimeline.setCycleCount(Animation.INDEFINITE);
        botTimeline.play();
    }

    // CETTE MÉTHODE DOIT S'APPELER handleStop POUR CORRESPONDRE AU FXML
    @FXML
    private void handleStop() {
        if (botTimeline != null) {
            botTimeline.stop();
            lblBotStatus.setText("Statut : Arrêté");
            listLogs.getItems().add(0, "🛑 Bot arrêté manuellement.");
        }
    }
}