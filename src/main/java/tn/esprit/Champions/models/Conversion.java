package tn.esprit.Champions.models;

import java.sql.Timestamp;

public class Conversion {

    private int idConversion;

    private Double amountFrom;
    private Integer currencyFrom;

    private Double amountTo;
    private Integer currencyTo;

    private Double exchangeRate;

    private Timestamp createdAt;

    // Getters & Setters

    public int getIdConversion() {
        return idConversion;
    }

    public void setIdConversion(int idConversion) {
        this.idConversion = idConversion;
    }

    public Double getAmountFrom() {
        return amountFrom;
    }

    public void setAmountFrom(Double amountFrom) {
        this.amountFrom = amountFrom;
    }

    public Integer getCurrencyFrom() {
        return currencyFrom;
    }

    public void setCurrencyFrom(Integer currencyFrom) {
        this.currencyFrom = currencyFrom;
    }

    public Double getAmountTo() {
        return amountTo;
    }

    public void setAmountTo(Double amountTo) {
        this.amountTo = amountTo;
    }

    public Integer getCurrencyTo() {
        return currencyTo;
    }

    public void setCurrencyTo(Integer currencyTo) {
        this.currencyTo = currencyTo;
    }

    public Double getExchangeRate() {
        return exchangeRate;
    }

    public void setExchangeRate(Double exchangeRate) {
        this.exchangeRate = exchangeRate;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }
}