package tn.esprit.Champions.models;

public class Negociation {
    private int id_negociation;
    private int credit_id;
    private int investor_id;
    private double montant;
    private double taux_propose;
    private String status; // <-- AJOUT DU STATUS

    public Negociation() {
    }

    // Constructeur complet mis à jour
    public Negociation(int id_negociation, int credit_id, int investor_id, double montant, double taux_propose, String status) {
        this.id_negociation = id_negociation;
        this.credit_id = credit_id;
        this.investor_id = investor_id;
        this.montant = montant;
        this.taux_propose = taux_propose;
        this.status = status;
    }

    // Getters et Setters
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    // Gardez vos autres Getters/Setters ici...
    public int getCredit_id() { return credit_id; }
    public void setCredit_id(int credit_id) { this.credit_id = credit_id; }
    public int getId_negociation() { return id_negociation; }
    public void setId_negociation(int id_negociation) { this.id_negociation = id_negociation; }
    public int getInvestor_id() { return investor_id; }
    public void setInvestor_id(int investor_id) { this.investor_id = investor_id; }
    public double getMontant() { return montant; }
    public void setMontant(double montant) { this.montant = montant; }
    public double getTaux_propose() { return taux_propose; }
    public void setTaux_propose(double taux_propose) { this.taux_propose = taux_propose; }
}