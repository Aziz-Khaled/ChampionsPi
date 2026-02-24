package tn.esprit.Champions.models;


import java.time.LocalDateTime;

public class Trade {
    private int id;
    private int id_user;
    private TradeType tradeType;
    private OrderMode orderMode;
    private Double price;
    private Double quantity;
    private Status status;
    private int id_transaction;
    private int asset_id;
    private LocalDateTime createdAt;
    private LocalDateTime executedAt;


    public Trade(int CURRENT_USER_ID, int id, TradeType tType, double price, double qty, Status completed) {
    }

    public Trade(
            int id,
            int user_id,
            int asset_id,
            TradeType tradeType,
            OrderMode orderMode,
            double price,
            double quantity,
            Status status,
            LocalDateTime createdAt,
            LocalDateTime executedAt

    ) {
        this.id = id;
        this.id_user = user_id;
        this.asset_id = asset_id;
        this.tradeType = tradeType;
        this.orderMode = orderMode;
        this.price = price;
        this.quantity = quantity;
        this.status = status;
        this.createdAt = createdAt;
        this.executedAt = executedAt;

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

    public LocalDateTime getExecutedAt() {
        return executedAt;
    }

    public void setExecutedAt(LocalDateTime executedAt) {
        this.executedAt = executedAt;
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
                ", tradeType=" + tradeType +
                ", orderMode=" + orderMode +
                ", price=" + price +
                ", quantity=" + quantity +
                ", status=" + status +
                ", createdAt=" + createdAt +
                ", executedAt=" + executedAt +
                ", id_transaction=" + id_transaction +
                ", asset_id=" + asset_id +
                '}';
    }

}