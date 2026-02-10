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

    public certificats(){}

    public certificats(int idCertificat, Long idParticipation, LocalDate dateEmission, String codeVerification, String mention, String urlFichier) {
        this.idCertificat = idCertificat;
        this.idParticipation = idParticipation;
        this.dateEmission = dateEmission;
        this.codeVerification = codeVerification;
        this.mention =  MentionCertificat.valueOf(mention);
        this.urlFichier = urlFichier;
    }

    public int getIdCertificat() {
        return idCertificat;
    }

    public void setIdCertificat(int idCertificat) {
        this.idCertificat = idCertificat;
    }

    public Long getIdParticipation() {
        return idParticipation;
    }

    public void setIdParticipation(Long idParticipation) {
        this.idParticipation = idParticipation;
    }

    public LocalDate getDateEmission() {
        return dateEmission;
    }

    public void setDateEmission(LocalDate dateEmission) {
        this.dateEmission = dateEmission;
    }

    public String getCodeVerification() {
        return codeVerification;
    }

    public void setCodeVerification(String codeVerification) {
        this.codeVerification = codeVerification;
    }

    public MentionCertificat getMention() {
        return mention;
    }

    public void setMention(MentionCertificat mention) {
        this.mention = mention;
    }

    public String getUrlFichier() {
        return urlFichier;
    }

    public void setUrlFichier(String urlFichier) {
        this.urlFichier = urlFichier;
    }

    @Override
    public String toString() {
        return "certificats{" +
                "idCertificat=" + idCertificat +
                ", idParticipation=" + idParticipation +
                ", dateEmission=" + dateEmission +
                ", codeVerification='" + codeVerification + '\'' +
                ", mention='" + mention + '\'' +
                ", urlFichier='" + urlFichier + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        certificats that = (certificats) o;
        return Objects.equals(idCertificat, that.idCertificat) && Objects.equals(idParticipation, that.idParticipation) && Objects.equals(dateEmission, that.dateEmission) && Objects.equals(codeVerification, that.codeVerification) && mention == that.mention && Objects.equals(urlFichier, that.urlFichier);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idCertificat, idParticipation, dateEmission, codeVerification, mention, urlFichier);
    }
}