package tn.esprit.Champions.utils;

import tn.esprit.Champions.models.OrderItem;
import tn.esprit.Champions.models.Product;

import java.util.ArrayList;
import java.util.List;

public class ShoppingCart {
    private static ShoppingCart instance;
    private final List<OrderItem> items;

    private ShoppingCart() {
        items = new ArrayList<>();
    }

    public static ShoppingCart getInstance() {
        if (instance == null) {
            instance = new ShoppingCart();
        }
        return instance;
    }

    public List<OrderItem> getItems() {
        return items;
    }

    public void addProduct(Product product, int quantity) {
        // Build an OrderItem for the cart
        OrderItem item = new OrderItem();
        item.setProduct(product);
        item.setQuantity(quantity);
        item.setUnitPrice(product.getPrice());
        item.setSubTotal(product.getPrice() * quantity);

        // Check if product already in cart
        for (OrderItem existingItem : items) {
            if (existingItem.getProduct().getId().equals(product.getId())) {
                existingItem.setQuantity(existingItem.getQuantity() + quantity);
                existingItem.setSubTotal(existingItem.getUnitPrice() * existingItem.getQuantity());
                return;
            }
        }

        items.add(item);
    }

    public void removeItem(OrderItem item) {
        items.remove(item);
    }

    public double getTotal() {
        return items.stream().mapToDouble(OrderItem::getSubTotal).sum();
    }

    public void clear() {
        items.clear();
    }
}
