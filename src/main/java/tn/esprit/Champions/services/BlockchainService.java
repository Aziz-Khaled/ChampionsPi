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
import java.util.ArrayList;
import java.util.List;

public class BlockchainService {
    private Connection cnx;
    private NotificationAdminService notificationService;

    public BlockchainService() {
        cnx = DbConnection.getInstance().getCnx();
        notificationService = new NotificationAdminService();
    }

    public Blockchain getLastBlock() throws SQLException {
        String query = "SELECT * FROM blockchain ORDER BY block_index DESC LIMIT 1";
        try (PreparedStatement pst = cnx.prepareStatement(query);
             ResultSet rs = pst.executeQuery()) {
            if (rs.next()) {
                Blockchain block = new Blockchain();
                block.setIdBlock(rs.getInt("id_block"));
                block.setIdTransaction(rs.getInt("id_transaction"));
                block.setBlockIndex(rs.getInt("block_index"));
                block.setPreviousHash(rs.getString("previous_hash"));
                block.setCurrentHash(rs.getString("current_hash"));
                return block;
            }
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
            throw new RuntimeException("Erreur lors de la génération du hash", e);
        }
    }

    public void addBlock(transaction t) throws SQLException {
        if (isBlockchainCorrupted()) {
            throw new RuntimeException("🚨 Blockchain corrompue ! Aucun bloc ne peut être ajouté.");
        }

        Blockchain lastBlock = getLastBlock();
        String previousHash = (lastBlock == null) ? "0000" : lastBlock.getCurrentHash();
        int newIndex = (lastBlock == null) ? 1 : lastBlock.getBlockIndex() + 1;

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
            pst.setInt(9, t.getId_card()); // id_card est 0 si non applicable

            pst.executeUpdate();
        }
    }

    public boolean isBlockchainCorrupted() throws SQLException {
        String query = "SELECT * FROM blockchain ORDER BY block_index ASC";
        try (PreparedStatement pst = cnx.prepareStatement(query);
             ResultSet rs = pst.executeQuery()) {
            String previousHash = "0000";
            while (rs.next()) {
                if (!rs.getString("previous_hash").equals(previousHash)) return true;

                String dataToHash = String.join("|",
                        String.valueOf(rs.getInt("id_transaction")),
                        String.valueOf(rs.getInt("wallet_source")),
                        String.valueOf(rs.getInt("wallet_destination")),
                        String.valueOf(rs.getDouble("montant")),
                        rs.getString("type"),
                        String.valueOf(rs.getInt("id_card")),
                        rs.getString("previous_hash")
                );
                if (!generateHash(dataToHash).equals(rs.getString("current_hash"))) return true;
                previousHash = rs.getString("current_hash");
            }
        }
        return false;
    }

    public void verifyBlockchain() throws SQLException {
        String query = "SELECT * FROM blockchain ORDER BY block_index ASC";
        List<String> missingDetails = new ArrayList<>(); // Pour stocker les détails des blocs manquants
        List<Integer> brokenHashes = new ArrayList<>();

        try (PreparedStatement pst = cnx.prepareStatement(query);
             ResultSet rs = pst.executeQuery()) {

            String previousHash = "0000";
            int expectedIndex = 1;

            while (rs.next()) {
                int idTransaction = rs.getInt("id_transaction");
                int blockIndex = rs.getInt("block_index");
                int oldWalletSrc = rs.getInt("wallet_source");
                int oldWalletDst = rs.getInt("wallet_destination");
                double oldAmount = rs.getDouble("montant");
                String oldType = rs.getString("type");
                int oldIdCard = rs.getInt("id_card");
                String storedPrevHash = rs.getString("previous_hash");
                String storedCurrHash = rs.getString("current_hash");

                // 1. Détection blocs manquants (Si l'index saute, ex: 1 puis 3)
                while (blockIndex > expectedIndex) {
                    missingDetails.add("Index #" + expectedIndex);
                    expectedIndex++;
                }

                // 2. Détection chaîne brisée
                if (!storedPrevHash.equals(previousHash)) {
                    brokenHashes.add(blockIndex);
                }

                // 3. Vérification intégrité Transaction vs Blockchain
                String checkSql = "SELECT * FROM transaction WHERE id_transaction = ?";
                try (PreparedStatement checkPst = cnx.prepareStatement(checkSql)) {
                    checkPst.setInt(1, idTransaction);
                    try (ResultSet trs = checkPst.executeQuery()) {
                        if (!trs.next()) {
                            // Message détaillé pour la suppression
                            String sourceInfo = (oldWalletSrc == 0) ? "Card ID: " + oldIdCard : "Wallet: " + oldWalletSrc;
                            notificationService.createNotification(idTransaction, NotificationType.DELETE_DETECTED,
                                    "🚨 TRANSACTION DISPARUE\n" +
                                            "ID: " + idTransaction + "\n" +
                                            "De: " + sourceInfo + " ➔ Vers: " + oldWalletDst + "\n" +
                                            "Montant: " + oldAmount + " BTC");
                        } else {
                            // Comparaison détaillée pour modification
                            StringBuilder diff = new StringBuilder();
                            if (oldWalletSrc != trs.getInt("id_wallet_source"))
                                diff.append("Source: ").append(oldWalletSrc).append(" ➔ ").append(trs.getInt("id_wallet_source")).append("\n");

                            if (oldWalletDst != trs.getInt("id_wallet_destination"))
                                diff.append("Dest: ").append(oldWalletDst).append(" ➔ ").append(trs.getInt("id_wallet_destination")).append("\n");

                            if (Double.compare(oldAmount, trs.getDouble("montant")) != 0)
                                diff.append("Montant: ").append(oldAmount).append(" ➔ ").append(trs.getDouble("montant")).append("\n");

                            if (!oldType.equals(trs.getString("type")))
                                diff.append("Type: ").append(oldType).append(" ➔ ").append(trs.getString("type")).append("\n");

                            if (diff.length() > 0) {
                                notificationService.createNotification(idTransaction, NotificationType.UPDATE_DETECTED,
                                        "⚠ MODIFICATION FRAUDULEUSE (ID #" + idTransaction + ")\n" + diff.toString());
                            }
                        }
                    }
                }

                // 4. Vérification Hash du contenu
                String dataToHash = String.join("|", String.valueOf(idTransaction), String.valueOf(oldWalletSrc),
                        String.valueOf(oldWalletDst), String.valueOf(oldAmount), oldType,
                        String.valueOf(oldIdCard), storedPrevHash);

                if (!generateHash(dataToHash).equals(storedCurrHash)) {
                    if (!brokenHashes.contains(blockIndex)) brokenHashes.add(blockIndex);
                }

                previousHash = storedCurrHash;
                expectedIndex++;
            }

            // --- Notifications finales ---
            if (!missingDetails.isEmpty()) {
                notificationService.createNotification(null, NotificationType.BLOCKCHAIN_CORRUPTED,
                        "🚨 BLOCS SUPPRIMÉS DE LA CHAÎNE\n" +
                                "Les index suivants ont été effacés : " + String.join(", ", missingDetails));
            }

            if (!brokenHashes.isEmpty()) {
                notificationService.createNotification(null, NotificationType.BLOCKCHAIN_CORRUPTED,
                        "🚨 INTÉGRITÉ COMPROMISE\n" +
                                "Hash invalide ou chaîne rompue aux blocs : " + brokenHashes);
            }
        }
    }
}