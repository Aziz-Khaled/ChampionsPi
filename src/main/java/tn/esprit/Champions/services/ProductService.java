package tn.esprit.Champions.services;

import tn.esprit.Champions.models.Product;
import tn.esprit.Champions.models.ProductCategory;
import tn.esprit.Champions.models.ProductStatus;
import tn.esprit.Champions.utils.DbConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProductService implements CRUD<Product> {

    private Connection cnx;

    public ProductService() {
        this.cnx = DbConnection.getInstance().getCnx();
    }

    @Override
    public void insertOne(Product product) throws SQLException {
        if (cnx == null)
            throw new SQLException("Connexion à la base de données non établie.");
        String query = "INSERT INTO product (name, description, price, discount_price, brand, avg_rating, image_url, stock, category, status, user_id, created_at, updated_at) "
                +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = cnx.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, product.getName());
            pstmt.setString(2, product.getDescription());
            pstmt.setDouble(3, product.getPrice());
            pstmt.setDouble(4, product.getDiscountPrice());
            pstmt.setString(5, product.getBrand());
            pstmt.setDouble(6, product.getAvgRating());
            pstmt.setString(7, product.getImageUrl());
            pstmt.setInt(8, product.getStock());
            pstmt.setString(9, product.getCategory().name());
            pstmt.setString(10, product.getStatus().name());
            pstmt.setInt(11, product.getUserId());
            pstmt.setTimestamp(12, Timestamp.valueOf(product.getCreatedAt()));
            pstmt.setTimestamp(13, Timestamp.valueOf(product.getUpdatedAt()));

            pstmt.executeUpdate();

            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    product.setId(generatedKeys.getLong(1));
                }
            }
        }
    }

    @Override
    public void updateOne(Product product) throws SQLException {
        if (cnx == null)
            throw new SQLException("Connexion à la base de données non établie.");
        String query = "UPDATE product SET name = ?, description = ?, price = ?, discount_price = ?, brand = ?, avg_rating = ?, image_url = ?, stock = ?, category = ?, status = ?, user_id = ?, updated_at = ? WHERE id = ?";
        try (PreparedStatement pstmt = cnx.prepareStatement(query)) {
            pstmt.setString(1, product.getName());
            pstmt.setString(2, product.getDescription());
            pstmt.setDouble(3, product.getPrice());
            pstmt.setDouble(4, product.getDiscountPrice());
            pstmt.setString(5, product.getBrand());
            pstmt.setDouble(6, product.getAvgRating());
            pstmt.setString(7, product.getImageUrl());
            pstmt.setInt(8, product.getStock());
            pstmt.setString(9, product.getCategory().name());
            pstmt.setString(10, product.getStatus().name());
            pstmt.setInt(11, product.getUserId());
            pstmt.setTimestamp(12, Timestamp.valueOf(product.getUpdatedAt()));
            pstmt.setLong(13, product.getId());

            pstmt.executeUpdate();
        }
    }

    @Override
    public void deleteOne(Product product) throws SQLException {
        if (cnx == null)
            throw new SQLException("Connexion à la base de données non établie.");
        String query = "DELETE FROM product WHERE id = ?";
        try (PreparedStatement pstmt = cnx.prepareStatement(query)) {
            pstmt.setLong(1, product.getId());
            pstmt.executeUpdate();
        }
    }

    public void decrementStock(Long productId, int quantity) throws SQLException {
        if (cnx == null)
            throw new SQLException("Connexion à la base de données non établie.");
        String query = "UPDATE product SET stock = stock - ? WHERE id = ? AND stock >= ?";
        try (PreparedStatement pstmt = cnx.prepareStatement(query)) {
            pstmt.setInt(1, quantity);
            pstmt.setLong(2, productId);
            pstmt.setInt(3, quantity);
            int rowsUpdated = pstmt.executeUpdate();
            if (rowsUpdated == 0) {
                throw new SQLException("Stock insuffisant ou produit introuvable.");
            }
        }
    }

    @Override
    public List<Product> SelectAll() throws SQLException {
        if (cnx == null)
            throw new SQLException("Connexion à la base de données non établie.");
        List<Product> products = new ArrayList<>();
        // Sort by created_at DESC to show newest products first
        String query = "SELECT * FROM product ORDER BY created_at DESC";
        try (Statement stmt = cnx.createStatement();
                ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                Product product = new Product(
                        rs.getLong("id"),
                        rs.getString("name"),
                        rs.getString("description"),
                        rs.getDouble("price"),
                        rs.getDouble("discount_price"),
                        rs.getString("brand"),
                        rs.getDouble("avg_rating"),
                        rs.getString("image_url"),
                        rs.getInt("stock"),
                        ProductCategory.valueOf(rs.getString("category")),
                        ProductStatus.valueOf(rs.getString("status")),
                        rs.getInt("user_id"),
                        rs.getTimestamp("created_at").toLocalDateTime(),
                        rs.getTimestamp("updated_at").toLocalDateTime());
                products.add(product);
            }
        }
        return products;
    }
}
