package tn.esprit.Champions.models;

import java.util.Objects;

public class currency {
    private int id_currency;
    private String code;
    private String nom;
    private typeCurrency type_currency;
    private boolean is_trading;

    public currency() {
    }

    public currency(int id_currency,String code, String nom, typeCurrency type_currency, boolean is_trading) {
        this.id_currency = id_currency;
        this.code = code;
        this.nom = nom;
        this.type_currency = type_currency;
        this.is_trading = is_trading;
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

    public typeCurrency getType_currency() {
        return type_currency;
    }

    public void setType_currency(typeCurrency type_currency) {
        this.type_currency = type_currency;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public boolean isIs_trading() {
        return is_trading;
    }

    public void setIs_trading(boolean is_trading) {
        this.is_trading = is_trading;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        currency currency = (currency) o;
        return id_currency == currency.id_currency && is_trading == currency.is_trading && Objects.equals(code, currency.code) && Objects.equals(nom, currency.nom) && type_currency == currency.type_currency;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id_currency, code, nom, type_currency, is_trading);
    }

    @Override
    public String toString() {
        return "currency{" +
                "id_currency=" + id_currency +
                ", code='" + code + '\'' +
                ", nom='" + nom + '\'' +
                ", type_currency=" + type_currency +
                ", is_trading=" + is_trading +
                '}';
    }
}



