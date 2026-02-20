package tn.esprit.Champions.models;

import java.time.LocalDateTime;
import java.util.Objects;

public class wallet {
    private Utilisateur user;
    private int idWallet;
    private int idUser;
    private typeWallet typeWallet;   // fiat, crypto, trading
    private double solde;
    private statutWallet statut;     // bloque, actif
    private String rib;
    private LocalDateTime dateCreation;
    private LocalDateTime dateDerniereModification;

    // Constructeur vide
    public wallet() {
    }

    // Constructeur complet
    public wallet(int idWallet, int idUser, typeWallet typeWallet, double solde, statutWallet statut, String rib) {
        this.idWallet = idWallet;
        this.idUser = idUser;
        this.typeWallet = typeWallet;
        this.solde = solde;
        this.statut = statut;
        this.rib = rib;
    }

    // Constructeur sans idWallet ni solde
    public wallet(int idUser, typeWallet typeWallet, statutWallet statut, String rib) {
        this.idUser = idUser;
        this.typeWallet = typeWallet;
        this.statut = statut;
        this.rib = rib;
    }

    // Constructeur minimal (idUser non fourni)
    public wallet(typeWallet typeWallet, statutWallet statut, String rib) {
        this.typeWallet = typeWallet;
        this.statut = statut;
        this.rib = rib;
    }

    // ----- Getters et Setters -----
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

    public String getRib() {
        return rib;
    }

    public void setRib(String rib) {
        this.rib = rib;
    }

    public Utilisateur getUser() {
        return user;
    }

    public void setUser(Utilisateur user) {
        this.user = user;
    }

    public LocalDateTime getDateCreation() {
        return dateCreation;
    }

    public void setDateCreation(LocalDateTime dateCreation) {
        this.dateCreation = dateCreation;
    }

    public LocalDateTime getDateDerniereModification() {
        return dateDerniereModification;
    }

    public void setDateDerniereModification(LocalDateTime dateDerniereModification) {
        this.dateDerniereModification = dateDerniereModification;
    }

    // ----- Equals et hashCode -----
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        wallet wallet = (wallet) o;
        return idWallet == wallet.idWallet &&
                idUser == wallet.idUser &&
                Double.compare(wallet.solde, solde) == 0 &&
                Objects.equals(user, wallet.user) &&
                typeWallet == wallet.typeWallet &&
                statut == wallet.statut &&
                Objects.equals(rib, wallet.rib);
    }

    @Override
    public int hashCode() {
        return Objects.hash(user, idWallet, idUser, typeWallet, solde, statut, rib);
    }
}