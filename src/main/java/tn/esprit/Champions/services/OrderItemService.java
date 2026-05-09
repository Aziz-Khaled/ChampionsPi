package tn.esprit.Champions.services;

import tn.esprit.Champions.models.Order;
import tn.esprit.Champions.models.OrderItem;
import tn.esprit.Champions.models.Product;
import tn.esprit.Champions.utils.DbConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class OrderItemService implements CRUD<OrderItem> {

    private Connection cnx;

    public OrderItemService() {
        this.cnx = DbConnection.getInstance().getCnx();
    }

    @Override
    public void insertOne(OrderItem item) throws SQLException {
        if (cnx == null)
            throw new SQLException("Connexion à la base de données non établie.");
        String query = "INSERT INTO order_item (order_id, product_id, quantity, unit_price, sub_total, discount_applied) "
                +
                "VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = cnx.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, item.getOrder().getId());
            pstmt.setInt(2, item.getProduct().getId());
            pstmt.setInt(3, item.getQuantity());
            pstmt.setBigDecimal(4, item.getUnitPrice());
            pstmt.setBigDecimal(5, item.getSubTotal());
            pstmt.setBigDecimal(6, item.getDiscountApplied() != null ? item.getDiscountApplied() : java.math.BigDecimal.ZERO);

            pstmt.executeUpdate();

            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    item.setId(generatedKeys.getInt(1));
                }
            }
        }
    }

    @Override
    public void updateOne(OrderItem item) throws SQLException {
        if (cnx == null)
            throw new SQLException("Connexion à la base de données non établie.");
        String query = "UPDATE order_item SET order_id = ?, product_id = ?, quantity = ?, unit_price = ?, sub_total = ?, discount_applied = ? WHERE id = ?";
        try (PreparedStatement pstmt = cnx.prepareStatement(query)) {
            pstmt.setInt(1, item.getOrder().getId());
            pstmt.setInt(2, item.getProduct().getId());
            pstmt.setInt(3, item.getQuantity());
            pstmt.setBigDecimal(4, item.getUnitPrice());
            pstmt.setBigDecimal(5, item.getSubTotal());
            pstmt.setBigDecimal(6, item.getDiscountApplied());
            pstmt.setInt(7, item.getId());

            pstmt.executeUpdate();
        }
    }

    @Override
    public void deleteOne(OrderItem item) throws SQLException {
        if (cnx == null)
            throw new SQLException("Connexion à la base de données non établie.");
        String query = "DELETE FROM order_item WHERE id = ?";
        try (PreparedStatement pstmt = cnx.prepareStatement(query)) {
            pstmt.setInt(1, item.getId());
            pstmt.executeUpdate();
        }
    }

    @Override
    public List<OrderItem> SelectAll() throws SQLException {
        if (cnx == null)
            throw new SQLException("Connexion à la base de données non établie.");
        List<OrderItem> items = new ArrayList<>();
        String query = "SELECT oi.*, o.id as oid, p.id as pid FROM order_item oi " +
                "JOIN `order` o ON oi.order_id = o.id " +
                "JOIN product p ON oi.product_id = p.id";
        try (Statement stmt = cnx.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                // Note: Simplified loading of related objects.
                // In a real app, you might want to fetch full objects or use a DAO/Repository
                // pattern.
                Order order = new Order();
                order.setId(rs.getInt("order_id"));

                Product product = new Product();
                product.setId(rs.getInt("product_id"));

                OrderItem item = new OrderItem(
                        rs.getInt("id"),
                        order,
                        product,
                        rs.getInt("quantity"),
                        rs.getBigDecimal("unit_price"),
                        rs.getBigDecimal("sub_total"),
                        rs.getBigDecimal("discount_applied"));
                items.add(item);
            }
        }
        return items;
    }
}