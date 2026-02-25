package tn.esprit.Champions.services;

import tn.esprit.Champions.models.NotificationAdmin;
import tn.esprit.Champions.models.NotificationType;
import tn.esprit.Champions.utils.DbConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class NotificationAdminService {

    private Connection cnx;

    public NotificationAdminService() {
        cnx = DbConnection.getInstance().getCnx();
    }

    // =====================================================
    // 🔹 1️⃣ CREER UNE NOTIFICATION
    // =====================================================
    public void createNotification(Integer idTransaction,
                                   NotificationType type,
                                   String message) throws SQLException {

        // 🔎 1. Si ID Transaction existe, on vérifie classiquement
        if (idTransaction != null) {
            if (notificationExists(idTransaction, type)) {
                return;
            }
        } else {
            // 🔎 2. Si ID Transaction est NULL (Corruption blockchain),
            // on vérifie si ce message exact existe déjà pour éviter le spam
            if (messageExists(message, type)) {
                return;
            }
        }

        String sql = """
            INSERT INTO notification
            (id_transaction, type_notification, message, created_at, is_read)
            VALUES (?, ?, ?, NOW(), 0)
            """;

        try (PreparedStatement pst = cnx.prepareStatement(sql)) {
            if (idTransaction == null) {
                pst.setNull(1, Types.INTEGER);
            } else {
                pst.setInt(1, idTransaction);
            }
            pst.setString(2, type.name());
            pst.setString(3, message);
            pst.executeUpdate();
        }
    }

    // Nouvelle méthode pour vérifier les messages globaux (sans ID TX)
    public boolean messageExists(String message, NotificationType type) throws SQLException {
        String sql = "SELECT COUNT(*) FROM notification WHERE message = ? AND type_notification = ?";
        try (PreparedStatement pst = cnx.prepareStatement(sql)) {
            pst.setString(1, message);
            pst.setString(2, type.name());
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }
        return false;
    }

    // =====================================================
    // 🔹 2️⃣ RECUPERER TOUTES LES NOTIFICATIONS
    // =====================================================
    public List<NotificationAdmin> getAllNotifications() throws SQLException {

        List<NotificationAdmin> list = new ArrayList<>();

        String sql = """
                SELECT *
                FROM notification
                ORDER BY created_at DESC
                """;

        try (PreparedStatement pst = cnx.prepareStatement(sql);
             ResultSet rs = pst.executeQuery()) {

            while (rs.next()) {

                NotificationAdmin n = new NotificationAdmin();

                n.setIdNotification(rs.getInt("id_notification"));

                int idTransaction = rs.getInt("id_transaction");
                if (rs.wasNull()) {
                    n.setIdTransaction(null);
                } else {
                    n.setIdTransaction(idTransaction);
                }

                n.setTypeNotification(
                        NotificationType.valueOf(
                                rs.getString("type_notification")
                        )
                );

                n.setMessage(rs.getString("message"));

                n.setCreatedAt(
                        rs.getTimestamp("created_at")
                                .toLocalDateTime()
                );

                list.add(n);
            }
        }

        return list;
    }

    // =====================================================
    // 🔹 3️⃣ VERIFIER SI NOTIFICATION EXISTE
    // =====================================================
    public boolean notificationExists(int idTransaction,
                                      NotificationType type) throws SQLException {

        String sql = """
                SELECT COUNT(*)
                FROM notification
                WHERE id_transaction = ?
                AND type_notification = ?
                """;

        try (PreparedStatement pst = cnx.prepareStatement(sql)) {

            pst.setInt(1, idTransaction);
            pst.setString(2, type.name());

            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }

        return false;
    }

    // =====================================================
    // 🔹 4️⃣ COMPTER NOTIFICATIONS NON LUES
    // =====================================================
    public int getUnreadCount() throws SQLException {

        String sql = """
                SELECT COUNT(*)
                FROM notification
                WHERE is_read = 0
                """;

        try (PreparedStatement pst = cnx.prepareStatement(sql);
             ResultSet rs = pst.executeQuery()) {

            if (rs.next()) {
                return rs.getInt(1);
            }
        }

        return 0;
    }

    // =====================================================
    // 🔹 5️⃣ MARQUER TOUT COMME LU
    // =====================================================
    public void markAllAsRead() throws SQLException {

        String sql = """
                UPDATE notification
                SET is_read = 1
                WHERE is_read = 0
                """;

        try (PreparedStatement pst = cnx.prepareStatement(sql)) {
            pst.executeUpdate();
        }
    }

}