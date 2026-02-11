package tn.esprit.Champions.models;

import java.util.Objects;

public class currency {
    private int id_currency;
    private String nom;
    private typeWallet type_currency;

    public currency() {
    }

    public currency(int id_currency, String nom, typeWallet type_currency) {
        this.id_currency = id_currency;
        this.nom = nom;
        this.type_currency = type_currency;
    }

    public int getId_currency() {
        return id_currency;
    }

    public void setId_currency(int id_currency) {
        this.id_currency = id_currency;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public typeWallet getType_currency() {
        return type_currency;
    }

    public void setType_currency(typeWallet type_currency) {
        this.type_currency = type_currency;
    }

    @Override
    public String toString() {
        return "currency{" +
                "id_currency=" + id_currency +
                ", nom='" + nom + '\'' +
                ", currency=" + type_currency +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        currency currency1 = (currency) o;
        return id_currency == currency1.id_currency && Objects.equals(nom, currency1.nom) && type_currency == currency1.type_currency;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id_currency, nom, type_currency);
    }
}
