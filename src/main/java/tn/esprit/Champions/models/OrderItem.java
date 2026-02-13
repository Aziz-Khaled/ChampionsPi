package tn.esprit.Champions.models;

import java.math.BigDecimal;
import java.util.Objects;

public class OrderItem {

    private int id;
    private Order order;
    private Product product;
    private int quantity;
    private double unitPrice;
    private double subTotal;
    private double discountApplied;

    public OrderItem() {
    }

    public OrderItem(int id, Order order, Product product,
            int quantity, double unitPrice, double subTotal, double discountApplied) {
        this.id = id;
        this.order = order;
        this.product = product;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.subTotal = subTotal;
        this.discountApplied = discountApplied;
    }

    // Getters & Setters

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Order getOrder() {
        return order;
    }

    public void setOrder(Order order) {
        this.order = order;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public double getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(double unitPrice) {
        this.unitPrice = unitPrice;
    }

    public double getSubTotal() {
        return subTotal;
    }

    public void setSubTotal(double subTotal) {
        this.subTotal = subTotal;
    }

    public double getDiscountApplied() {
        return discountApplied;
    }

    public void setDiscountApplied(double discountApplied) {
        this.discountApplied = discountApplied;
    }

    @Override
    public String toString() {
        return "OrderItem{" +
                "id=" + id +
                ", quantity=" + quantity +
                ", subTotal=" + subTotal +
                ", discountApplied=" + discountApplied +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof OrderItem that))
            return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}