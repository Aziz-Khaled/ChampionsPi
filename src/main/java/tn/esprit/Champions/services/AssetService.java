package tn.esprit.Champions.services;

import tn.esprit.Champions.models.Asset;
import tn.esprit.Champions.models.AssetType;
import tn.esprit.Champions.models.Market;
import tn.esprit.Champions.models.Status;
import tn.esprit.Champions.utils.DbConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AssetService implements CRUD <Asset>  {

    private Connection cnx ;

    public AssetService() {
        cnx = DbConnection.getInstance().getCnx();
    }

    @Override
    public void insertOne(Asset asset) throws SQLException {
        String req = "INSERT INTO `asset`(`symbol`, `name`, `type`, `market`, `current_price`, `status`, `created_at`, `updated_at`, `user_id`)" +
                " VALUES ( ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        PreparedStatement ps = cnx.prepareStatement(req);


        ps.setString(1, asset.getSymbol());
        ps.setString(2, asset.getName());
        ps.setString(3, asset.getType().name());
        ps.setString(4, asset.getMarket().name());
        ps.setDouble(5, asset.getCurrentPrice());
        ps.setString(6, asset.getStatus().name());
        ps.setTimestamp(7, Timestamp.valueOf(asset.getCreatedAt()));
        ps.setTimestamp(8, Timestamp.valueOf(asset.getUpdatedAt()));
        ps.setInt(9, asset.getUserId());

        ps.executeUpdate();

    }

    @Override
    public void updateOne(Asset asset) throws SQLException {
        String req ="UPDATE `asset` SET `symbol`=?,`name`=?,`type`=?,`market`=?,`current_price`=?,`status`=?,`created_at`=?,`updated_at`=?,`user_id`=? WHERE id = ?";

        PreparedStatement ps = cnx.prepareStatement(req);

        ps.setString(1, asset.getSymbol());
        ps.setString(2, asset.getName());
        ps.setString(3, asset.getType().name());
        ps.setString(4, asset.getMarket().name());
        ps.setDouble(5, asset.getCurrentPrice());
        ps.setString(6, asset.getStatus().name());
        ps.setTimestamp(7, Timestamp.valueOf(asset.getCreatedAt()));
        ps.setTimestamp(8, Timestamp.valueOf(asset.getUpdatedAt()));
        ps.setInt(9, asset.getUserId());
        ps.setInt(10, asset.getId());

        ps.executeUpdate();

    }

    @Override
    public void deleteOne(Asset asset) throws SQLException {
        String req ="DELETE FROM `asset` WHERE id = ?";

        PreparedStatement ps = cnx.prepareStatement(req);

        ps.setInt(1, asset.getId());

        ps.executeUpdate();


    }

    @Override
    public List<Asset> SelectAll() throws SQLException {

        List<Asset> assets = new ArrayList<>();

        String req = "SELECT * FROM asset";
        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(req);

        while (rs.next()) {

            Asset asset = new Asset(
                    rs.getInt("id"),
                    rs.getString("symbol"),
                    rs.getString("name"),
                    AssetType.valueOf(rs.getString("type")),
                    Market.valueOf(rs.getString("market")),
                    rs.getDouble("current_price"),
                    Status.valueOf(rs.getString("status")),
                    rs.getTimestamp("created_at").toLocalDateTime(),
                    rs.getTimestamp("updated_at").toLocalDateTime(),
                    rs.getInt("user_id")
            );

            assets.add(asset);
        }

        return assets;
    }
    }








