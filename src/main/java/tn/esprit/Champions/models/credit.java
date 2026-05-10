package tn.esprit.Champions.models;
import tn.esprit.Champions.models.Utilisateur;

import java.time.LocalDateTime;

public class credit {
    private int id_credit;
    private int project_id;
    private Utilisateur borrower_id;
    private Utilisateur investisseur_id;
    private double montant;
    private String devise;
    private double taux;
    private int duree;
    private String description;
    private CreditStatus status;
    private String contrat_id;
    private LocalDateTime date_demande;
    private LocalDateTime date_contrat;

    public credit() {}

    @Override
    public String toString() {
        return "credit{" +
                "borrower_id=" + borrower_id +
                ", id_credit=" + id_credit +
                ", project_id=" + project_id +
                ", investisseur_id=" + investisseur_id +
                ", montant=" + montant +
                ", devise='" + devise + '\'' +
                ", taux=" + taux +
                ", duree=" + duree +
                ", description='" + description + '\'' +
                ", status=" + status +
                ", contrat_id='" + contrat_id + '\'' +
                ", date_demande=" + date_demande +
                ", date_contrat=" + date_contrat +
                '}';
    }


    public credit(Utilisateur borrower_id, String contrat_id, LocalDateTime date_contrat, LocalDateTime date_demande, String description, String devise, int duree, int id_credit, Utilisateur investisseur_id, double montant, int project_id, CreditStatus status, double taux) {
        this.borrower_id = borrower_id;
        this.contrat_id = contrat_id;
        this.date_contrat = date_contrat;
        this.date_demande = date_demande;
        this.description = description;
        this.devise = devise;
        this.duree = duree;
        this.id_credit = id_credit;
        this.investisseur_id = investisseur_id;
        this.montant = montant;
        this.project_id = project_id;
        this.status = status;
        this.taux = taux;
    }

    public Utilisateur getBorrower_id() {
        return borrower_id;
    }

    public String getContrat_id() {
        return contrat_id;
    }

    public LocalDateTime getDate_contrat() {
        return date_contrat;
    }

    public LocalDateTime getDate_demande() {
        return date_demande;
    }

    public String getDescription() {
        return description;
    }

    public String getDevise() {
        return devise;
    }

    public int getDuree() {
        return duree;
    }

    public int getId_credit() {
        return id_credit;
    }

    public Utilisateur getInvestisseur_id() {
        return investisseur_id;
    }

    public double getMontant() {
        return montant;
    }

    public int getProject_id() {
        return project_id;
    }

    public CreditStatus getStatus() {
        return status;
    }

    public double getTaux() {
        return taux;
    }

    public void setBorrower_id(Utilisateur borrower_id) {
        this.borrower_id = borrower_id;
    }

    public void setContrat_id(String contrat_id) {
        this.contrat_id = contrat_id;
    }

    public void setDate_contrat(LocalDateTime date_contrat) {
        this.date_contrat = date_contrat;
    }

    public void setDate_demande(LocalDateTime date_demande) {
        this.date_demande = date_demande;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setDevise(String devise) {
        this.devise = devise;
    }

    public void setDuree(int duree) {
        this.duree = duree;
    }

    public void setId_credit(int id_credit) {
        this.id_credit = id_credit;
    }

    public void setInvestisseur_id(Utilisateur investisseur_id) {
        this.investisseur_id = investisseur_id;
    }

    public void setMontant(double montant) {
        this.montant = montant;
    }

    public void setProject_id(int project_id) {
        this.project_id = project_id;
    }

    public void setStatus(CreditStatus status) {
        this.status = status;
    }

    public void setTaux(double taux) {
        this.taux = taux;
    }
}