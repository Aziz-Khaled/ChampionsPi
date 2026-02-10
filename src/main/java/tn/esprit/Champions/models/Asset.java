package tn.esprit.Champions.models;



import java.time.LocalDateTime;
import java.util.Objects;

public class Asset {

    private int id;
    private String symbol;
    private String name;
    private AssetType type;
    private Market market;
    private Double currentPrice;
    private Status status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private int user_id ;

    public Asset() {
    }

    public Asset(int id, String symbol, String name, AssetType type, Market market, Double currentPrice, Status status, LocalDateTime createdAt, LocalDateTime updatedAt, int user_id) {
        this.id = id;
        this.symbol = symbol;
        this.name = name;
        this.type = type;
        this.market = market;
        this.currentPrice = currentPrice;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.user_id = user_id;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public AssetType getType() {
        return type;
    }

    public void setType(AssetType type) {
        this.type = type;
    }

    public Market getMarket() {
        return market;
    }

    public void setMarket(Market market) {
        this.market = market;
    }

    public Double getCurrentPrice() {
        return currentPrice;
    }

    public void setCurrentPrice(Double currentPrice) {
        this.currentPrice = currentPrice;
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

    public int getUser_id() {
        return user_id;
    }

    public void setUser_id(int user_id) {
        this.user_id = user_id;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Asset asset)) return false;
        return id == asset.id && user_id == asset.user_id && Objects.equals(symbol, asset.symbol) && Objects.equals(name, asset.name) && type == asset.type && market == asset.market && Objects.equals(currentPrice, asset.currentPrice) && status == asset.status && Objects.equals(createdAt, asset.createdAt) && Objects.equals(updatedAt, asset.updatedAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, symbol, name, type, market, currentPrice, status, createdAt, updatedAt, user_id);
    }

    @Override
    public String toString() {
        return "Asset{" +
                "id=" + id +
                ", symbol='" + symbol + '\'' +
                ", name='" + name + '\'' +
                ", type=" + type +
                ", market=" + market +
                ", currentPrice=" + currentPrice +
                ", status=" + status +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                ", user_id=" + user_id +
                '}';
    }
}
