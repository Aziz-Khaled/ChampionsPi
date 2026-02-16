package tn.esprit.Champions.services;

import tn.esprit.Champions.models.*;
import tn.esprit.Champions.utils.DbConnection;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TransactionService implements CRUD<transaction> {

    private Connection cnx;


    private wallet_currencyService walletCurrencyService;

    public TransactionService() {
        cnx = DbConnection.getInstance().getCnx();
        walletCurrencyService = new wallet_currencyService(); // instanciation pour l'appel non statique
    }


    @Override
    public void insertOne(transaction t) throws SQLException {
        WalletService walletService = new WalletService();

        wallet sourceWallet = walletService.SelectById(t.getIdWalletSource());
        wallet destWallet = walletService.SelectById(t.getIdWalletDestination());

        if (sourceWallet == null) throw new SQLException("Wallet source introuvable !");
        if (destWallet == null) throw new SQLException("Wallet destinataire introuvable !");
        if (sourceWallet.getIdWallet() == destWallet.getIdWallet())
            throw new SQLException("Le wallet source et destination doivent être différents !");
        if (sourceWallet.getStatut() == statutWallet.bloque)
            throw new SQLException("Le wallet source est bloqué !");


        if (sourceWallet.getTypeWallet() == typeWallet.fiat && destWallet.getTypeWallet() != typeWallet.fiat)
            throw new SQLException("Un wallet fiat ne peut envoyer qu'à un autre wallet fiat !");
        if ((sourceWallet.getTypeWallet() == typeWallet.crypto || sourceWallet.getTypeWallet() == typeWallet.trading)
                && destWallet.getTypeWallet() == typeWallet.fiat)
            throw new SQLException("Crypto/Trading ne peut pas envoyer vers un wallet fiat !");


        if (sourceWallet.getTypeWallet() == typeWallet.crypto && destWallet.getTypeWallet() == typeWallet.trading) {
            String query = "SELECT is_trading FROM currency WHERE id_currency = ?";
            boolean isTradingCurrency = false;

            try (PreparedStatement stmt = cnx.prepareStatement(query)) {
                stmt.setInt(1, t.getCurrencyId());
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) isTradingCurrency = rs.getBoolean("is_trading");
            }

            if (!isTradingCurrency) {
                throw new SQLException("Cette currency n'est pas autorisée pour un wallet TRADING !");
            }
        }


        wallet_currency sourceCurrency = walletCurrencyService.getWalletCurrencyByWalletAndId(
                sourceWallet.getIdWallet(), t.getCurrencyId()
        );
        if (sourceCurrency == null)
            throw new SQLException("La currency n'existe pas dans le wallet source !");

        BigDecimal soldeSource = BigDecimal.valueOf(sourceCurrency.getSolde());
        BigDecimal montant = BigDecimal.valueOf(t.getMontant());

        if (soldeSource.compareTo(montant) < 0)
            throw new SQLException("Solde insuffisant dans le wallet source !");


        String updateSourceSql = "UPDATE wallet_currency SET solde = solde - ? WHERE id_wallet = ? AND id_currency = ?";
        try (PreparedStatement pst = cnx.prepareStatement(updateSourceSql)) {
            pst.setDouble(1, t.getMontant());
            pst.setInt(2, sourceWallet.getIdWallet());
            pst.setInt(3, t.getCurrencyId());
            pst.executeUpdate();
        }


        wallet_currency destCurrency = walletCurrencyService.getWalletCurrencyByWalletAndId(
                destWallet.getIdWallet(), t.getCurrencyId()
        );

        if (destCurrency == null) {

            String insertDestSql = "INSERT INTO wallet_currency (id_wallet, id_currency, solde, nom_currency) VALUES (?, ?, ?, ?)";
            try (PreparedStatement pst = cnx.prepareStatement(insertDestSql)) {
                pst.setInt(1, destWallet.getIdWallet());
                pst.setInt(2, t.getCurrencyId());
                pst.setDouble(3, t.getMontant());
                pst.setString(4, sourceCurrency.getNom_currency()); // nom currency depuis source
                pst.executeUpdate();
            }
        } else {

            String updateDestSql = "UPDATE wallet_currency SET solde = solde + ? WHERE id_wallet = ? AND id_currency = ?";
            try (PreparedStatement pst = cnx.prepareStatement(updateDestSql)) {
                pst.setDouble(1, t.getMontant());
                pst.setInt(2, destWallet.getIdWallet());
                pst.setInt(3, t.getCurrencyId());
                pst.executeUpdate();
            }
        }


        String insertTransactionSql = "INSERT INTO transaction " +
                "(id_wallet_source, id_wallet_destination, montant, `type`, statut, date_transaction, id_currency) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pst = cnx.prepareStatement(insertTransactionSql)) {
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
    public void updateOne(transaction t) throws SQLException {


        String selectSql = "SELECT * FROM transaction WHERE id_transaction = ?";
        transaction oldTransaction = null;

        try (PreparedStatement ps = cnx.prepareStatement(selectSql)) {
            ps.setInt(1, t.getIdTransaction());
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                oldTransaction = new transaction();
                oldTransaction.setIdTransaction(rs.getInt("id_transaction"));
                oldTransaction.setIdWalletSource(rs.getInt("id_wallet_source"));
                oldTransaction.setIdWalletDestination(rs.getInt("id_wallet_destination"));
                oldTransaction.setMontant(rs.getDouble("montant"));
                oldTransaction.setCurrencyId(rs.getInt("id_currency"));
            } else {
                throw new SQLException("Transaction introuvable !");
            }
        }


        double oldAmount = oldTransaction.getMontant();
        double newAmount = t.getMontant();
        double diff = newAmount - oldAmount;

        int walletSourceId = oldTransaction.getIdWalletSource();
        int walletDestId = oldTransaction.getIdWalletDestination();
        int currencyId = oldTransaction.getCurrencyId();


        wallet_currency sourceCurrency = walletCurrencyService
                .getWalletCurrencyByWalletAndId(walletSourceId, currencyId);

        if (sourceCurrency == null) {
            throw new SQLException("Currency introuvable dans le wallet source !");
        }

        double soldeSource = sourceCurrency.getSolde();


        if (diff > 0 && soldeSource < diff) {
            throw new SQLException("Solde insuffisant pour augmenter le montant !");
        }


        String updateSourceSql = "UPDATE wallet_currency SET solde = solde - ? WHERE id_wallet = ? AND id_currency = ?";
        try (PreparedStatement pst = cnx.prepareStatement(updateSourceSql)) {
            pst.setDouble(1, diff);
            pst.setInt(2, walletSourceId);
            pst.setInt(3, currencyId);
            pst.executeUpdate();
        }


        wallet_currency destCurrency = walletCurrencyService
                .getWalletCurrencyByWalletAndId(walletDestId, currencyId);

        if (destCurrency == null) {

            String insertDestSql = "INSERT INTO wallet_currency (id_wallet, id_currency, solde, nom_currency) VALUES (?, ?, ?, ?)";
            try (PreparedStatement pst = cnx.prepareStatement(insertDestSql)) {
                pst.setInt(1, walletDestId);
                pst.setInt(2, currencyId);
                pst.setDouble(3, diff);
                pst.setString(4, sourceCurrency.getNom_currency());
                pst.executeUpdate();
            }
        } else {
            String updateDestSql = "UPDATE wallet_currency SET solde = solde + ? WHERE id_wallet = ? AND id_currency = ?";
            try (PreparedStatement pst = cnx.prepareStatement(updateDestSql)) {
                pst.setDouble(1, diff);
                pst.setInt(2, walletDestId);
                pst.setInt(3, currencyId);
                pst.executeUpdate();
            }
        }


        String updateTransactionSql = "UPDATE transaction SET montant = ?, date_transaction = NOW() WHERE id_transaction = ?";
        try (PreparedStatement pst = cnx.prepareStatement(updateTransactionSql)) {
            pst.setDouble(1, newAmount);
            pst.setInt(2, t.getIdTransaction());
            pst.executeUpdate();
        }
    }

    @Override
    public void deleteOne(transaction t) throws SQLException {
//        String sql = "DELETE FROM transaction WHERE id_transaction = ?";
//
//        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
//            ps.setInt(1, t.getIdTransaction());
//            ps.executeUpdate();
//        }
    }

    @Override
    public List<transaction> SelectAll() throws SQLException {
        return List.of();
    }
    public List<transaction> getTransactionsByWallet(int walletId) throws SQLException {
        List<transaction> transactions = new ArrayList<>();

        String query = "SELECT * FROM transaction WHERE id_wallet_source = ? OR id_wallet_destination = ?";
        PreparedStatement ps = cnx.prepareStatement(query);
        ps.setInt(1, walletId);
        ps.setInt(2, walletId);

        ResultSet rs = ps.executeQuery();

        while (rs.next()) {
            transaction t = new transaction();
            t.setIdTransaction(rs.getInt("id_transaction"));
            t.setIdWalletSource(rs.getInt("id_wallet_source"));
            t.setIdWalletDestination(rs.getInt("id_wallet_destination"));
            t.setMontant(rs.getDouble("montant"));


            String typeStr = rs.getString("type").trim(); // supprimer espaces éventuels
            t.setType(typeTransaction.valueOf(typeStr));

            String statutStr = rs.getString("statut").trim();
            t.setStatut(StatutTransaction.valueOf(statutStr));

            t.setDateTransaction(rs.getTimestamp("date_transaction").toLocalDateTime());
            t.setCurrencyId(rs.getInt("id_currency"));
            transactions.add(t);
        }
        return transactions;
    }
}
