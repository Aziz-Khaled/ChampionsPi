package tn.esprit.Champions.services;

import tn.esprit.Champions.models.OrderMode;
import tn.esprit.Champions.models.Trade;
import tn.esprit.Champions.models.TradeType;
import tn.esprit.Champions.utils.DbConnection;
import tn.esprit.Champions.models.Status;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TradeService implements CRUD<Trade> {

    private Connection cnx ;


    public TradeService() {
        cnx = DbConnection.getInstance().getCnx();
    }


    @Override
    public void insertOne(Trade trade) throws SQLException {
        String req ="INSERT INTO `trade`( `user_id`, `asset_id`, `trade_type`, `order_mode`, `price`, `quantity`, `status`, `created_at`, `executed_at`, `id_transaction`)" +
                " VALUES (?,?,?,?,?,?,?,?,?,?)";

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

        ps.setInt(10,trade.getId_transaction());

        ps.executeUpdate();





    }

    @Override
    public void updateOne(Trade trade) throws SQLException {
        String req =" UPDATE `trade` SET `user_id`=?,`asset_id`=?,`trade_type`=?,`order_mode`=?,`price`=?,`quantity`=?,`status`=?,`created_at`=?,`executed_at`=?,`id_transaction`=?WHERE id = ?";
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
        ps.setInt(10,trade.getId_transaction());
        ps.setInt(11,trade.getId());

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
                    executedAtTs != null ? executedAtTs.toLocalDateTime() : null,
                    rs.getInt("id_transaction")
            );

            trades.add(trade);
        }

        return trades;
    }



}
