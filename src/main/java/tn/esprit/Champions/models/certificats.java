package tn.esprit.Champions.models;

import java.time.LocalDate;
import java.util.Objects;

public class certificats {

    private int idCertificat;
    private Long idParticipation;
    private LocalDate dateEmission;
    private String codeVerification;
    private MentionCertificat mention;
    private String urlFichier;

    // --- AJOUT : Champs pour l'affichage PDF (non persistés en DB) ---
    private String nomEtudiant;
    private String nomFormation;

    public certificats(){}

    public certificats(int idCertificat, Long idParticipation, LocalDate dateEmission, String codeVerification, MentionCertificat mention, String urlFichier) {
        this.idCertificat = idCertificat;
        this.idParticipation = idParticipation;
        this.dateEmission = dateEmission;
        this.codeVerification = codeVerification;
        this.mention = mention;
        this.urlFichier = urlFichier;
    }

    // Getters et Setters existants...
    public int getIdCertificat() { return idCertificat; }
    public void setIdCertificat(int idCertificat) { this.idCertificat = idCertificat; }
    public Long getIdParticipation() { return idParticipation; }
    public void setIdParticipation(Long idParticipation) { this.idParticipation = idParticipation; }
    public LocalDate getDateEmission() { return dateEmission; }
    public void setDateEmission(LocalDate dateEmission) { this.dateEmission = dateEmission; }
    public String getCodeVerification() { return codeVerification; }
    public void setCodeVerification(String codeVerification) { this.codeVerification = codeVerification; }
    public MentionCertificat getMention() { return mention; }
    public void setMention(MentionCertificat mention) { this.mention = mention; }
    public String getUrlFichier() { return urlFichier; }
    public void setUrlFichier(String urlFichier) { this.urlFichier = urlFichier; }

    // --- AJOUT : Getters et Setters pour les données PDF ---
    public String getNomEtudiant() { return nomEtudiant; }
    public void setNomEtudiant(String nomEtudiant) { this.nomEtudiant = nomEtudiant; }
    public String getNomFormation() { return nomFormation; }
    public void setNomFormation(String nomFormation) { this.nomFormation = nomFormation; }

    @Override
    public String toString() {
        return "certificats{" +
                "idCertificat=" + idCertificat +
                ", nomEtudiant='" + nomEtudiant + '\'' +
                ", formation='" + nomFormation + '\'' +
                ", mention=" + mention +
                '}';
    }

    // Equals et HashCode restent identiques (basés sur les IDs et le code)
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        certificats that = (certificats) o;
        return idCertificat == that.idCertificat && Objects.equals(idParticipation, that.idParticipation) && Objects.equals(codeVerification, that.codeVerification);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idCertificat, idParticipation, codeVerification);
    }
}