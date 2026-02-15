package tn.esprit.Champions.gui.invest;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import tn.esprit.Champions.models.credit;

public class CreditCardController {
    @FXML private Label lblTitle, lblMontant, lblTaux, lblRisque;
    @FXML private ImageView imgProject;

    public void setData(String titre, String secteur, double montant, double taux, String risque) {
        if (lblTitle != null) lblTitle.setText(titre);
        if (lblMontant != null) lblMontant.setText(montant + " DT");
        if (lblTaux != null) lblTaux.setText(taux + "%");
        if (lblRisque != null) lblRisque.setText(risque);

        // On appelle la nouvelle méthode de génération
        generateProImage(titre);
    }

    public void setCreditData(credit c) {
        if (c != null) {
            setData(c.getDescription(), "", c.getMontant(), c.getTaux(),
                    (c.getStatus() != null ? c.getStatus().toString() : "OPEN"));
        }
    }

    private void generateProImage(String description) {
        try {
            // 1. On définit une banque de thèmes pour garantir le contexte
            String theme = "business,office"; // Par défaut
            String descLower = description.toLowerCase();

            // 2. Mapping manuel simple pour être sûr du résultat
            if (descLower.contains("médical") || descLower.contains("santé") || descLower.contains("docteur")) {
                theme = "hospital,medical,clinic";
            } else if (descLower.contains("startup") || descLower.contains("ia") || descLower.contains("tech")) {
                theme = "technology,startup,computer";
            } else if (descLower.contains("éco") || descLower.contains("construction") || descLower.contains("maison")) {
                theme = "architecture,construction,house";
            } else if (descLower.contains("artisanat") || descLower.contains("atelier")) {
                theme = "craft,workshop,handmade";
            } else if (descLower.contains("commerce") || descLower.contains("boutique")) {
                theme = "shop,retail,store";
            }

            // 3. Utilisation de l'URL Unsplash Source la plus robuste
            // On ajoute le hash de la description pour que deux projets "Médicaux"
            // n'aient pas exactement la même photo, mais restent dans le thème.
            String imageUrl = "https://source.unsplash.com/featured/400x225/?" + theme + "&sig=" + description.hashCode();

            // 4. Chargement avec un petit "User-Agent" simulé (certains services bloquent Java sinon)
            Image img = new Image(imageUrl, true);

            imgProject.setImage(img);

        } catch (Exception e) {
            // Image générique pro en cas de coupure internet
            imgProject.setImage(new Image("https://images.unsplash.com/photo-1486406146926-c627a92ad1ab?q=80&w=400&h=225&auto=format&fit=crop"));
        }
    }

    @FXML private void handleInvest() {
        if (lblTitle != null) System.out.println("Analyse de : " + lblTitle.getText());
    }
}