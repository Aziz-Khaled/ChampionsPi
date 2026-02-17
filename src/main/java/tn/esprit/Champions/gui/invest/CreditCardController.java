package tn.esprit.Champions.gui.invest;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import tn.esprit.Champions.models.credit;
import tn.esprit.Champions.models.projet;
import tn.esprit.Champions.services.projetService;

public class CreditCardController {

    @FXML private ImageView imgProject;
    @FXML private Label lblTitle, lblMontant, lblTaux, lblRisque;

    // On instancie le service pour aller chercher les infos du projet
    private final projetService ps = new projetService();

    public void setCreditData(credit c) {
        if (c == null) return;

        try {
            // 1. Récupérer l'objet projet complet via son ID stocké dans le crédit
            projet p = ps.findById(c.getProject_id());

            if (p != null) {
                // On affiche les infos du projet (Titre + Image IA)
                lblTitle.setText(p.getTitle());

                if (p.getImageUrl() != null && !p.getImageUrl().isEmpty()) {
                    imgProject.setImage(new Image(p.getImageUrl(), true));
                }
            } else {
                lblTitle.setText("Projet Inconnu");
            }

            // 2. Afficher les infos propres au crédit (depuis le modèle credit)
            lblMontant.setText(String.format("%.2f %s", c.getMontant(), c.getDevise()));
            lblTaux.setText(c.getTaux() + "%");
            lblRisque.setText(c.getStatus() != null ? c.getStatus().name() : "En attente");

        } catch (Exception e) {
            System.err.println("Erreur lors de la récupération du projet : " + e.getMessage());
            e.printStackTrace();
        }
    }
    // Dans CreditCardController.java

    @FXML
    private void handleInvest() {
        System.out.println("Bouton Investir cliqué pour le projet : " + lblTitle.getText());
        // Ici tu pourras ajouter la logique pour ouvrir la page de paiement ou de détails
    }
}