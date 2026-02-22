package tn.esprit.Champions.services;

public class RiskManager {
    // Calcule le Profit & Loss (PnL) en pourcentage
    public static double calculatePnL(double currentPrice, double entryPrice) {
        if (entryPrice <= 0) return 0.0;
        return ((currentPrice - entryPrice) / entryPrice) * 100;
    }

    // Retourne la couleur hexadécimale selon la performance
    public static String getPnLColor(double pnl) {
        return (pnl >= 0) ? "#00ff88" : "#f23645"; // Vert si profit, Rouge si perte
    }
}