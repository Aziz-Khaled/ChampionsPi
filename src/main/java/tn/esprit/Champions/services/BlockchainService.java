package tn.esprit.Champions.services;

import tn.esprit.Champions.models.Blockchain;
import tn.esprit.Champions.models.NotificationType;
import tn.esprit.Champions.models.transaction;
import tn.esprit.Champions.models.typeTransaction;
import tn.esprit.Champions.utils.DbConnection;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class BlockchainService {
    private Connection cnx;
    private NotificationAdminService notificationService;

    public BlockchainService() {
        cnx = DbConnection.getInstance().getCnx();
        notificationService = new NotificationAdminService();
    }
    public Blockchain getLastBlock() throws SQLException {

        String query = "SELECT * FROM blockchain ORDER BY block_index DESC LIMIT 1";

        PreparedStatement pst = cnx.prepareStatement(query);
        ResultSet rs = pst.executeQuery();

        if (rs.next()) {
            Blockchain block = new Blockchain();
            block.setIdBlock(rs.getInt("id_block"));
            block.setIdTransaction(rs.getInt("id_transaction"));
            block.setBlockIndex(rs.getInt("block_index"));
            block.setPreviousHash(rs.getString("previous_hash"));
            block.setCurrentHash(rs.getString("current_hash"));
            return block;
        }

        return null;
    }
    public String generateHash(String data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data.getBytes(StandardCharsets.UTF_8));

            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    public void addBlock(transaction t) throws SQLException {


        if (isBlockchainCorrupted()) {
            throw new RuntimeException("🚨 Blockchain corrompue ! Aucun bloc ne peut être ajouté.");
        }

        Blockchain lastBlock = getLastBlock();

        String previousHash = (lastBlock == null)
                ? "0000"
                : lastBlock.getCurrentHash();

        int newIndex = (lastBlock == null)
                ? 1
                : lastBlock.getBlockIndex() + 1;

        String dataToHash = String.join("|",
                String.valueOf(t.getIdTransaction()),
                String.valueOf(t.getIdWalletSource()),
                String.valueOf(t.getIdWalletDestination()),
                String.valueOf(t.getMontant()),
                t.getType().name(),
                String.valueOf(t.getId_card()),
                previousHash
        );

        String newHash = generateHash(dataToHash);

        String sql = """
        INSERT INTO blockchain
        (id_transaction, block_index, previous_hash, current_hash,
         wallet_source, wallet_destination, montant, type, id_card)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
    """;

        try (PreparedStatement pst = cnx.prepareStatement(sql)) {

            pst.setInt(1, t.getIdTransaction());
            pst.setInt(2, newIndex);
            pst.setString(3, previousHash);
            pst.setString(4, newHash);

            if (t.getType() == typeTransaction.RECHARGE) {
                pst.setNull(5, java.sql.Types.INTEGER);
            } else {
                pst.setInt(5, t.getIdWalletSource());
            }

            pst.setInt(6, t.getIdWalletDestination());
            pst.setDouble(7, t.getMontant());
            pst.setString(8, t.getType().name());

            if (t.getType() == typeTransaction.RECHARGE) {
                pst.setInt(9, t.getId_card());
            } else {
                pst.setNull(9, java.sql.Types.INTEGER);
            }

            pst.executeUpdate();
        }
    }

    public boolean isBlockchainCorrupted() throws SQLException {

        String query = "SELECT * FROM blockchain ORDER BY block_index ASC";

        try (PreparedStatement pst = cnx.prepareStatement(query);
             ResultSet rs = pst.executeQuery()) {

            String previousHash = "0000";

            while (rs.next()) {

                String storedPreviousHash = rs.getString("previous_hash");
                String storedCurrentHash = rs.getString("current_hash");

                // Vérifier le lien des blocs
                if (!storedPreviousHash.equals(previousHash)) {
                    return true; // Blockchain corrompue
                }

                // Recalcul du hash
                int idTransaction = rs.getInt("id_transaction");
                int walletSource = rs.getInt("wallet_source");
                int walletDest = rs.getInt("wallet_destination");
                double montant = rs.getDouble("montant");
                String type = rs.getString("type");
                int idCard = rs.getInt("id_card");

                String dataToHash = String.join("|",
                        String.valueOf(idTransaction),
                        String.valueOf(walletSource),
                        String.valueOf(walletDest),
                        String.valueOf(montant),
                        type,
                        String.valueOf(idCard),
                        storedPreviousHash
                );

                String recalculatedHash = generateHash(dataToHash);

                if (!recalculatedHash.equals(storedCurrentHash)) {
                    return true; // Hash modifié
                }

                previousHash = storedCurrentHash;
            }
        }

        return false; // Blockchain valide
    }
    public void verifyBlockchain() throws SQLException {

        String query = "SELECT * FROM blockchain ORDER BY block_index ASC";

        try (PreparedStatement pst = cnx.prepareStatement(query);
             ResultSet rs = pst.executeQuery()) {

            String previousHash = "0000";
            int expectedIndex = 1;

            while (rs.next()) {

                int idBlock = rs.getInt("id_block");
                int idTransaction = rs.getInt("id_transaction");
                int blockIndex = rs.getInt("block_index");

                int oldWalletSource = rs.getInt("wallet_source");
                int oldWalletDest = rs.getInt("wallet_destination");
                double oldMontant = rs.getDouble("montant");
                String oldType = rs.getString("type");
                int oldIdCard = rs.getInt("id_card");

                String storedPreviousHash = rs.getString("previous_hash");
                String storedCurrentHash = rs.getString("current_hash");

                // =====================================================
                // 🔴 1️⃣ BLOC MANQUANT (ordre cassé)
                // =====================================================
                if (blockIndex != expectedIndex) {

                    notificationService.createNotification(
                            null,
                            NotificationType.BLOCKCHAIN_CORRUPTED,
                            "🚨 BLOC MANQUANT DÉTECTÉ\n\n" +
                                    "Index attendu: " + expectedIndex +
                                    "\nIndex trouvé: " + blockIndex +
                                    "\n👉 Un bloc a été supprimé."
                    );
                }

                // =====================================================
                // 🔎 2️⃣ CHAÎNAGE HASH
                // =====================================================
                if (!storedPreviousHash.equals(previousHash)) {

                    notificationService.createNotification(
                            null,
                            NotificationType.BLOCKCHAIN_CORRUPTED,
                            "🚨 CHAÎNE BRISÉE DÉTECTÉE\n\n" +
                                    "Bloc #" + blockIndex +
                                    " a un previous_hash incorrect."
                    );
                }

                // =====================================================
                // 🔎 3️⃣ VÉRIFIER SI TRANSACTION EXISTE
                // =====================================================
                String checkSql =
                        "SELECT * FROM transaction WHERE id_transaction = ?";

                try (PreparedStatement check = cnx.prepareStatement(checkSql)) {

                    check.setInt(1, idTransaction);
                    ResultSet trs = check.executeQuery();

                    // ================= SUPPRESSION =================
                    if (!trs.next()) {

                        notificationService.createNotification(
                                idTransaction,
                                NotificationType.DELETE_DETECTED,
                                "🚨 TRANSACTION SUPPRIMÉE\n\n" +
                                        "ID Transaction: " + idTransaction +
                                        "\nWallet Source: " + oldWalletSource +
                                        "\nWallet Destination: " + oldWalletDest +
                                        "\nMontant: " + oldMontant +
                                        "\nType: " + oldType +
                                        (oldIdCard != 0 ? "\nID Card: " + oldIdCard : "")
                        );
                    }

                    // ================= MODIFICATION =================
                    else {

                        int newWalletSource = trs.getInt("id_wallet_source");
                        int newWalletDest = trs.getInt("id_wallet_destination");
                        double newMontant = trs.getDouble("montant");
                        String newType = trs.getString("type");
                        int newIdCard = trs.getInt("id_card");

                        boolean isModified =
                                oldWalletSource != newWalletSource ||
                                        oldWalletDest != newWalletDest ||
                                        Double.compare(oldMontant, newMontant) != 0 ||
                                        !oldType.equals(newType) ||
                                        oldIdCard != newIdCard;

                        if (isModified) {

                            notificationService.createNotification(
                                    idTransaction,
                                    NotificationType.UPDATE_DETECTED,
                                    "⚠ TRANSACTION MODIFIÉE\n\n" +

                                            "🔴 ANCIENNES DONNÉES:\n" +
                                            "Wallet Source: " + oldWalletSource +
                                            "\nWallet Destination: " + oldWalletDest +
                                            "\nMontant: " + oldMontant +
                                            "\nType: " + oldType +
                                            (oldIdCard != 0 ? "\nID Card: " + oldIdCard : "") +

                                            "\n\n🟢 NOUVELLES DONNÉES:\n" +
                                            "Wallet Source: " + newWalletSource +
                                            "\nWallet Destination: " + newWalletDest +
                                            "\nMontant: " + newMontant +
                                            "\nType: " + newType +
                                            (newIdCard != 0 ? "\nID Card: " + newIdCard : "")
                            );
                        }
                    }
                }

                // =====================================================
                // 🔥 4️⃣ VÉRIFICATION HASH COMPLET (très important)
                // =====================================================
                String dataToHash = String.join("|",
                        String.valueOf(idTransaction),
                        String.valueOf(oldWalletSource),
                        String.valueOf(oldWalletDest),
                        String.valueOf(oldMontant),
                        oldType,
                        String.valueOf(oldIdCard),
                        storedPreviousHash
                );

                String recalculatedHash = generateHash(dataToHash);

                if (!recalculatedHash.equals(storedCurrentHash)) {

                    notificationService.createNotification(
                            null,
                            NotificationType.BLOCKCHAIN_CORRUPTED,
                            "🚨 HASH DU BLOC #" + blockIndex + " INCORRECT\n\n" +
                                    "La blockchain a été modifiée."
                    );
                }


                previousHash = storedCurrentHash;
                expectedIndex++;
            }
        }
    }


}
