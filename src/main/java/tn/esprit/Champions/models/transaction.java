
package tn.esprit.Champions.models;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

public class transaction {

    private int idTransaction;
    private int idWalletSource;
    private int idWalletDestination;
    private double montant;
    private typeTransaction type;
    private StatutTransaction statut;
    private LocalDateTime dateTransaction;
    private int CurrencyId;


    public transaction() {
    }


    public transaction(int idTransaction, int idWalletSource, int idWalletDestination,
                       double montant, typeTransaction type, StatutTransaction statut,
                       LocalDateTime dateTransaction) {
        this.idTransaction = idTransaction;
        this.idWalletSource = idWalletSource;
        this.idWalletDestination = idWalletDestination;
        this.montant = montant;
        this.type = type;
        this.statut = statut;
        this.dateTransaction = dateTransaction;

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

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        transaction that = (transaction) o;
        return idTransaction == that.idTransaction && idWalletSource == that.idWalletSource && idWalletDestination == that.idWalletDestination && Objects.equals(montant, that.montant) && Objects.equals(type, that.type) && Objects.equals(statut, that.statut) && Objects.equals(dateTransaction, that.dateTransaction);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idTransaction, idWalletSource, idWalletDestination, montant, type, statut, dateTransaction);
    }
}
