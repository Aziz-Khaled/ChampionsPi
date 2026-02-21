package tn.esprit.Champions.services;

import tn.esprit.Champions.models.*;

public class TradingEngine {
    public static final double COMMISSION_RATE = 0.0015; // 0.15%

    public static double calculateTotal(double qty, double price) {
        return (qty * price) * (1 + COMMISSION_RATE);
    }

    public static double calculateSaleGain(double qty, double price) {
        return (qty * price) * (1 - COMMISSION_RATE);
    }

    public static Status resolveStatus(OrderMode mode) {
        return (mode == OrderMode.MARKET) ? Status.ACTIVE : Status.PENDING;
    }
}
