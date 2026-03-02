package tn.esprit.Champions.models;

import java.time.LocalDate;
import java.util.Objects;

public class formations {
    private int idFormation;
    private String titre;
    private String description;
    private String domaine;
    private LocalDate dateDebut;
    private LocalDate dateFin;
    private double prix;
    private int capaciteMax;
    private StatutFormation statut;
    private double rating;
    private String imagePath;

    public formations(){}

    public formations(String titre, int idFormation, String description, String domaine, LocalDate dateDebut, LocalDate dateFin, double prix, int capaciteMax, String statut) {
        this.titre = titre;
        this.idFormation = idFormation;
        this.description = description;
        this.domaine = domaine;
        this.dateDebut = dateDebut;
        this.dateFin = dateFin;
        this.prix = prix;
        this.capaciteMax = capaciteMax;
        this.statut = StatutFormation.valueOf(statut);
        this.rating = rating;


    }

    public int getIdFormation() {
        return idFormation;
    }

    public String getTitre() {
        return titre;
    }

    public String getDescription() {
        return description;
    }

    public String getDomaine() {
        return domaine;
    }

    public LocalDate getDateDebut() {
        return dateDebut;
    }

    public LocalDate getDateFin() {
        return dateFin;
    }

    public double getPrix() {
        return prix;
    }

    public int getCapaciteMax() {
        return capaciteMax;
    }

    public StatutFormation getStatut() {
        return statut;
    }

    public double getRating() { return rating; }

    public String getImagePath() { return imagePath; }
    public void setImagePath(String imagePath) { this.imagePath = imagePath; }

    public void setRating(double rating) { this.rating = rating; }

    public void setIdFormation(int idFormation) {
        this.idFormation = idFormation;
    }

    public void setTitre(String titre) {
        this.titre = titre;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setDomaine(String domaine) {
        this.domaine = domaine;
    }

    public void setDateDebut(LocalDate dateDebut) {
        this.dateDebut = dateDebut;
    }

    public void setDateFin(LocalDate dateFin) {
        this.dateFin = dateFin;
    }

    public void setPrix(double prix) {
        this.prix = prix;
    }

    public void setCapaciteMax(int capaciteMax) {
        this.capaciteMax = capaciteMax;
    }

    public void setStatut(StatutFormation statut) {
        this.statut = statut;
    }

    @Override
    public String toString() {
        return "formations{" +
                "idFormation=" + idFormation +
                ", titre='" + titre + '\'' +
                ", description='" + description + '\'' +
                ", domaine='" + domaine + '\'' +
                ", dateDebut=" + dateDebut +
                ", dateFin=" + dateFin +
                ", prix=" + prix +
                ", capaciteMax=" + capaciteMax +
                ", statut='" + statut + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        formations that = (formations) o;
        return Double.compare(prix, that.prix) == 0 && capaciteMax == that.capaciteMax && Objects.equals(idFormation, that.idFormation) && Objects.equals(titre, that.titre) && Objects.equals(description, that.description) && Objects.equals(domaine, that.domaine) && Objects.equals(dateDebut, that.dateDebut) && Objects.equals(dateFin, that.dateFin) && statut == that.statut;
    }

    @Override
    public int hashCode() {
        return Objects.hash(idFormation, titre, description, domaine, dateDebut, dateFin, prix, capaciteMax, statut);
    }
}