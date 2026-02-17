package tn.esprit.Champions.models;

import java.sql.Timestamp;

public class projet {
    private int id_project;
    private Utilisateur owner_id; // On conserve exactement ce nom
    private String title;
    private String description;
    private projetStatus status;
    private double target_amount;
    private Timestamp start_date;
    private Timestamp end_date;
    private String imageUrl; // Ajout pour l'IA

    public projet() {}

    // Constructeur complet avec Image
    public projet(int id_project, Utilisateur owner_id, String title, String description,
                  projetStatus status, double target_amount, Timestamp start_date,
                  Timestamp end_date, String imageUrl) {
        this.id_project = id_project;
        this.owner_id = owner_id;
        this.title = title;
        this.description = description;
        this.status = status;
        this.target_amount = target_amount;
        this.start_date = start_date;
        this.end_date = end_date;
        this.imageUrl = imageUrl;
    }

    // --- GETTERS ET SETTERS ---

    public int getId_project() {
        return id_project;
    }

    public void setId_project(int id_project) {
        this.id_project = id_project;
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

    public Timestamp getStart_date() {
        return start_date;
    }

    public void setStart_date(Timestamp start_date) {
        this.start_date = start_date;
    }

    public Timestamp getEnd_date() {
        return end_date;
    }

    public void setEnd_date(Timestamp end_date) {
        this.end_date = end_date;
    }

    // NOUVEAU : Accesseurs pour l'image
    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    @Override
    public String toString() {
        return "projet{" +
                "id_project=" + id_project +
                ", owner_id=" + (owner_id != null ? owner_id.getNom() : "null") +
                ", title='" + title + '\'' +
                ", target_amount=" + target_amount +
                ", imageUrl='" + imageUrl + '\'' +
                '}';
    }
}