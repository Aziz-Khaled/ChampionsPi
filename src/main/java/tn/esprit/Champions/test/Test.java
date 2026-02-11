package tn.esprit.Champions.test;

import tn.esprit.Champions.models.*;
import tn.esprit.Champions.services.CurrencyService;
import tn.esprit.Champions.services.WalletService;
import tn.esprit.Champions.services.wallet_currencyService;

import java.sql.SQLException;
import java.util.List;
import java.util.Scanner;

public class Test {

    public static void main(String[] args) {

        WalletService wService = new WalletService();
        CurrencyService currencyService = new CurrencyService();
        wallet_currencyService wcService = new wallet_currencyService();

        Scanner scanner = new Scanner(System.in);

        try {
            // 1️⃣ Créer un wallet exemple (tu peux en créer plusieurs)
            wallet wal1 = new wallet(1, typeWallet.fiat, statutWallet.actif); // 1 = id_user
            wService.insertOne(wal1);

            wallet wal2 = new wallet(1, typeWallet.crypto, statutWallet.actif);
            wService.insertOne(wal2);

            // 2️⃣ Afficher tous les wallets existants
            List<wallet> allWallets = wService.SelectAll();
            System.out.println("Liste des wallets disponibles :");
            for (wallet w : allWallets) {
                System.out.println("ID Wallet: " + w.getIdWallet() + ", Type: " + w.getTypeWallet());
            }

            // 3️⃣ Ajouter des currencies si elles n'existent pas encore
//            currency c1 = new currency();
//            c1.setNom("Dollar");
//            currencyService.insertOne(c1);
//
//            currency c2 = new currency();
//            c2.setNom("Euro");
//            currencyService.insertOne(c2);
//
//            currency c3 = new currency();
//            c3.setNom("BTC");
//            currencyService.insertOne(c3);

            // 4️⃣ Afficher toutes les currencies
            List<currency> allCurrencies = currencyService.SelectAll();
            System.out.println("Liste des currencies disponibles :");
            for (currency c : allCurrencies) {
                System.out.println("- " + c.getNom());
            }

            // 5️⃣ Choisir le wallet par ID
            System.out.print("Entrez l'ID du wallet à utiliser : ");
            int choixWalletId = scanner.nextInt();
            scanner.nextLine(); // consommer le \n restant

            // 6️⃣ Choisir la currency
            System.out.print("Entrez le nom de la currency à ajouter dans le wallet : ");
            String choixCurrency = scanner.nextLine();

            wallet_currency wc = new wallet_currency();
            wc.setId_wallet(choixWalletId);
            wc.setNom_currency(choixCurrency);

            // 7️⃣ Ajouter le wallet_currency
            wcService.insertOne(wc);
            System.out.println("WalletCurrency ajouté : " + wc.getNom_currency() +
                    " dans le wallet ID : " + choixWalletId);

            // 8️⃣ Afficher tous les wallet_currency pour vérifier
            List<wallet_currency> allWalletCurrency = wcService.SelectAll();
            System.out.println("Liste des wallet_currency :");
            for (wallet_currency w : allWalletCurrency) {
                System.out.println("Wallet ID: " + w.getId_wallet() +
                        ", Currency: " + w.getNom_currency() +
                        ", Solde: " + w.getSolde());
            }

        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            scanner.close();
        }
    }
}