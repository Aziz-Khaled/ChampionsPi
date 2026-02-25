package tn.esprit.Champions.services;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import okhttp3.*;
import org.json.JSONObject;
import tn.esprit.Champions.models.Utilisateur;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class SmartContractService {

    private final OkHttpClient client = new OkHttpClient();

    public void deployAndShowContract(int creditId, double montant, String projet, Utilisateur investisseur, double taux, int dureeMois) {
        new Thread(() -> {
            try {
                // 1. APPEL API RÉEL (Simulation de validation réseau)
                JSONObject json = new JSONObject();
                json.put("creditId", creditId);
                json.put("investisseur_nom", investisseur.getNom());
                json.put("phone", investisseur.getTelephone());

                RequestBody body = RequestBody.create(json.toString(), MediaType.get("application/json; charset=utf-8"));
                Request request = new Request.Builder()
                        .url("https://jsonplaceholder.typicode.com/posts")
                        .post(body)
                        .build();

                try (Response response = client.newCall(request).execute()) {
                    if (response.isSuccessful()) {
                        String txHash = "0x" + java.util.UUID.randomUUID().toString().replace("-", "") + response.code();

                        // 2. Interface UI & Notifications SMS
                        Platform.runLater(() -> {
                            showContractDetails(txHash, montant, projet, investisseur, taux, dureeMois);
                            sendSmsNotification(investisseur, projet, txHash);
                        });
                    }
                }
            } catch (IOException e) {
                Platform.runLater(() -> new Alert(Alert.AlertType.ERROR, "Erreur réseau API Blockchain").show());
            }
        }).start();
    }

    private void showContractDetails(String hash, double montant, String projet, Utilisateur inv, double taux, int duree) {
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("📜 Smart Contract Signature");

        VBox root = new VBox(20);
        root.setPadding(new Insets(25));
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-color: #0f172a; -fx-border-color: #4ecca3; -fx-border-width: 2;");

        Label title = new Label("CONTRAT VALIDÉ PAR BLOCKCHAIN");
        title.setStyle("-fx-text-fill: #4ecca3; -fx-font-size: 18; -fx-font-weight: bold;");

        double total = montant * (1 + (taux / 100));
        double mens = total / duree;

        VBox details = new VBox(10);
        details.setPadding(new Insets(15));
        details.setStyle("-fx-background-color: #1e293b; -fx-background-radius: 10;");

        details.getChildren().addAll(
                createDataRow("🔗 Transaction Hash", hash.substring(0, 20) + "..."),
                createDataRow("👤 Investisseur", inv.getNom() + " " + inv.getPrenom()),
                createDataRow("📞 Téléphone", inv.getTelephone()),
                createDataRow("🏗️ Projet", projet),
                new Separator(),
                createDataRow("💰 Capital", montant + " TND"),
                createDataRow("💵 Mensualité", String.format("%.2f", mens) + " TND")
        );

        Button btnExport = new Button("📥 Exporter le Contrat & Échéancier");
        btnExport.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");
        btnExport.setOnAction(e -> exportFullLedger(hash, montant, projet, inv, taux, duree, mens, total));

        root.getChildren().addAll(title, details, btnExport);
        stage.setScene(new Scene(root, 520, 520));
        stage.show();
    }

    private void exportFullLedger(String hash, double m, String p, Utilisateur inv, double t, int d, double mens, double total) {
        StringBuilder sb = new StringBuilder();
        sb.append("====================================================\n");
        sb.append("         CERTIFICAT DE PRÊT DIGITAL (LEDGER)        \n");
        sb.append("====================================================\n");
        sb.append("DATE : ").append(LocalDateTime.now()).append("\n");
        sb.append("HASH BLOCKCHAIN : ").append(hash).append("\n\n");
        sb.append("INVESTISSEUR : ").append(inv.getNom().toUpperCase()).append(" ").append(inv.getPrenom()).append("\n");
        sb.append("TÉLÉPHONE : ").append(inv.getTelephone()).append("\n");
        sb.append("PIÈCE D'IDENTITÉ : ").append(inv.getPiece_identite()).append("\n\n");
        sb.append("DÉTAILS DU REMBOURSEMENT :\n");
        sb.append(String.format("%-10s | %-15s | %-15s\n", "Mois", "Mensualité", "Reste dû"));

        double reste = total;
        for (int i = 1; i <= d; i++) {
            reste -= mens;
            sb.append(String.format("%-10d | %-15.2f | %-15.2f\n", i, mens, Math.max(0, reste)));
        }

        try {
            File dir = new File("blockchain_ledger");
            if (!dir.exists()) dir.mkdir();
            File contractFile = new File(dir, "Contrat_" + inv.getNom() + "_" + System.currentTimeMillis() + ".txt");
            FileWriter fw = new FileWriter(contractFile);
            fw.write(sb.toString());
            fw.close();
            new Alert(Alert.AlertType.INFORMATION, "Contrat archivé dans le Ledger : " + contractFile.getName()).show();
        } catch (IOException ex) { ex.printStackTrace(); }
    }

    private void sendSmsNotification(Utilisateur inv, String projet, String hash) {
        Alert sms = new Alert(Alert.AlertType.INFORMATION);
        sms.setTitle("SMS Gateway");
        sms.setHeaderText("Notification envoyée au " + inv.getTelephone());
        sms.setContentText("Bonjour " + inv.getNom() + ", votre investissement pour le projet '" + projet + "' est confirmé. Transaction Hash: " + hash.substring(0, 10));
        sms.show();
    }

    private HBox createDataRow(String label, String value) {
        HBox row = new HBox(10);
        Label l = new Label(label + " :"); l.setStyle("-fx-text-fill: #94a3b8; -fx-min-width: 150;");
        Label v = new Label(value); v.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");
        row.getChildren().addAll(l, v);
        return row;
    }
}