package tn.esprit.Champions.services;

public class RiskManager {
    public static double getStopLoss(double price) { return price * 0.95; } // -5%
    public static double getTakeProfit(double price) { return price * 1.10; } // +10%

    public static double calculateROI(double currentPrice, double entryPrice, double qty) {
        if (entryPrice <= 0) return 0;
        return ((currentPrice - entryPrice) / entryPrice) * 100;
    }
}