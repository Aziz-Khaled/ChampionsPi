package tn.esprit.Champions.models;

import java.util.Objects;

public class wallet {
    private Utilisateur user;
    private int idWallet;
    private int idUser;
    private typeWallet typeWallet;   // fiat, crypto, trading
    private double solde;
    private statutWallet statut;       // bloque, actif


    public wallet() {
    }


    public wallet(int idWallet, int idUser, typeWallet typeWallet, double solde, statutWallet statut) {
        this.idWallet = idWallet;
        this.idUser = idUser;
        this.typeWallet = typeWallet;
        this.solde = solde;
        this.statut = statut;
    }

    public wallet(int idUser,typeWallet typeWallet, statutWallet statut) {
        this.typeWallet = typeWallet;
        this.statut = statut;
        this.idUser = idUser;
    }
    public wallet(typeWallet typeWallet, statutWallet statut) {
        this.typeWallet = typeWallet;
        this.statut = statut;
        this.idUser = idUser;
    }


    public int getIdWallet() {
        return idWallet;
    }

    public void setIdWallet(int idWallet) {
        this.idWallet = idWallet;
    }

    public int getIdUser() {
        return idUser;
    }

    public void setIdUser(int idUser) {
        this.idUser = idUser;
    }

    public typeWallet getTypeWallet() {
        return typeWallet;
    }

    public void setTypeWallet(typeWallet typeWallet) {
        this.typeWallet = typeWallet;
    }


    public double getSolde() {
        return solde;
    }

    public void setSolde(double solde) {
        this.solde = solde;
    }

    public statutWallet getStatut() {
        return statut;
    }

    public void setStatut(statutWallet statut) {
        this.statut = statut;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        wallet wallet = (wallet) o;
        return idWallet == wallet.idWallet && idUser == wallet.idUser && Double.compare(solde, wallet.solde) == 0 && Objects.equals(typeWallet, wallet.typeWallet) &&  Objects.equals(statut, wallet.statut);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idWallet, idUser, typeWallet, solde, statut);
    }
}
