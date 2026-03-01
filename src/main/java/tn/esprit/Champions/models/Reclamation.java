package tn.esprit.Champions.models;

import java.time.LocalDateTime;

public class Reclamation {

    private int idRec;
    private int idUtilisateur;
    private long idFormation; // Correspond au BIGINT de ta base de données
    private String sujet;
    private String description;
    private LocalDateTime dateEnvoi;
    private StatutReclamation statut; // Utilisation de l'Enum
    // Dans Reclamation.java, ajoute cet attribut pour le nom de l'étudiant
    private String nomUtilisateur;

    public String getNomUtilisateur() { return nomUtilisateur; }
    public void setNomUtilisateur(String nomUtilisateur) { this.nomUtilisateur = nomUtilisateur; }

    // --- Constructeurs ---

    // Constructeur vide (nécessaire pour JDBC / Hibernate)
    public Reclamation() {
        this.statut = StatutReclamation.EN_ATTENTE; // Statut par défaut
        this.dateEnvoi = LocalDateTime.now();
    }

    // Constructeur complet (utile pour le Back Office)
    public Reclamation(int idRec, int idUtilisateur, long idFormation, String sujet, String description, LocalDateTime dateEnvoi, StatutReclamation statut) {
        this.idRec = idRec;
        this.idUtilisateur = idUtilisateur;
        this.idFormation = idFormation;
        this.sujet = sujet;
        this.description = description;
        this.dateEnvoi = dateEnvoi;
        this.statut = statut;
    }

    // --- Getters et Setters ---

    public int getIdRec() {
        return idRec;
    }

    public void setIdRec(int idRec) {
        this.idRec = idRec;
    }

    public int getIdUtilisateur() {
        return idUtilisateur;
    }

    public void setIdUtilisateur(int idUtilisateur) {
        this.idUtilisateur = idUtilisateur;
    }

    public long getIdFormation() {
        return idFormation;
    }

    public void setIdFormation(long idFormation) {
        this.idFormation = idFormation;
    }

    public String getSujet() {
        return sujet;
    }

    public void setSujet(String sujet) {
        this.sujet = sujet;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getDateEnvoi() {
        return dateEnvoi;
    }

    public void setDateEnvoi(LocalDateTime dateEnvoi) {
        this.dateEnvoi = dateEnvoi;
    }

    public StatutReclamation getStatut() {
        return statut;
    }

    public void setStatut(StatutReclamation statut) {
        this.statut = statut;
    }
    // Dans Reclamation.java, ajoute cet attribut
    private String titreFormation;

    // Ajoute son getter et setter
    public String getTitreFormation() { return titreFormation; }
    public void setTitreFormation(String titreFormation) { this.titreFormation = titreFormation; }

    // --- Méthode toString (Utile pour le débug) ---
    @Override
    public String toString() {
        return "Reclamation{" +
                "idRec=" + idRec +
                ", idFormation=" + idFormation +
                ", sujet='" + sujet + '\'' +
                ", statut=" + statut +
                '}';
    }
}