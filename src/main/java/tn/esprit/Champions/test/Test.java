package tn.esprit.Champions.test;

import tn.esprit.Champions.models.*;
import tn.esprit.Champions.services.AssetService;
import tn.esprit.Champions.utils.DbConnection;

import java.sql.SQLException;
import java.time.LocalDateTime;

import static tn.esprit.Champions.models.AssetType.CRYPTO;
import static tn.esprit.Champions.models.Market.BINANCE;
import static tn.esprit.Champions.models.Status.ACTIVE;


public class Test {




    public static void main(String[] args) throws SQLException {

        DbConnection.getInstance() ;
        Utilisateur user = new Utilisateur(2 ,"kouki", "jesser", "123456kouki", "26691259", "11453991", "", Role.ADMIN);


        Asset asset1 = new Asset (0,"BTC", "Bitcoin", CRYPTO , BINANCE , 2.5 , ACTIVE , LocalDateTime.now(),LocalDateTime.now(), 1  );

        AssetService assetService = new AssetService();
        try {
            assetService.insertOne(asset1);

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
