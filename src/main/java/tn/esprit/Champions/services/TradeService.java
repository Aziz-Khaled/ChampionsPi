package tn.esprit.Champions.services;

import tn.esprit.Champions.models.OrderMode;
import tn.esprit.Champions.models.Trade;
import tn.esprit.Champions.models.TradeType;
import tn.esprit.Champions.utils.DbConnection;
import tn.esprit.Champions.models.Status;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class TradeService implements CRUD<Trade> {

    private Connection cnx ;


    public TradeService() {
        cnx = DbConnection.getInstance().getCnx();
    }


    @Override
    public void insertOne(Trade trade) throws SQLException {
        String req ="INSERT INTO `trade`( `user_id`, `asset_id`, `trade_type`, `order_mode`, `price`, `quantity`, `status`, `created_at`, `executed_at`)" +
                " VALUES (?,?,?,?,?,?,?,?,?)";

        PreparedStatement ps =  cnx.prepareStatement(req);

        ps.setInt(1,trade.getId_user());
        ps.setInt(2,trade.getAsset_id());
        ps.setString(3,trade.getTradeType().name());
        ps.setString(4,trade.getOrderMode().name());
        ps.setDouble(5,trade.getPrice());
        ps.setDouble(6,trade.getQuantity());
        ps.setString(7,trade.getStatus().name());
        ps.setTimestamp(8, Timestamp.valueOf(trade.getCreatedAt()));

        if (trade.getExecutedAt() != null) {
            ps.setTimestamp(9, Timestamp.valueOf(trade.getExecutedAt()));
        } else {
            ps.setNull(9, Types.TIMESTAMP);
        }



        ps.executeUpdate();





    }

    @Override
    public void updateOne(Trade trade) throws SQLException {
        String req =" UPDATE `trade` SET `user_id`=?,`asset_id`=?,`trade_type`=?,`order_mode`=?,`price`=?,`quantity`=?,`status`=?,`created_at`=?,`executed_at`=? WHERE id = ?";
        PreparedStatement ps =  cnx.prepareStatement(req);
        ps.setInt(1,trade.getId_user());
        ps.setInt(2,trade.getAsset_id());
        ps.setString(3,trade.getTradeType().name());
        ps.setString(4,trade.getOrderMode().name());
        ps.setDouble(5,trade.getPrice());
        ps.setDouble(6,trade.getQuantity());
        ps.setString(7,trade.getStatus().name());
        ps.setTimestamp(8, Timestamp.valueOf(trade.getCreatedAt()));
        if (trade.getExecutedAt() != null) {
            ps.setTimestamp(9, Timestamp.valueOf(trade.getExecutedAt()));
        } else {
            ps.setNull(9, Types.TIMESTAMP);
        }
        ;

        ps.setInt(10,trade.getId());

        ps.executeUpdate();
    }

    @Override
    public void deleteOne(Trade trade) throws SQLException {
        String req ="DELETE FROM `trade` WHERE id=?";

        PreparedStatement ps =  cnx.prepareStatement(req);

        ps.setInt(1,trade.getId());

        ps.executeUpdate();

    }

    @Override
    public List<Trade> SelectAll() throws SQLException {
        List<Trade> trades = new ArrayList<>();

        String req = "SELECT * FROM `trade` ";
        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(req);

        while (rs.next()) {
            Timestamp executedAtTs = rs.getTimestamp("executed_at");
            Trade trade = new Trade(
                    rs.getInt("id"),
                    rs.getInt("user_id"),
                    rs.getInt("asset_id"),
                    TradeType.valueOf(rs.getString("trade_type")),
                    OrderMode.valueOf(rs.getString("order_mode")),
                    rs.getDouble("price"),
                    rs.getDouble("quantity"),
                    Status.valueOf(rs.getString("status")),
                    rs.getTimestamp("created_at").toLocalDateTime(),
                    executedAtTs != null ? executedAtTs.toLocalDateTime() : null
            );

            trades.add(trade);
        }

        return trades;
    }

    // Utilisé par le BOT pour mettre à jour la trace d'exécution
    public void finalizeLimitOrder(int tradeId, double execPrice) throws SQLException {
        String req = "UPDATE `trade` SET status = 'COMPLETED', executed_at = ?, price = ? WHERE id = ?";
        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
        ps.setDouble(2, execPrice);
        ps.setInt(3, tradeId);
        ps.executeUpdate();
    }

    public List<Trade> selectAll() throws SQLException {
        List<Trade> trades = new ArrayList<>();
        String req = "SELECT * FROM `trade` ORDER BY created_at DESC";
        ResultSet rs = cnx.createStatement().executeQuery(req);
        while (rs.next()) {
            trades.add(mapRowToTrade(rs));
        }
        return trades;
    }

    public List<Trade> selectPendingLimitOrders() throws SQLException {
        List<Trade> trades = new ArrayList<>();
        String req = "SELECT * FROM `trade` WHERE status = 'PENDING' AND order_mode = 'LIMIT'";
        ResultSet rs = cnx.createStatement().executeQuery(req);
        while (rs.next()) {
            trades.add(mapRowToTrade(rs));
        }
        return trades;
    }

    private Trade mapRowToTrade(ResultSet rs) throws SQLException {
        // Safe handling of status field
        String statusStr = rs.getString("status");
        Status status;

        if (statusStr == null || statusStr.trim().isEmpty()) {
            // Default to PENDING if status is null or empty
            status = Status.PENDING;
            System.out.println("Warning: Trade ID " + rs.getInt("id") + " has null/empty status, defaulting to PENDING");
        } else {
            try {
                status = Status.valueOf(statusStr.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                // If status value is invalid, default to PENDING
                status = Status.PENDING;
                System.out.println("Warning: Trade ID " + rs.getInt("id") + " has invalid status '" + statusStr + "', defaulting to PENDING");
            }
        }

        Timestamp executedAtTs = rs.getTimestamp("executed_at");

        return new Trade(
                rs.getInt("id"),
                rs.getInt("user_id"),
                rs.getInt("asset_id"),
                TradeType.valueOf(rs.getString("trade_type")),
                OrderMode.valueOf(rs.getString("order_mode")),
                rs.getDouble("price"),
                rs.getDouble("quantity"),
                status,  // Use the safely parsed status
                rs.getTimestamp("created_at").toLocalDateTime(),
                executedAtTs != null ? executedAtTs.toLocalDateTime() : null
        );
    }


    public List<Trade> getTradesByUserId(int userId) throws SQLException {
        List<Trade> trades = new ArrayList<>();
        String req = "SELECT * FROM `trade` WHERE user_id = ? ORDER BY created_at DESC";

        try (PreparedStatement ps = cnx.prepareStatement(req)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    trades.add(mapRowToTrade(rs));
                }
            }
        }

        return trades;
    }



}