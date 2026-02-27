package tn.esprit.Champions.models;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

public class transaction {

    private int idTransaction;
    private int idWalletSource;
    private int id_card;
    private int idWalletDestination;
    private double montant;
    private typeTransaction type;
    private StatutTransaction statut;
    private LocalDateTime dateTransaction;
    private int CurrencyId;
    private int id_conversion;
    private int id_trade;



    public transaction() {
    }


    public transaction(int idTransaction, int idWalletSource, int idWalletDestination,int id_card,
                       double montant, typeTransaction type, StatutTransaction statut,
                       LocalDateTime dateTransaction, int id_conversion) {
        this.idTransaction = idTransaction;
        this.idWalletSource = idWalletSource;
        this.idWalletDestination = idWalletDestination;
        this.montant = montant;
        this.type = type;
        this.statut = statut;
        this.dateTransaction = dateTransaction;
        this.id_card = id_card;
        this.id_conversion = id_conversion;

    }


    public int getIdTransaction() {
        return idTransaction;
    }

    public void setIdTransaction(int idTransaction) {
        this.idTransaction = idTransaction;
    }

    public int getIdWalletSource() {
        return idWalletSource;
    }

    public void setIdWalletSource(int idWalletSource) {
        this.idWalletSource = idWalletSource;
    }

    public int getIdWalletDestination() {
        return idWalletDestination;
    }

    public void setIdWalletDestination(int idWalletDestination) {
        this.idWalletDestination = idWalletDestination;
    }

    public double getMontant() {
        return montant;
    }

    public void setMontant(double montant) {
        this.montant = montant;
    }

    public typeTransaction getType() {
        return type;
    }

    public void setType(typeTransaction type) {
        this.type = type;
    }

    public StatutTransaction getStatut() {
        return statut;
    }

    public void setStatut(StatutTransaction statut) {
        this.statut = statut;
    }

    public LocalDateTime getDateTransaction() {
        return dateTransaction;
    }

    public void setDateTransaction(LocalDateTime dateTransaction) {
        this.dateTransaction = dateTransaction;
    }

    public int getCurrencyId() {
        return CurrencyId;
    }

    public void setCurrencyId(int currencyId) {
        CurrencyId = currencyId;
    }

    public int getId_card() {
        return id_card;
    }

    public void setId_card(int id_card) {
        this.id_card = id_card;
    }

    public int getId_conversion() {
        return id_conversion;
    }

    public void setId_conversion(int id_conversion) {
        this.id_conversion = id_conversion;
    }

    public int getId_trade() {
        return id_trade;
    }

    public void setId_trade(int id_trade) {
        this.id_trade = id_trade;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        transaction that = (transaction) o;
        return idTransaction == that.idTransaction && idWalletSource == that.idWalletSource && id_card == that.id_card && idWalletDestination == that.idWalletDestination && Double.compare(montant, that.montant) == 0 && CurrencyId == that.CurrencyId && id_conversion == that.id_conversion && id_trade == that.id_trade && type == that.type && statut == that.statut && Objects.equals(dateTransaction, that.dateTransaction);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idTransaction, idWalletSource, id_card, idWalletDestination, montant, type, statut, dateTransaction, CurrencyId, id_conversion, id_trade);
    }
}