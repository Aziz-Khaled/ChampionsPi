package tn.esprit.Champions.services;

import tn.esprit.Champions.models.NotificationType;
import tn.esprit.Champions.models.transaction;
import tn.esprit.Champions.utils.DbConnection;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.*;
import java.util.Base64;

public class BlockchainService {
    private Connection cnx;
    private NotificationAdminService notificationService;
    private static final String AES_KEY = "Champions_Secure"; // 16 bytes

    public BlockchainService() {
        cnx = DbConnection.getInstance().getCnx();
        notificationService = new NotificationAdminService();
    }

    // --- SÉCURITÉ AES (BACKUP INVIOLABLE) ---
    private String encrypt(String data) {
        try {
            SecretKeySpec secretKey = new SecretKeySpec(AES_KEY.getBytes(StandardCharsets.UTF_8), "AES");
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            byte[] encrypted = cipher.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) { return "ERROR_ENCRYPT"; }
    }

    private String decrypt(String encryptedData) {
        try {
            SecretKeySpec secretKey = new SecretKeySpec(AES_KEY.getBytes(StandardCharsets.UTF_8), "AES");
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            byte[] decoded = Base64.getDecoder().decode(encryptedData);
            return new String(cipher.doFinal(decoded), StandardCharsets.UTF_8);
        } catch (Exception e) { return "ERREUR_DE_DECHIFFREMENT"; }
    }

    // --- LOGIQUE CORE : AJOUT ET VÉRIFICATION ---

    public void addBlock(transaction t) throws SQLException {
        // 1. Audit complet avant d'autoriser une nouvelle transaction
        verifyBlockchain();

        // 2. Blocage si une anomalie est détectée
        if (isBlockchainCorrupted()) {
            throw new RuntimeException("CRITICAL: Blockchain integrity compromised. Operation blocked.");
        }

        // 3. Récupération des données du dernier bloc pour le lien
        String queryLast = """
            SELECT b.current_hash, b.id_transaction, b.montant, b.type, 
                   ws.rib as rib_s, wd.rib as rib_d, c.last_4_digits as last4
            FROM blockchain b
            LEFT JOIN wallet ws ON b.wallet_source = ws.id_wallet
            LEFT JOIN wallet wd ON b.wallet_destination = wd.id_wallet
            LEFT JOIN credit_card c ON b.id_card = c.id_card
            ORDER BY b.block_index DESC LIMIT 1
        """;

        String previousHashOnly = "0000";
        String encryptedBackupOfPrevious = "";
        int newIndex = 1;

        try (Statement st = cnx.createStatement(); ResultSet rs = st.executeQuery(queryLast)) {
            if (rs.next()) {
                previousHashOnly = rs.getString("current_hash");
                newIndex = getLatestIndex() + 1;
                String typePrev = rs.getString("type");

                // Source (Masquage si Carte)
                String source = "RECHARGE".equals(typePrev) ?
                        "************" + (rs.getString("last4") != null ? rs.getString("last4") : "0000") :
                        (rs.getString("rib_s") != null ? rs.getString("rib_s") : "N/A");

                String dest = (rs.getString("rib_d") != null) ? rs.getString("rib_d") : "EXTERNE";

                // Création du backup chiffré
                String dataToBackup = String.format("ID:%d|MT:%.2f|TYP:%s|SRC:%s|DST:%s",
                        rs.getInt("id_transaction"), rs.getDouble("montant"), typePrev, source, dest);

                encryptedBackupOfPrevious = encrypt(dataToBackup);
            }
        }

        // 4. Calcul du nouveau hash et insertion
        String newHash = generateHash(t.getIdTransaction() + "|" + previousHashOnly + "|" + t.getMontant());
        String storageValue = previousHashOnly + ";" + encryptedBackupOfPrevious;

        String sql = "INSERT INTO blockchain (id_transaction, block_index, previous_hash, current_hash, wallet_source, wallet_destination, montant, type, id_card) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pst = cnx.prepareStatement(sql)) {
            pst.setInt(1, t.getIdTransaction());
            pst.setInt(2, newIndex);
            pst.setString(3, storageValue);
            pst.setString(4, newHash);
            if (t.getIdWalletSource() <= 0) pst.setNull(5, Types.INTEGER); else pst.setInt(5, t.getIdWalletSource());
            pst.setInt(6, t.getIdWalletDestination());
            pst.setDouble(7, t.getMontant());
            pst.setString(8, t.getType().name());
            if (t.getId_card() <= 0) pst.setNull(9, Types.INTEGER); else pst.setInt(9, t.getId_card());
            pst.executeUpdate();
        }
    }

    public void verifyBlockchain() throws SQLException {
        String query = "SELECT * FROM blockchain ORDER BY block_index ASC";
        String lastKnownHash = "0000";
        int expectedIndex = 1;

        try (Statement st = cnx.createStatement(); ResultSet rs = st.executeQuery(query)) {
            while (rs.next()) {
                int currentIndex = rs.getInt("block_index");
                String[] parts = rs.getString("previous_hash").split(";");
                String storedPrevHash = parts[0];

                // A. DÉTECTION DELETE
                if (currentIndex > expectedIndex) {
                    handleDeleteDetected(expectedIndex, parts);
                    expectedIndex = currentIndex;
                }

                // B. DÉTECTION UPDATE (Audit SQL vs AES)
                if (parts.length > 1) {
                    String decrypted = decrypt(parts[1]);
                    try {
                        int oldId = Integer.parseInt(decrypted.split("\\|")[0].split(":")[1]);
                        double oldAmount = Double.parseDouble(decrypted.split("\\|")[1].split(":")[1]);
                        checkAndTriggerRupture(oldId, oldAmount, decrypted);
                    } catch (Exception e) {}
                }

                // C. DÉTECTION RUPTURE DE LIEN
                if (!storedPrevHash.equals(lastKnownHash)) {
                    notificationService.createNotification(rs.getInt("id_transaction"),
                            NotificationType.BLOCKCHAIN_CORRUPTED, "🚨 RUPTURE : Lien brisé au bloc #" + currentIndex);
                }

                lastKnownHash = rs.getString("current_hash");
                expectedIndex++;
            }
        }
    }

    private void handleDeleteDetected(int missingIdx, String[] nextBlockParts) throws SQLException {
        StringBuilder msg = new StringBuilder();
        msg.append("🚨 ALERTE : DELETE_detected\n");
        msg.append("------------------------------------------\n");
        msg.append("❌ Bloc disparu à l'index : ").append(missingIdx).append("\n");

        if (nextBlockParts.length > 1) {
            String decrypted = decrypt(nextBlockParts[1]);
            String[] d = decrypted.split("\\|");
            msg.append("📝 Type : ").append(d[2].split(":")[1]).append("\n");
            msg.append("💰 Montant : ").append(d[1].split(":")[1]).append(" DT\n");
            msg.append("💳 Source : ").append(d[3].split(":")[1]).append("\n");
            msg.append("📥 Destination : ").append(d[4].split(":")[1]).append("\n");
        }
        msg.append("------------------------------------------\n");
        msg.append("⚠️ Blocage immédiat activé !");
        notificationService.createNotification(null, NotificationType.DELETE_DETECTED, msg.toString());
    }

    private void checkAndTriggerRupture(int transId, double backupAmount, String fullBackupLabel) throws SQLException {
        String sql = "SELECT montant FROM transaction WHERE id_transaction = ?";
        try (PreparedStatement pst = cnx.prepareStatement(sql)) {
            pst.setInt(1, transId);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    double currentAmount = rs.getDouble("montant");
                    if (Math.abs(currentAmount - backupAmount) > 0.001) {
                        String[] d = fullBackupLabel.split("\\|");
                        StringBuilder msg = new StringBuilder();
                        msg.append("🚨 ALERTE : UPDATE_detected\n");
                        msg.append("------------------------------------------\n");
                        msg.append("📝 Type : ").append(d[2].split(":")[1]).append("\n");
                        msg.append("💳 Source : ").append(d[3].split(":")[1]).append("\n");
                        msg.append("📥 Destination : ").append(d[4].split(":")[1]).append("\n");
                        msg.append("------------------------------------------\n");
                        msg.append("📜 ANCIEN : ").append(String.format("%.2f", backupAmount)).append(" DT\n");
                        msg.append("🕵️ NOUVEAU : ").append(String.format("%.2f", currentAmount)).append(" DT\n");
                        msg.append("------------------------------------------\n");

                        notificationService.createNotification(transId, NotificationType.UPDATE_DETECTED, msg.toString());
                        // On sabote le hash pour que isBlockchainCorrupted() échoue
                        breakChainOnUpdate(transId, currentAmount);
                    }
                }
            }
        }
    }

    public void breakChainOnUpdate(int transactionId, double newAmount) throws SQLException {
        String sqlSelect = "SELECT previous_hash FROM blockchain WHERE id_transaction = ?";
        String prevHash = "0000";
        try (PreparedStatement pst = cnx.prepareStatement(sqlSelect)) {
            pst.setInt(1, transactionId);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) prevHash = rs.getString("previous_hash").split(";")[0];
        }
        // Recalcul d'un hash basé sur la fraude pour briser la chaîne
        String corruptedHash = generateHash(transactionId + "|" + prevHash + "|" + newAmount);
        String sqlUpdate = "UPDATE blockchain SET current_hash = ? WHERE id_transaction = ?";
        try (PreparedStatement pst = cnx.prepareStatement(sqlUpdate)) {
            pst.setString(1, corruptedHash);
            pst.setInt(2, transactionId);
            pst.executeUpdate();
        }
    }

    public boolean isBlockchainCorrupted() throws SQLException {
        // RECALCUL DYNAMIQUE : On compare le hash calculé "en direct" avec celui stocké
        String query = """
            SELECT b.block_index, b.previous_hash, b.current_hash, b.id_transaction, t.montant 
            FROM blockchain b 
            JOIN transaction t ON b.id_transaction = t.id_transaction 
            ORDER BY b.block_index ASC
        """;

        String lastHash = "0000";
        int expectedIdx = 1;

        try (Statement st = cnx.createStatement(); ResultSet rs = st.executeQuery(query)) {
            while (rs.next()) {
                int idTrans = rs.getInt("id_transaction");
                double montantSql = rs.getDouble("montant");
                String storedCurrentHash = rs.getString("current_hash");
                String storedPrevHash = rs.getString("previous_hash").split(";")[0];

                // 1. Validation du contenu (Update Detection via Recalcul)
                String liveHash = generateHash(idTrans + "|" + storedPrevHash + "|" + montantSql);
                if (!liveHash.equals(storedCurrentHash)) return true;

                // 2. Validation de la continuité (Delete Detection)
                if (rs.getInt("block_index") != expectedIdx) return true;

                // 3. Validation du lien (Hash Chain)
                if (!storedPrevHash.equals(lastHash)) return true;

                lastHash = storedCurrentHash;
                expectedIdx++;
            }
        }
        return false;
    }

    private int getLatestIndex() throws SQLException {
        try (Statement st = cnx.createStatement(); ResultSet rs = st.executeQuery("SELECT MAX(block_index) FROM blockchain")) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    public String generateHash(String data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) hexString.append(String.format("%02x", b));
            return hexString.toString();
        } catch (Exception e) { return "ERROR"; }
    }
}