package tn.esprit.Champions.models;

public class Utilisateur {
    private int id_user ;
    private String nom,prenom , mot_de_passe, telephone ;
    private String piece_identite , user_image ;
    private Role role ;


    public Utilisateur(int id_user, String nom, String prenom, String mot_de_passe, String telephone, String piece_identite, String user_image, Role role) {
        this.id_user = id_user;
        this.nom = nom;
        this.prenom = prenom;
        this.mot_de_passe = mot_de_passe;
        this.telephone = telephone;
        this.piece_identite = piece_identite;
        this.user_image = user_image;
        this.role = role;
    }

    public int getId_user() {
        return id_user;
    }

    public void setId_user(int id_user) {
        this.id_user = id_user;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getPrenom() {
        return prenom;
    }

    public void setPrenom(String prenom) {
        this.prenom = prenom;
    }

    public String getMot_de_passe() {
        return mot_de_passe;
    }

    public void setMot_de_passe(String mot_de_passe) {
        this.mot_de_passe = mot_de_passe;
    }

    public String getTelephone() {
        return telephone;
    }

    public void setTelephone(String telephone) {
        this.telephone = telephone;
    }

    public String getPiece_identite() {
        return piece_identite;
    }

    public void setPiece_identite(String piece_identite) {
        this.piece_identite = piece_identite;
    }

    public String getUser_image() {
        return user_image;
    }

    public void setUser_image(String user_image) {
        this.user_image = user_image;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }
}
