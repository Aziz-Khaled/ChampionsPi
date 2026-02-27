package tn.esprit.Champions.services;

public class PositionSizer {

    /**
     * Calcule la quantité recommandée en fonction du risque.
     * @param totalBalance Le solde total du portefeuille (ex: 1000 USDT)
     * @param riskPercentage Le % du capital qu'on accepte de perdre (ex: 1% soit 0.01)
     * @param entryPrice Prix d'achat
     * @param stopLossPrice Prix auquel on coupera la position en cas de baisse
     * @return La quantité (qty) à acheter
     */
    public static double calculateRecommendedQty(double totalBalance, double riskPercentage, double entryPrice, double stopLossPrice) {
        if (entryPrice <= stopLossPrice) return 0.0;

        // Montant maximal que l'on accepte de perdre en cash
        double amountToRisk = totalBalance * riskPercentage;

        // Perte par unité (différence de prix)
        double riskPerUnit = entryPrice - stopLossPrice;

        // Quantité = Risque Total / Risque par unité
        return amountToRisk / riskPerUnit;
    }
}