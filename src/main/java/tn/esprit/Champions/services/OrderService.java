package tn.esprit.Champions.services;

import tn.esprit.Champions.models.Order;
import tn.esprit.Champions.models.OrderStatus;
import tn.esprit.Champions.utils.DbConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class OrderService implements CRUD<Order> {

    private Connection cnx;

    public OrderService() {
        this.cnx = DbConnection.getInstance().getCnx();
    }

    @Override
    public void insertOne(Order order) throws SQLException {
        if (cnx == null)
            throw new SQLException("Connexion à la base de données non établie.");
        String query = "INSERT INTO `orders` (user_id, order_date, total_amount, status, shipping_address, payment_method, phone_number) "
                +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = cnx.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, order.getUserId());
            pstmt.setTimestamp(2, Timestamp.valueOf(order.getOrderDate()));
            pstmt.setDouble(3, order.getTotalAmount());
            pstmt.setString(4, order.getStatus().name());
            pstmt.setString(5, order.getShippingAddress());
            pstmt.setString(6, order.getPaymentMethod());
            pstmt.setString(7, order.getPhoneNumber());

            pstmt.executeUpdate();

            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    order.setId(generatedKeys.getInt(1));
                }
            }
        }
    }

    @Override
    public void updateOne(Order order) throws SQLException {
        if (cnx == null)
            throw new SQLException("Connexion à la base de données non établie.");
        String query = "UPDATE `orders` SET user_id = ?, order_date = ?, total_amount = ?, status = ?, shipping_address = ?, payment_method = ?, phone_number = ? WHERE id = ?";
        try (PreparedStatement pstmt = cnx.prepareStatement(query)) {
            pstmt.setInt(1, order.getUserId());
            pstmt.setTimestamp(2, Timestamp.valueOf(order.getOrderDate()));
            pstmt.setDouble(3, order.getTotalAmount());
            pstmt.setString(4, order.getStatus().name());
            pstmt.setString(5, order.getShippingAddress());
            pstmt.setString(6, order.getPaymentMethod());
            pstmt.setString(7, order.getPhoneNumber());
            pstmt.setInt(8, order.getId());

            pstmt.executeUpdate();
        }
    }

    @Override
    public void deleteOne(Order order) throws SQLException {
        if (cnx == null)
            throw new SQLException("Connexion à la base de données non établie.");
        String query = "DELETE FROM `orders` WHERE id = ?";
        try (PreparedStatement pstmt = cnx.prepareStatement(query)) {
            pstmt.setInt(1, order.getId());
            pstmt.executeUpdate();
        }
    }

    @Override
    public List<Order> SelectAll() throws SQLException {
        if (cnx == null)
            throw new SQLException("Connexion à la base de données non établie.");
        List<Order> orders = new ArrayList<>();
        String query = "SELECT * FROM `orders`";
        try (Statement stmt = cnx.createStatement();
                ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                Order order = new Order(
                        rs.getInt("id"),
                        rs.getInt("user_id"),
                        rs.getTimestamp("order_date").toLocalDateTime(),
                        rs.getDouble("total_amount"),
                        OrderStatus.valueOf(rs.getString("status")),
                        rs.getString("shipping_address"),
                        rs.getString("payment_method"),
                        rs.getString("phone_number"));
                orders.add(order);
            }
        }
        return orders;
    }
}
