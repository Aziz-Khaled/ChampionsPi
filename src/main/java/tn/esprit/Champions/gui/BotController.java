package tn.esprit.Champions.gui;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.util.Duration;
import tn.esprit.Champions.models.Status;
import tn.esprit.Champions.models.Trade;
import tn.esprit.Champions.models.TradeType;
import tn.esprit.Champions.services.MarketApiService;
import tn.esprit.Champions.services.TradeService;

import java.sql.SQLException;
import java.util.List;

public class BotController {

    @FXML private ListView<String> listPending;
    @FXML private Label lblBotStatus;

    private TradeService tradeService = new TradeService();
    private MarketApiService marketApi = new MarketApiService();
    private Timeline botEngine;

    @FXML
    public void initialize() {
        startAutonomousMonitoring();
    }

    private void startAutonomousMonitoring() {
        // Le métier tourne toutes les 5 secondes pour surveiller le marché
        botEngine = new Timeline(new KeyFrame(Duration.seconds(5), e -> {
            try {
                // 1. Charger les ordres LIMIT en attente (Trace BDD)
                List<Trade> pendingOrders = tradeService.selectPendingLimitOrders();

                // Mise à jour visuelle de la liste pour le trader
                listPending.getItems().clear();

                for (Trade order : pendingOrders) {
                    listPending.getItems().add(order.getTradeType() + " BTC @ Cible: " + order.getPrice());

                    // 2. Récupérer le prix réel via l'API
                    double currentMarketPrice = marketApi.fetchPrice("BTC");

                    // 3. LOGIQUE MÉTIER AVANCÉE
                    boolean conditionMet = false;
                    if (order.getTradeType() == TradeType.BUY && currentMarketPrice <= order.getPrice()) {
                        conditionMet = true; // Prix est descendu assez bas pour acheter
                    } else if (order.getTradeType() == TradeType.SELL && currentMarketPrice >= order.getPrice()) {
                        conditionMet = true; // Prix est monté assez haut pour vendre (Profit)
                    }

                    // 4. Exécution automatique si condition remplie
                    if (conditionMet) {
                        tradeService.finalizeLimitOrder(order.getId(), currentMarketPrice);
                        System.out.println("🤖 BOT : Ordre ID " + order.getId() + " exécuté automatiquement !");
                    }
                }

                if (pendingOrders.isEmpty()) {
                    lblBotStatus.setText("Statut : Aucun ordre en attente.");
                } else {
                    lblBotStatus.setText("Statut : Surveillance active de " + pendingOrders.size() + " ordres...");
                }

            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }));
        botEngine.setCycleCount(Animation.INDEFINITE);
        botEngine.play();
    }
}