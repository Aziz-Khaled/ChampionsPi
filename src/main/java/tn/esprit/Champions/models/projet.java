package tn.esprit.Champions.models;

import java.time.LocalDate;

public class projet {
    private int id_projet;
    private Utilisateur owner_id; // On conserve exactement ce nom
    private String title;
    private String description;
    private projetStatus status;
    private double target_amount;
    private LocalDate start_date;
    private LocalDate end_date;
    private String image_url; // Modifié de imageUrl à image_url pour correspondre à SQL
    private String secteur;

    public projet() {}

    // Constructeur complet avec Image
    public projet(int id_projet, Utilisateur owner_id, String title, String description,
                  projetStatus status, double target_amount, LocalDate start_date,
                  LocalDate end_date, String image_url) {
        this.id_projet = id_projet;
        this.owner_id = owner_id;
        this.title = title;
        this.description = description;
        this.status = status;
        this.target_amount = target_amount;
        this.start_date = start_date;
        this.end_date = end_date;
        this.image_url = image_url;
        this.secteur = secteur;

    }

    // --- GETTERS ET SETTERS ---

    public int getId_projet() {
        return id_projet;
    }

    public void setId_projet(int id_projet) {
        this.id_projet = id_projet;
    }

    public Utilisateur getOwner_id() {
        return owner_id;
    }

    public void setOwner_id(Utilisateur owner_id) {
        this.owner_id = owner_id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public projetStatus getStatus() {
        return status;
    }

    public void setStatus(projetStatus status) {
        this.status = status;
    }

    public double getTarget_amount() {
        return target_amount;
    }

    public void setTarget_amount(double target_amount) { // Changé float en double pour cohérence
        this.target_amount = target_amount;
    }

    public LocalDate getStart_date() {
        return start_date;
    }

    public void setStart_date(LocalDate start_date) {
        this.start_date = start_date;
    }

    public LocalDate getEnd_date() {
        return end_date;
    }

    public void setEnd_date(LocalDate end_date) {
        this.end_date = end_date;
    }

    // NOUVEAU : Accesseurs pour l'image
    public String getImage_url() {
        return image_url;
    }

    public void setImage_url(String image_url) {
        this.image_url = image_url;
    }
    public String getSecteur() { return secteur; }
    public void setSecteur(String secteur) { this.secteur = secteur; }

    @Override
    public String toString() {
        return "projet{" +
                "id_projet=" + id_projet +
                ", owner_id=" + (owner_id != null ? owner_id.getNom() : "null") +
                ", title='" + title + '\'' +
                ", secteur='" + secteur + '\'' +
                ", target_amount=" + target_amount +
                ", image_url='" + image_url + '\'' +
                '}';
    }
}