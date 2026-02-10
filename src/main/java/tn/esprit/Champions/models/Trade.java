package tn.esprit.Champions.models;


import java.time.LocalDateTime;
import java.util.Objects;
public class Trade {
    private int id;
    private int id_user;
    private Asset asset;
    private TradeType tradeType;
    private OrderMode orderMode;
    private Double price;
    private Double quantity;
    private Status status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private int id_transaction;
    private int asset_id;

    public Trade() {
    }

    public Trade(int id, int id_user, Asset asset, TradeType tradeType, OrderMode orderMode, Double price, Double quantity, Status status, LocalDateTime createdAt, LocalDateTime updatedAt, int id_transaction, int asset_id) {
        this.id = id;
        this.id_user = id_user;
        this.asset = asset;
        this.tradeType = tradeType;
        this.orderMode = orderMode;
        this.price = price;
        this.quantity = quantity;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.id_transaction = id_transaction;
        this.asset_id = asset_id;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getId_user() {
        return id_user;
    }

    public void setId_user(int id_user) {
        this.id_user = id_user;
    }

    public Asset getAsset() {
        return asset;
    }

    public void setAsset(Asset asset) {
        this.asset = asset;
    }

    public TradeType getTradeType() {
        return tradeType;
    }

    public void setTradeType(TradeType tradeType) {
        this.tradeType = tradeType;
    }

    public OrderMode getOrderMode() {
        return orderMode;
    }

    public void setOrderMode(OrderMode orderMode) {
        this.orderMode = orderMode;
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public Double getQuantity() {
        return quantity;
    }

    public void setQuantity(Double quantity) {
        this.quantity = quantity;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public int getId_transaction() {
        return id_transaction;
    }

    public void setId_transaction(int id_transaction) {
        this.id_transaction = id_transaction;
    }

    public int getAsset_id() {
        return asset_id;
    }

    public void setAsset_id(int asset_id) {
        this.asset_id = asset_id;
    }

    @Override
    public String toString() {
        return "Trade{" +
                "id=" + id +
                ", id_user=" + id_user +
                ", asset=" + asset +
                ", tradeType=" + tradeType +
                ", orderMode=" + orderMode +
                ", price=" + price +
                ", quantity=" + quantity +
                ", status=" + status +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                ", id_transaction=" + id_transaction +
                ", asset_id=" + asset_id +
                '}';
    }
}