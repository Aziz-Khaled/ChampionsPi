package tn.esprit.Champions.models;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

public class Product {

    private Long id;
    private String name;
    private String description;
    private double price;
    private double discountPrice;
    private String brand;
    private double avgRating;
    private String imageUrl;
    private int stock;
    private ProductCategory category;
    private ProductStatus status;
    private int userId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Product() {
    }

    public Product(Long id, String name, String description, double price, double discountPrice,
                   String brand, double avgRating, String imageUrl,
                   int stock, ProductCategory category, ProductStatus status,
                   int userId, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.price = price;
        this.discountPrice = discountPrice;
        this.brand = brand;
        this.avgRating = avgRating;
        this.imageUrl = imageUrl;
        this.stock = stock;
        this.category = category;
        this.status = status;
        this.userId = userId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // Getters & Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public double getDiscountPrice() {
        return discountPrice;
    }

    public void setDiscountPrice(double discountPrice) {
        this.discountPrice = discountPrice;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public double getAvgRating() {
        return avgRating;
    }

    public void setAvgRating(double avgRating) {
        this.avgRating = avgRating;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public int getStock() {
        return stock;
    }

    public void setStock(int stock) {
        this.stock = stock;
    }

    public ProductCategory getCategory() {
        return category;
    }

    public void setCategory(ProductCategory category) {
        this.category = category;
    }

    public ProductStatus getStatus() {
        return status;
    }

    public void setStatus(ProductStatus status) {
        this.status = status;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
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

    @Override
    public String toString() {
        return "Product{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", price=" + price +
                ", brand='" + brand + '\'' +
                ", imageUrl='" + imageUrl + '\'' +
                ", stock=" + stock +
                ", category=" + category +
                ", status=" + status +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Product product))
            return false;
        return Objects.equals(id, product.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}