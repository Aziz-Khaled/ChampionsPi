package tn.esprit.Champions.models;

import java.time.LocalDateTime;
import java.util.Objects;

public class participations {
    private int idParticipation;
    private int idFormation;
    private int idUtilisateur;
    private LocalDateTime dateInscription;
    private StatutParticipation statut;
    private boolean presence;
    private Float note;

    // CHAMPS ADDITIONNELS (Pour l'affichage sans ID)
    private String titreFormation;
    private String nomUtilisateur;

    public participations() {}

    public participations(int idParticipation, int idFormation, int idUtilisateur,
                          LocalDateTime dateInscription, StatutParticipation statut,
                          boolean presence, Float note) {
        this.idParticipation = idParticipation;
        this.idFormation = idFormation;
        this.idUtilisateur = idUtilisateur;
        this.dateInscription = dateInscription;
        this.statut = statut;
        this.presence = presence;
        this.note = note;
    }

    // Getters et Setters pour l'affichage (Crucial pour TableView)
    public String getTitreFormation() { return titreFormation; }
    public void setTitreFormation(String titreFormation) { this.titreFormation = titreFormation; }
    public String getNomUtilisateur() { return nomUtilisateur; }
    public void setNomUtilisateur(String nomUtilisateur) { this.nomUtilisateur = nomUtilisateur; }

    // Getters et Setters standards
    public int getIdParticipation() { return idParticipation; }
    public void setIdParticipation(int idParticipation) { this.idParticipation = idParticipation; }
    public int getIdFormation() { return idFormation; }
    public void setIdFormation(int idFormation) { this.idFormation = idFormation; }
    public int getIdUtilisateur() { return idUtilisateur; }
    public void setIdUtilisateur(int idUtilisateur) { this.idUtilisateur = idUtilisateur; }
    public LocalDateTime getDateInscription() { return dateInscription; }
    public void setDateInscription(LocalDateTime dateInscription) { this.dateInscription = dateInscription; }
    public StatutParticipation getStatut() { return statut; }
    public void setStatut(StatutParticipation statut) { this.statut = statut; }
    public boolean isPresence() { return presence; }
    public void setPresence(boolean presence) { this.presence = presence; }
    public Float getNote() { return note; }
    public void setNote(Float note) { this.note = note; }
}