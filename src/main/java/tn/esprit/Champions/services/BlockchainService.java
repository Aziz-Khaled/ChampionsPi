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
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class BlockchainService {
    private Connection cnx;
    private NotificationAdminService notificationService;
    private static final String AES_KEY = "Champions_Secure"; // 16 bytes

    public BlockchainService() {
        cnx = DbConnection.getInstance().getCnx();
        notificationService = new NotificationAdminService();
    }

    // =====================================================
    // --- CHIFFREMENT AES (aligné PHP openssl AES-128-ECB / PKCS7) ---
    // =====================================================
    private String encrypt(String data) {
        try {
            SecretKeySpec secretKey = new SecretKeySpec(AES_KEY.getBytes(StandardCharsets.UTF_8), "AES");
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            byte[] encrypted = cipher.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            System.err.println("Erreur chiffrement AES : " + e.getMessage());
            return "ERROR_ENCRYPT";
        }
    }

    // =====================================================
    // --- DÉCHIFFREMENT AES avec fallback double-mode ---
    //
    // Tentative 1 : AES/ECB/PKCS5Padding  → blocs PHP + nouveaux blocs Java
    // Tentative 2 : AES/ECB/NoPadding     → anciens blocs Java (taille fixe)
    // Si les deux échouent → retourne "" sans jamais crasher
    // =====================================================
    private String decrypt(String encryptedData) {
        if (encryptedData == null || encryptedData.isEmpty()) return "";

        SecretKeySpec secretKey = new SecretKeySpec(AES_KEY.getBytes(StandardCharsets.UTF_8), "AES");
        byte[] decoded;
        try {
            decoded = Base64.getDecoder().decode(encryptedData);
        } catch (Exception e) {
            System.err.println("Base64 invalide, déchiffrement ignoré.");
            return "";
        }

        // Tentative 1 : PKCS5Padding (compatible PHP et nouveaux blocs)
        try {
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            return new String(cipher.doFinal(decoded), StandardCharsets.UTF_8);
        } catch (Exception e1) {
            // Tentative 2 : NoPadding (anciens blocs Java si taille multiple de 16)
            try {
                if (decoded.length % 16 == 0) {
                    Cipher cipher = Cipher.getInstance("AES/ECB/NoPadding");
                    cipher.init(Cipher.DECRYPT_MODE, secretKey);
                    // Supprimer les bytes nuls de fin (padding manuel)
                    String result = new String(cipher.doFinal(decoded), StandardCharsets.UTF_8);
                    return result.replace("\u0000", "").trim();
                }
            } catch (Exception e2) {
                System.err.println("Déchiffrement NoPadding échoué aussi : " + e2.getMessage());
            }
        }

        System.err.println("⚠️ Bloc non déchiffrable (ancien format), ignoré sans crash.");
        return ""; // retourne vide → parseBackupChain retournera une Map vide → pas de crash
    }

    // =====================================================
    // --- PARSING SÉCURISÉ du backup (aligné avec PHP parseBackupChain) ---
    // Format : "ID:123|MT:50.00|TYP:RECHARGE|SRC:N/A|DST:EXTERNE"
    // Retourne une Map vide si le format est invalide → jamais de crash
    // =====================================================
    private Map<String, String> parseBackupChain(String decrypted) {
        Map<String, String> result = new HashMap<>();
        if (decrypted == null || decrypted.isEmpty()
                || decrypted.startsWith("ERREUR") || decrypted.startsWith("ERROR")) {
            return result;
        }
        try {
            for (String part : decrypted.split("\\|")) {
                String[] kv = part.split(":", 2);
                if (kv.length == 2) {
                    result.put(kv[0].trim(), kv[1].trim());
                }
            }
        } catch (Exception e) {
            System.err.println("Erreur parsing backup : " + e.getMessage());
        }
        return result;
    }

    // =====================================================
    // --- AJOUT DE BLOC ---
    // =====================================================
    public void addBlock(transaction t) throws SQLException {
        // 🔹 On vérifie la corruption MAIS on ne bloque plus sur les blocs manquants
        // isBlockchainCorrupted() détecte uniquement les hash invalides (UPDATE frauduleux)
        if (isBlockchainCorrupted()) {
            throw new RuntimeException("CRITICAL: Blockchain integrity compromised. Operation blocked.");
        }

        String previousHashOnly = "0000";
        String encryptedBackupOfPrevious = "";
        int newIndex = 1;

        String queryLast = """
            SELECT b.current_hash, b.block_index, b.id_transaction_id, b.montant, b.type,
                   ws.rib as rib_s, wd.rib as rib_d, c.last_4_digits as last4
            FROM blockchain b
            LEFT JOIN wallet ws ON b.wallet_source_id = ws.id_wallet
            LEFT JOIN wallet wd ON b.wallet_destination_id = wd.id_wallet
            LEFT JOIN credit_card c ON b.card_id = c.id_card
            ORDER BY b.block_index DESC LIMIT 1
        """;

        try (PreparedStatement st = cnx.prepareStatement(queryLast); ResultSet rs = st.executeQuery()) {
            if (rs.next()) {
                previousHashOnly = rs.getString("current_hash");
                newIndex = rs.getInt("block_index") + 1;

                // SOURCE LABEL aligné avec PHP
                String sourceLabel = "N/A";
                String ribS  = rs.getString("rib_s");
                String last4 = rs.getString("last4");
                String type  = rs.getString("type");

                if (ribS != null) {
                    sourceLabel = ribS;
                } else if ("RECHARGE".equalsIgnoreCase(type) && last4 != null) {
                    sourceLabel = "********" + last4;
                }

                String dataToBackup = String.format(Locale.US, "ID:%d|MT:%.2f|TYP:%s|SRC:%s|DST:%s",
                        rs.getInt("id_transaction_id"),
                        rs.getDouble("montant"),
                        type,
                        sourceLabel,
                        (rs.getString("rib_d") != null ? rs.getString("rib_d") : "EXTERNE"));

                encryptedBackupOfPrevious = encrypt(dataToBackup);
            }
        }

        String formattedAmount = String.format(Locale.US, "%.2f", t.getMontant());
        String newHash = generateHash(t.getIdTransaction() + "|" + previousHashOnly + "|" + formattedAmount);
        String storageValue = previousHashOnly + ";" + encryptedBackupOfPrevious;

        String sql = "INSERT INTO blockchain (id_transaction_id, block_index, previous_hash, current_hash, wallet_source_id, wallet_destination_id, montant, type, card_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pst = cnx.prepareStatement(sql)) {
            pst.setInt(1, t.getIdTransaction());
            pst.setInt(2, newIndex);
            pst.setString(3, storageValue);
            pst.setString(4, newHash);
            if (t.getIdWalletSource() <= 0)      pst.setNull(5, Types.INTEGER); else pst.setInt(5, t.getIdWalletSource());
            if (t.getIdWalletDestination() <= 0) pst.setNull(6, Types.INTEGER); else pst.setInt(6, t.getIdWalletDestination());
            pst.setDouble(7, t.getMontant());
            pst.setString(8, t.getType().name());
            if (t.getId_card() <= 0)             pst.setNull(9, Types.INTEGER); else pst.setInt(9, t.getId_card());
            pst.executeUpdate();
        }
    }

    // =====================================================
    // --- VÉRIFICATION INTÉGRITÉ ---
    // 🔹 Ne vérifie QUE les hash (UPDATE frauduleux)
    // 🔹 Les blocs manquants (DELETE) ne bloquent PAS les nouvelles transactions
    // =====================================================
    public boolean isBlockchainCorrupted() throws SQLException {
        String query = "SELECT id_transaction_id, montant, previous_hash, current_hash, block_index FROM blockchain ORDER BY block_index ASC";
        String lastHash = "0000";

        try (Statement st = cnx.createStatement(); ResultSet rs = st.executeQuery(query)) {
            while (rs.next()) {
                String storedCurrentHash = rs.getString("current_hash");
                String storedPrevHash    = rs.getString("previous_hash").split(";")[0];
                String formattedAmount   = String.format(Locale.US, "%.2f", rs.getDouble("montant"));

                String recalculatedHash = generateHash(
                        rs.getInt("id_transaction_id") + "|" + storedPrevHash + "|" + formattedAmount);

                // 🔹 On vérifie UNIQUEMENT que le hash du bloc est valide (anti-UPDATE)
                // On ne vérifie plus block_index ni storedPrevHash == lastHash
                // → un bloc supprimé ne fait plus bloquer les nouvelles transactions
                if (!recalculatedHash.equals(storedCurrentHash)) {
                    return true;
                }

                lastHash = storedCurrentHash;
            }
        }
        return false;
    }

    // =====================================================
    // --- AUDIT COMPLET (pour la vue admin uniquement) ---
    // Détecte DELETE + UPDATE + RUPTURE sans jamais bloquer
    // =====================================================
    public void verifyBlockchain() throws SQLException {
        String query = "SELECT * FROM blockchain ORDER BY block_index ASC";
        String lastKnownHash = "0000";
        int expectedIndex = 1;

        try (Statement st = cnx.createStatement(); ResultSet rs = st.executeQuery(query)) {
            while (rs.next()) {
                int currentIndex = rs.getInt("block_index");

                // limit=2 : évite de couper le contenu chiffré
                String[] parts = rs.getString("previous_hash").split(";", 2);
                String storedPrevHash = parts[0];

                // 1. DÉTECTION SUPPRESSION → notification uniquement, pas de blocage
                if (currentIndex > expectedIndex) {
                    handleDeleteDetected(expectedIndex, parts);
                    expectedIndex = currentIndex; // on saute les index manquants
                }

                // 2. DÉTECTION UPDATE via backup chiffré
                if (parts.length > 1 && !parts[1].isEmpty()) {
                    String decrypted = decrypt(parts[1]);
                    Map<String, String> d = parseBackupChain(decrypted);
                    try {
                        if (d.containsKey("ID") && d.containsKey("MT")) {
                            int oldId        = Integer.parseInt(d.get("ID"));
                            double oldAmount = Double.parseDouble(d.get("MT"));
                            checkAndTriggerRupture(oldId, oldAmount, d);
                        }
                    } catch (Exception e) {
                        System.err.println("⚠️ Parsing backup bloc #" + currentIndex + " ignoré : " + e.getMessage());
                    }
                }

                // 3. RUPTURE DE LIEN → notification uniquement
                if (!storedPrevHash.equals(lastKnownHash)) {
                    notificationService.createNotification(
                            rs.getInt("id_transaction_id"),
                            NotificationType.BLOCKCHAIN_CORRUPTED,
                            "🚨 RUPTURE : Lien brisé au bloc #" + currentIndex);
                }

                lastKnownHash = rs.getString("current_hash");
                expectedIndex++;
            }
        }
    }

    // =====================================================
    // --- ALERTE BLOC SUPPRIMÉ → notification seulement, pas de blocage ---
    // =====================================================
    private void handleDeleteDetected(int missingIdx, String[] nextBlockParts) throws SQLException {
        StringBuilder msg = new StringBuilder();
        msg.append("🚨 ALERTE : DELETE_detected\n");
        msg.append("------------------------------------------\n");
        msg.append("❌ Bloc disparu à l'index : ").append(missingIdx).append("\n");

        if (nextBlockParts.length > 1 && !nextBlockParts[1].isEmpty()) {
            String decrypted = decrypt(nextBlockParts[1]);
            Map<String, String> d = parseBackupChain(decrypted);
            msg.append("📝 Type : ").append(d.getOrDefault("TYP", "N/A")).append("\n");
            msg.append("💰 Montant : ").append(d.getOrDefault("MT", "0.00")).append(" DT\n");
            msg.append("💳 Source : ").append(d.getOrDefault("SRC", "N/A")).append("\n");
            msg.append("📥 Destination : ").append(d.getOrDefault("DST", "N/A")).append("\n");
        }
        msg.append("------------------------------------------\n");
        msg.append("⚠️ Suppression détectée et journalisée.");

        // 🔹 Notification admin uniquement — aucune exception lancée
        notificationService.createNotification(null, NotificationType.DELETE_DETECTED, msg.toString());
    }

    private void checkAndTriggerRupture(int transId, double backupAmount, Map<String, String> d) throws SQLException {
        String sql = "SELECT montant FROM transaction WHERE id_transaction = ?";
        try (PreparedStatement pst = cnx.prepareStatement(sql)) {
            pst.setInt(1, transId);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    double currentAmount = rs.getDouble("montant");
                    if (Math.abs(currentAmount - backupAmount) > 0.001) {
                        StringBuilder msg = new StringBuilder();
                        msg.append("🚨 ALERTE : UPDATE_detected\n");
                        msg.append("------------------------------------------\n");
                        msg.append("📝 Type : ").append(d.getOrDefault("TYP", "N/A")).append("\n");
                        msg.append("💳 Source : ").append(d.getOrDefault("SRC", "N/A")).append("\n");
                        msg.append("📥 Destination : ").append(d.getOrDefault("DST", "N/A")).append("\n");
                        msg.append("------------------------------------------\n");
                        msg.append("📜 ANCIEN : ").append(String.format("%.2f", backupAmount)).append(" DT\n");
                        msg.append("🕵️ NOUVEAU : ").append(String.format("%.2f", currentAmount)).append(" DT\n");
                        msg.append("------------------------------------------\n");

                        notificationService.createNotification(transId, NotificationType.UPDATE_DETECTED, msg.toString());
                        breakChainOnUpdate(transId, currentAmount);
                    }
                }
            }
        }
    }

    public void breakChainOnUpdate(int transactionId, double newAmount) throws SQLException {
        String sqlSelect = "SELECT previous_hash FROM blockchain WHERE id_transaction_id = ?";
        String prevHash = "0000";
        try (PreparedStatement pst = cnx.prepareStatement(sqlSelect)) {
            pst.setInt(1, transactionId);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) prevHash = rs.getString("previous_hash").split(";")[0];
        }
        String corruptedHash = generateHash(transactionId + "|" + prevHash + "|" + String.format(Locale.US, "%.2f", newAmount));
        String sqlUpdate = "UPDATE blockchain SET current_hash = ? WHERE id_transaction_id = ?";
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
        } catch (Exception e) {
            return "ERROR";
        }
    }
}