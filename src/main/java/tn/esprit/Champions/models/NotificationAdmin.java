package tn.esprit.Champions.models;

import java.time.LocalDateTime;

public class NotificationAdmin {

    private int idNotification;

    private Integer idTransaction; // peut être NULL (ex: BLOCKCHAIN_CORRUPTED)

    private NotificationType typeNotification; // 👈 enum

    private String message;

    private LocalDateTime createdAt;

    // 🔹 Constructeur vide
    public NotificationAdmin() {
    }

    // 🔹 Constructeur pratique
    public NotificationAdmin(Integer idTransaction,
                        NotificationType typeNotification,
                        String message) {
        this.idTransaction = idTransaction;
        this.typeNotification = typeNotification;
        this.message = message;
        this.createdAt = LocalDateTime.now();
    }

    // 🔹 Getters & Setters

    public int getIdNotification() {
        return idNotification;
    }

    public void setIdNotification(int idNotification) {
        this.idNotification = idNotification;
    }

    public Integer getIdTransaction() {
        return idTransaction;
    }

    public void setIdTransaction(Integer idTransaction) {
        this.idTransaction = idTransaction;
    }

    public NotificationType getTypeNotification() {
        return typeNotification;
    }

    public void setTypeNotification(NotificationType typeNotification) {
        this.typeNotification = typeNotification;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}