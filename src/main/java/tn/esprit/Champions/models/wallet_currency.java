package tn.esprit.Champions.models;

import java.util.Objects;

public class wallet_currency {
    private int id_wallet_currency;
    private int id_wallet;
    private int id_currency;
    private String nom_currency;
    private double solde;

    public wallet_currency() {
    }

    public wallet_currency(int id_wallet_currency, int id_wallet, int id_currency, double solde, String nom_currency) {
        this.id_wallet_currency = id_wallet_currency;
        this.id_wallet = id_wallet;
        this.id_currency = id_currency;
        this.solde = solde;
        this.nom_currency = nom_currency;
    }

    public int getId_wallet_currency() {
        return id_wallet_currency;
    }

    public void setId_wallet_currency(int id_wallet_currency) {
        this.id_wallet_currency = id_wallet_currency;
    }

    public int getId_wallet() {
        return id_wallet;
    }

    public void setId_wallet(int id_wallet) {
        this.id_wallet = id_wallet;
    }

    public int getId_currency() {
        return id_currency;
    }

    public void setId_currency(int id_currency) {
        this.id_currency = id_currency;
    }

    public double getSolde() {
        return solde;
    }

    public void setSolde(double solde) {
        this.solde = solde;
    }

    public String getNom_currency() {
        return nom_currency;
    }

    public void setNom_currency(String nom_currency) {
        this.nom_currency = nom_currency;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        wallet_currency that = (wallet_currency) o;
        return id_wallet_currency == that.id_wallet_currency && id_wallet == that.id_wallet && id_currency == that.id_currency && Double.compare(solde, that.solde) == 0 && nom_currency.equals(that.nom_currency);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id_wallet_currency, id_wallet, id_currency, solde, nom_currency);
    }
}