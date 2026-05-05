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
import java.util.Locale;

public class BlockchainService {
    private Connection cnx;
    private NotificationAdminService notificationService;
    private static final String AES_KEY = "Champions_Secure"; // 16 bytes

    public BlockchainService() {
        cnx = DbConnection.getInstance().getCnx();
        notificationService = new NotificationAdminService();
    }

    // --- SÉCURITÉ AES ---
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

    // --- AJOUT DE BLOC ---
    public void addBlock(transaction t) throws SQLException {
        if (isBlockchainCorrupted()) {
            throw new RuntimeException("CRITICAL: Blockchain integrity compromised. Operation blocked.");
        }

        String previousHashOnly = "0000";
        String encryptedBackupOfPrevious = "";
        int newIndex = 1;

        String queryLast = """
            SELECT b.current_hash, b.block_index, b.id_transaction, b.montant, b.type, 
                   ws.rib as rib_s, wd.rib as rib_d, c.last_4_digits as last4
            FROM blockchain b
            LEFT JOIN wallet ws ON b.wallet_source = ws.id_wallet
            LEFT JOIN wallet wd ON b.wallet_destination = wd.id_wallet
            LEFT JOIN credit_card c ON b.id_card = c.id_card
            ORDER BY b.block_index DESC LIMIT 1
        """;

        try (PreparedStatement st = cnx.prepareStatement(queryLast); ResultSet rs = st.executeQuery()) {
            if (rs.next()) {
                previousHashOnly = rs.getString("current_hash");
                newIndex = rs.getInt("block_index") + 1;

                String dataToBackup = String.format(Locale.US, "ID:%d|MT:%.2f|TYP:%s|SRC:%s|DST:%s",
                        rs.getInt("id_transaction"), rs.getDouble("montant"),
                        rs.getString("type"),
                        (rs.getString("rib_s") != null ? rs.getString("rib_s") : "N/A"),
                        (rs.getString("rib_d") != null ? rs.getString("rib_d") : "EXTERNE"));
                encryptedBackupOfPrevious = encrypt(dataToBackup);
            }
        }

        String formattedAmount = String.format(Locale.US, "%.2f", t.getMontant());
        String newHash = generateHash(t.getIdTransaction() + "|" + previousHashOnly + "|" + formattedAmount);
        String storageValue = previousHashOnly + ";" + encryptedBackupOfPrevious;

        String sql = "INSERT INTO blockchain (id_transaction, block_index, previous_hash, current_hash, wallet_source, wallet_destination, montant, type, id_card) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pst = cnx.prepareStatement(sql)) {
            pst.setInt(1, t.getIdTransaction());
            pst.setInt(2, newIndex);
            pst.setString(3, storageValue);
            pst.setString(4, newHash);
            if (t.getIdWalletSource() <= 0) pst.setNull(5, Types.INTEGER); else pst.setInt(5, t.getIdWalletSource());
            if (t.getIdWalletDestination() <= 0) pst.setNull(6, Types.INTEGER); else pst.setInt(6, t.getIdWalletDestination());
            pst.setDouble(7, t.getMontant());
            pst.setString(8, t.getType().name());
            if (t.getId_card() <= 0) pst.setNull(9, Types.INTEGER); else pst.setInt(9, t.getId_card());
            pst.executeUpdate();
        }
    }

    // --- AUDIT ET VÉRIFICATION ---
    public boolean isBlockchainCorrupted() throws SQLException {
        String query = "SELECT id_transaction, montant, previous_hash, current_hash, block_index FROM blockchain ORDER BY block_index ASC";
        String lastHash = "0000";
        int expectedIdx = 1;

        try (Statement st = cnx.createStatement(); ResultSet rs = st.executeQuery(query)) {
            while (rs.next()) {
                String storedCurrentHash = rs.getString("current_hash");
                String storedPrevHash = rs.getString("previous_hash").split(";")[0];
                String formattedAmount = String.format(Locale.US, "%.2f", rs.getDouble("montant"));

                String recalculatedHash = generateHash(rs.getInt("id_transaction") + "|" + storedPrevHash + "|" + formattedAmount);

                if (!recalculatedHash.equals(storedCurrentHash) || rs.getInt("block_index") != expectedIdx || !storedPrevHash.equals(lastHash)) {
                    return true;
                }
                lastHash = storedCurrentHash;
                expectedIdx++;
            }
        }
        return false;
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

                if (currentIndex > expectedIndex) {
                    handleDeleteDetected(expectedIndex, parts);
                    expectedIndex = currentIndex;
                }

                if (parts.length > 1) {
                    String decrypted = decrypt(parts[1]);
                    try {
                        int oldId = Integer.parseInt(decrypted.split("\\|")[0].split(":")[1]);
                        double oldAmount = Double.parseDouble(decrypted.split("\\|")[1].split(":")[1]);
                        checkAndTriggerRupture(oldId, oldAmount, decrypted);
                    } catch (Exception e) {}
                }

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
        String corruptedHash = generateHash(transactionId + "|" + prevHash + "|" + String.format(Locale.US, "%.2f", newAmount));
        String sqlUpdate = "UPDATE blockchain SET current_hash = ? WHERE id_transaction = ?";
        try (PreparedStatement pst = cnx.prepareStatement(sqlUpdate)) {
            pst.setString(1, corruptedHash);
            pst.setInt(2, transactionId);
            pst.executeUpdate();
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