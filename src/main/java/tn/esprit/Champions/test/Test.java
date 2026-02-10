package tn.esprit.Champions.test;

import tn.esprit.Champions.models.statutWallet;
import tn.esprit.Champions.models.typeWallet;
import tn.esprit.Champions.models.wallet;
import tn.esprit.Champions.services.WalletService;

import java.sql.SQLException;
import java.util.List;

public class Test {

    public static void main(String[] args) {

        WalletService wService = new WalletService();

        try {

//            wallet wal1 = new wallet(typeWallet.FIAT, "dt", statutWallet.actif);
//            wService.insertOne(wal1);
//            System.out.println("Wallet inséré");


            int idWallet = 1;


            wallet walUpdate = new wallet();
            walUpdate.setIdWallet(idWallet);
            walUpdate.setTypeWallet(typeWallet.fiat);
            walUpdate.setCategorie("btc");
            walUpdate.setSolde(900.0);
            walUpdate.setStatut(statutWallet.actif);

            // 3. UPDATE
            wService.updateOne(walUpdate);
            System.out.println("Wallet modifié avec succès");
            wService.deleteOne(walUpdate);
            System.out.println("Wallet supprimer avec succès");

            List<wallet> wallets = wService.SelectAll();

            for (wallet w : wallets) {
                System.out.println("ID: " + w.getIdWallet());
                System.out.println("Type: " + w.getTypeWallet());
                System.out.println("Categorie: " + w.getCategorie());
                System.out.println("Solde: " + w.getSolde());
                System.out.println("----------------------");
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}