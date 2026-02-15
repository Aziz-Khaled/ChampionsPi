package tn.esprit.Champions.services;

import tn.esprit.Champions.models.*;
import tn.esprit.Champions.utils.DbConnection;

import java.sql.*;
import java.util.List;

public class TransactionService implements CRUD<transaction> {

    private Connection cnx;

    // Création d'une instance du service wallet_currency
    private wallet_currencyService walletCurrencyService;

    public TransactionService() {
        cnx = DbConnection.getInstance().getCnx();
        walletCurrencyService = new wallet_currencyService(); // instanciation pour l'appel non statique
    }


    @Override
    public void insertOne(transaction t) throws SQLException {
        WalletService walletService = new WalletService();
        wallet_currencyService walletCurrencyService = new wallet_currencyService();

        wallet sourceWallet = walletService.SelectById(t.getIdWalletSource());
        wallet destWallet = walletService.SelectById(t.getIdWalletDestination());

        if (sourceWallet == null)
            throw new SQLException("Wallet source introuvable !");
        if (destWallet == null)
            throw new SQLException("Wallet destinataire introuvable !");
        if (sourceWallet.getIdWallet() == destWallet.getIdWallet())
            throw new SQLException("Le wallet source et destination doivent être différents !");
        if (sourceWallet.getStatut() == statutWallet.bloque)
            throw new SQLException("Le wallet source est bloqué !");

        if (sourceWallet.getTypeWallet() == typeWallet.fiat && destWallet.getTypeWallet() != typeWallet.fiat)
            throw new SQLException("Un wallet fiat ne peut envoyer qu'à un autre wallet fiat !");
        if ((sourceWallet.getTypeWallet() == typeWallet.crypto || sourceWallet.getTypeWallet() == typeWallet.trading) &&
                destWallet.getTypeWallet() == typeWallet.fiat)
            throw new SQLException("Crypto/Trading ne peut pas envoyer vers un wallet fiat !");

        // Utiliser l'id_currency directement
        wallet_currency sourceCurrency = walletCurrencyService.getWalletCurrencyByWalletAndId(
                sourceWallet.getIdWallet(), t.getCurrencyId()
        );
        if (sourceCurrency == null)
            throw new SQLException("La currency n'existe pas dans le wallet source !");
        if (sourceCurrency.getSolde() < t.getMontant())
            throw new SQLException("Solde insuffisant dans le wallet source !");

        // Débiter le wallet source
        sourceCurrency.setSolde(sourceCurrency.getSolde() - t.getMontant());
        walletCurrencyService.updateOne(sourceCurrency);

        // Crédite le wallet destination
        wallet_currency destCurrency = walletCurrencyService.getWalletCurrencyByWalletAndId(
                destWallet.getIdWallet(), t.getCurrencyId()
        );

        if (destCurrency == null) {
            wallet_currency newCurrency = new wallet_currency();
            newCurrency.setId_wallet(destWallet.getIdWallet());
            newCurrency.setId_currency(t.getCurrencyId());
            newCurrency.setSolde(t.getMontant());
            walletCurrencyService.insertOne(newCurrency);
        } else {
            destCurrency.setSolde(destCurrency.getSolde() + t.getMontant());
            walletCurrencyService.updateOne(destCurrency);
        }

        // Enregistrement de la transaction
        String insertQuery = "INSERT INTO transaction " +
                "(id_wallet_source, id_wallet_destination, montant, `type`, statut, date_transaction, id_currency) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement pst = cnx.prepareStatement(insertQuery)) {
            pst.setInt(1, t.getIdWalletSource());
            pst.setInt(2, t.getIdWalletDestination());
            pst.setDouble(3, t.getMontant());
            pst.setString(4, t.getType().name());
            pst.setString(5, t.getStatut().name());
            pst.setObject(6, t.getDateTransaction());
            pst.setInt(7, t.getCurrencyId());
            pst.executeUpdate();
        }
    }
    @Override
    public void updateOne(transaction transaction) throws SQLException {
        // À implémenter
    }

    @Override
    public void deleteOne(transaction transaction) throws SQLException {
        // À implémenter
    }

    @Override
    public List<transaction> SelectAll() throws SQLException {
        return List.of();
    }
}