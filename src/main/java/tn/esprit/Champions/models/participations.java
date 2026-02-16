package tn.esprit.Champions.models;

import java.time.LocalDateTime;
import java.util.Objects;

public class participations {

    private int idParticipation;
    private int idFormation;
    private int idUtilisateur;
    private LocalDateTime dateInscription;
    private  StatutParticipation statut;
    private boolean presence;
    private Float note;
    // --- AJOUT : Champ pour le titre ---
    private String titreFormation;

    public participations(){}

    public participations(int idParticipation, int idFormation, int idUtilisateur, LocalDateTime dateInscription, String statut, boolean presence, Float note) {
        this.idParticipation = idParticipation;
        this.idFormation = idFormation;
        this.idUtilisateur = idUtilisateur;
        this.dateInscription = dateInscription;
        this.statut = StatutParticipation.valueOf(statut);
        this.presence = presence;
        this.note = note;
    }

    // --- NOUVEAUX GETTER / SETTER ---
    public String getTitreFormation() { return titreFormation; }
    public void setTitreFormation(String titreFormation) { this.titreFormation = titreFormation; }

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

    @Override
    public String toString() {
        return "participations{" +
                "idParticipation=" + idParticipation +
                ", idFormation=" + idFormation +
                ", idUtilisateur=" + idUtilisateur +
                ", dateInscription=" + dateInscription +
                ", statut='" + statut + '\'' +
                ", presence=" + presence +
                ", note=" + note +
                ", titreFormation='" + titreFormation + '\'' + // Ajouté au toString
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        participations that = (participations) o;
        return presence == that.presence && Objects.equals(idParticipation, that.idParticipation) && Objects.equals(idFormation, that.idFormation) && Objects.equals(idUtilisateur, that.idUtilisateur) && Objects.equals(dateInscription, that.dateInscription) && statut == that.statut && Objects.equals(note, that.note);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idParticipation, idFormation, idUtilisateur, dateInscription, statut, presence, note);
    }
}