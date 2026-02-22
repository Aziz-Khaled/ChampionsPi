package tn.esprit.Champions.gui.invest;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.chart.PieChart;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import tn.esprit.Champions.models.*;
import tn.esprit.Champions.services.RiskAnalysisService;
import tn.esprit.Champions.services.projetService;

import java.io.IOException;
import java.sql.SQLException;

public class DetailsCreditController {

    @FXML private Label lblTitle, lblDescription, lblSecteurBadge, lblScoreRisque, lblWalletSolde;
    @FXML private Label lblSimulatedAmount, lblEstimatedProfit;
    @FXML private Slider investmentSlider;
    @FXML private VBox reportContainer;
    @FXML private ImageView imgLarge;
    @FXML private PieChart riskChart;
    @FXML private Button btnInvestir;

    private double currentTaux = 0.0;
    private credit currentCredit;
    private final projetService ps = new projetService();
    private wallet userWallet;
    private final RiskAnalysisService expertIA = new RiskAnalysisService();

    public void initData(credit c) {
        if (c == null) return;
        this.currentCredit = c;
        this.currentTaux = c.getTaux();

        try {
            // 1. Initialisation du Wallet Partenaire (Statique comme demandé)
            userWallet = new wallet(1, 101, typeWallet.FIAT, "Principal", 25000.0, statutWallet.actif);
            if (lblWalletSolde != null) {
                lblWalletSolde.setText(String.format("Solde Wallet: %.2f TND", userWallet.getSolde()));
            }

            // 2. Chargement des données projet
            projet p = ps.findById(c.getProject_id());
            if (p != null) {
                if (lblTitle != null) lblTitle.setText(p.getTitle());
                if (lblDescription != null) lblDescription.setText(p.getDescription());
                if (lblSecteurBadge != null) lblSecteurBadge.setText(p.getSecteur().toUpperCase());

                if (imgLarge != null && p.getImageUrl() != null && !p.getImageUrl().isEmpty()) {
                    try {
                        imgLarge.setImage(new Image(p.getImageUrl(), true));
                    } catch (Exception e) {
                        System.err.println("Erreur image : " + e.getMessage());
                    }
                }
            }

            // 3. Configuration du simulateur
            if (investmentSlider != null) {
                investmentSlider.setMin(0);
                investmentSlider.setMax(userWallet.getSolde());
                investmentSlider.setValue(Math.min(1000, userWallet.getSolde()));
                investmentSlider.valueProperty().addListener((obs, oldVal, newVal) -> updateSimulation(newVal.doubleValue()));
                updateSimulation(investmentSlider.getValue());
            }

            // 4. Lancement de l'étude IA
            genererEtudePro();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void updateSimulation(double amount) {
        if (lblSimulatedAmount != null) lblSimulatedAmount.setText(String.format("%.0f TND", amount));
        double profit = amount * (currentTaux / 100);
        if (lblEstimatedProfit != null) lblEstimatedProfit.setText(String.format("+ %.2f TND", profit));

        // Sécurité : bloquer l'investissement si le solde est dépassé
        if (btnInvestir != null) btnInvestir.setDisable(amount <= 0 || amount > userWallet.getSolde());
    }

    @FXML
    public void genererEtudePro() {
        // Afficher un message de chargement
        lblDescription.setText("L'IA analyse votre dossier...");

        // Lancer l'appel API (dans un nouveau thread pour ne pas freezer l'appli)
        new Thread(() -> {
            String resultIA = expertIA.getAiAnalysis(currentCredit, userWallet);

            // Revenir sur le thread principal pour mettre à jour l'UI
            javafx.application.Platform.runLater(() -> {
                lblDescription.setText(resultIA);
            });
        }).start();
    }

    private void construireRapport() {
        double reliability = 70 + (Math.random() * 25);
        if (lblScoreRisque != null) lblScoreRisque.setText(String.format("%.1f%%", reliability));

        if (riskChart != null) {
            riskChart.setData(FXCollections.observableArrayList(
                    new PieChart.Data("Fiabilité", reliability),
                    new PieChart.Data("Risque", 100 - reliability)
            ));
        }

        ajouterLigneRapport("📊 Analyse de Marché", "Le secteur " + lblSecteurBadge.getText() + " présente une stabilité confirmée.");

        double impact = (investmentSlider.getValue() / userWallet.getSolde()) * 100;
        String avis = impact < 15 ? "Risque de capital : Faible" : "Risque de capital : Modéré";
        ajouterLigneRapport("💳 Impact Portefeuille", avis + " (" + String.format("%.1f%%", impact) + " du solde).");

        ajouterLigneRapport("🛡️ Garantie Partenaire", "Projet éligible au fonds de garantie Champions PI.");
    }

    private void ajouterLigneRapport(String titre, String texte) {
        VBox ligne = new VBox(2);
        Label lTitre = new Label(titre);
        lTitre.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #2d3436;");
        Label lTexte = new Label(texte);
        lTexte.setStyle("-fx-font-size: 11px; -fx-text-fill: #636e72;");
        lTexte.setWrapText(true);
        ligne.getChildren().addAll(lTitre, lTexte);
        reportContainer.getChildren().add(ligne);
    }

    @FXML
    private void retourMarketplace() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/invest/Marketplace.fxml"));
            lblTitle.getScene().setRoot(root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}