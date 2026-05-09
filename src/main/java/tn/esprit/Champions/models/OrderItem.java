package tn.esprit.Champions.models;

import java.math.BigDecimal;
import java.util.Objects;

public class OrderItem {

    private int id;

    private Order order;

    private Product product;

    private int quantity;

    // decimal(10,2) -> BigDecimal
    private BigDecimal unitPrice;

    private BigDecimal subTotal;

    private BigDecimal discountApplied;

    public OrderItem() {
    }

    public OrderItem(
            int id,
            Order order,
            Product product,
            int quantity,
            BigDecimal unitPrice,
            BigDecimal subTotal,
            BigDecimal discountApplied
    ) {
        this.id = id;
        this.order = order;
        this.product = product;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.subTotal = subTotal;
        this.discountApplied = discountApplied;
    }

    // =========================
    // GETTERS & SETTERS
    // =========================

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


    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }


    public BigDecimal getSubTotal() {
        return subTotal;
    }

    public void setSubTotal(BigDecimal subTotal) {
        this.subTotal = subTotal;
    }


    public BigDecimal getDiscountApplied() {
        return discountApplied;
    }

    public void setDiscountApplied(BigDecimal discountApplied) {
        this.discountApplied = discountApplied;
    }

    // =========================
    // toString
    // =========================

    @Override
    public String toString() {
        return "OrderItem{" +
                "id=" + id +
                ", quantity=" + quantity +
                ", unitPrice=" + unitPrice +
                ", subTotal=" + subTotal +
                ", discountApplied=" + discountApplied +
                '}';
    }

    // =========================
    // equals & hashCode
    // =========================

    @Override
    public boolean equals(Object o) {

        if (this == o)
            return true;

        if (!(o instanceof OrderItem))
            return false;

        OrderItem that = (OrderItem) o;

        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}