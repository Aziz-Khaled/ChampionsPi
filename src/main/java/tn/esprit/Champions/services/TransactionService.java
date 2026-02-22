package tn.esprit.Champions.services;

import tn.esprit.Champions.models.*;
import tn.esprit.Champions.utils.DbConnection;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;

public class TransactionService implements CRUD<transaction> {

    private Connection cnx;


    private wallet_currencyService walletCurrencyService;

    public TransactionService() {
        cnx = DbConnection.getInstance().getCnx();
        walletCurrencyService = new wallet_currencyService();
    }





    @Override
    public void insertOne(transaction t) throws SQLException {
        WalletService walletService = new WalletService();
        wallet_currencyService walletCurrencyService = new wallet_currencyService();

        wallet sourceWallet = walletService.SelectById(t.getIdWalletSource());
        wallet destWallet = walletService.SelectById(t.getIdWalletDestination());

        if (sourceWallet == null) throw new SQLException("Wallet source introuvable !");
        if (destWallet == null) throw new SQLException("Wallet destinataire introuvable !");
        if (sourceWallet.getIdWallet() == destWallet.getIdWallet())
            throw new SQLException("Le wallet source et destination doivent être différents !");
        if (sourceWallet.getStatut() == statutWallet.bloque)
            throw new SQLException("Le wallet source est bloqué !");

        // 🔹 Vérifications types wallet
        if (sourceWallet.getTypeWallet() == typeWallet.fiat && destWallet.getTypeWallet() != typeWallet.fiat)
            throw new SQLException("Un wallet fiat ne peut envoyer qu'à un autre wallet fiat !");
        if ((sourceWallet.getTypeWallet() == typeWallet.crypto || sourceWallet.getTypeWallet() == typeWallet.trading)
                && destWallet.getTypeWallet() == typeWallet.fiat)
            throw new SQLException("Crypto/Trading ne peut pas envoyer vers un wallet fiat !");

        // 🔹 Vérification currency pour wallet trading
        if (sourceWallet.getTypeWallet() == typeWallet.crypto && destWallet.getTypeWallet() == typeWallet.trading) {
            String query = "SELECT is_trading FROM currency WHERE id_currency = ?";
            boolean isTradingCurrency = false;
            try (PreparedStatement stmt = cnx.prepareStatement(query)) {
                stmt.setInt(1, t.getCurrencyId());
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) isTradingCurrency = rs.getBoolean("is_trading");
            }
            if (!isTradingCurrency)
                throw new SQLException("Cette currency n'est pas autorisée pour un wallet TRADING !");
        }

        // 🔹 Vérifie le solde du wallet source
        wallet_currency sourceCurrency = walletCurrencyService
                .getWalletCurrencyByWalletAndId(sourceWallet.getIdWallet(), t.getCurrencyId());
        if (sourceCurrency == null)
            throw new SQLException("La currency n'existe pas dans le wallet source !");

        BigDecimal soldeSource = BigDecimal.valueOf(sourceCurrency.getSolde());
        BigDecimal montant = BigDecimal.valueOf(t.getMontant());
        if (soldeSource.compareTo(montant) < 0)
            throw new SQLException("Solde insuffisant dans le wallet source !");

        boolean previousAutoCommit = cnx.getAutoCommit();
        try {
            cnx.setAutoCommit(false);

            // 🔹 Débite le wallet source
            sourceCurrency.setSolde(soldeSource.subtract(montant).doubleValue());
            walletCurrencyService.updateOne(sourceCurrency);

            // 🔹 Créditer le wallet destinataire
            wallet_currency destCurrency = walletCurrencyService
                    .getWalletCurrencyByWalletAndId(destWallet.getIdWallet(), t.getCurrencyId());

            if (destCurrency == null) {
                // Currency n'existe pas → création avec solde initial = 0
                destCurrency = new wallet_currency();
                destCurrency.setId_wallet(destWallet.getIdWallet());
                destCurrency.setId_currency(t.getCurrencyId());
                destCurrency.setNom_currency(sourceCurrency.getNom_currency());
                destCurrency.setSolde(0);
                walletCurrencyService.insertOne(destCurrency);
            }

            // Ajoute le montant après insertion
            destCurrency.setSolde(destCurrency.getSolde() + t.getMontant());
            walletCurrencyService.updateOne(destCurrency);

            // 🔹 Insère la transaction
            String insertTransactionSql = "INSERT INTO transaction " +
                    "(id_wallet_source, id_wallet_destination, montant, `type`, statut, date_transaction, id_currency) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement pst = cnx.prepareStatement(insertTransactionSql)) {
                pst.setInt(1, sourceWallet.getIdWallet());
                pst.setInt(2, destWallet.getIdWallet());
                pst.setDouble(3, t.getMontant());
                pst.setString(4, t.getType().name());
                pst.setString(5, t.getStatut().name());
                pst.setObject(6, t.getDateTransaction());
                pst.setInt(7, t.getCurrencyId());
                pst.executeUpdate();
            }

            // 🔹 Met à jour la date de modification des wallets
            String updateWalletSql = "UPDATE wallet SET date_derniere_modification = NOW() WHERE id_wallet = ?";
            try (PreparedStatement pst = cnx.prepareStatement(updateWalletSql)) {
                pst.setInt(1, sourceWallet.getIdWallet());
                pst.executeUpdate();
                pst.setInt(1, destWallet.getIdWallet());
                pst.executeUpdate();
            }

            cnx.commit();
            System.out.println("Transaction effectuée avec succès : " + t);

        } catch (SQLException ex) {
            cnx.rollback();
            throw new SQLException("Erreur lors de l'insertion de la transaction : " + ex.getMessage());
        } finally {
            cnx.setAutoCommit(previousAutoCommit);
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
        String updateWalletSql = "UPDATE wallet SET date_derniere_modification = NOW() WHERE id_wallet = ?";
        try (PreparedStatement pst = cnx.prepareStatement(updateWalletSql)) {
            pst.setInt(1, oldTransaction.getIdWalletSource());
            pst.executeUpdate();
            pst.setInt(1, oldTransaction.getIdWalletDestination());
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

        // 🔹 Requête pour récupérer toutes les colonnes nécessaires + id_card
        String query = """
        SELECT t.id_transaction,
               t.id_wallet_source,
               t.id_wallet_destination,
               t.montant,
               t.id_currency,
               t.type,
               t.statut,
               t.date_transaction,
               t.id_card
        FROM transaction t
        WHERE t.id_wallet_source = ? OR t.id_wallet_destination = ?
    """;

        try (PreparedStatement ps = cnx.prepareStatement(query)) {
            ps.setInt(1, walletId);
            ps.setInt(2, walletId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    transaction t = new transaction();

                    // 🔹 Remplissage des champs
                    t.setIdTransaction(rs.getInt("id_transaction"));
                    t.setIdWalletSource(rs.getInt("id_wallet_source"));
                    t.setIdWalletDestination(rs.getInt("id_wallet_destination"));
                    t.setMontant(rs.getDouble("montant"));
                    t.setCurrencyId(rs.getInt("id_currency"));

                    String typeStr = rs.getString("type").trim();
                    t.setType(typeTransaction.valueOf(typeStr));

                    String statutStr = rs.getString("statut").trim();
                    t.setStatut(StatutTransaction.valueOf(statutStr));

                    t.setDateTransaction(rs.getTimestamp("date_transaction").toLocalDateTime());

                    // 🔹 Très important : id_card pour les recharges
                    t.setId_card(rs.getInt("id_card"));

                    transactions.add(t);
                }
            }
        }

        return transactions;
    }


    public PaymentIntent createStripePayment(double
                                                     amount, String currency) throws Exception {

        PaymentIntentCreateParams params =
                PaymentIntentCreateParams.builder()
                        .setAmount((long)(amount * 100)) // Stripe travaille en centimes
                        .setCurrency(currency.toLowerCase())
                        .setAutomaticPaymentMethods(
                                PaymentIntentCreateParams.AutomaticPaymentMethods
                                        .builder()
                                        .setEnabled(true)
                                        .build()
                        )
                        .build();

        return PaymentIntent.create(params);
    }
    public void insertRechargeTransaction(
            int walletDestinationId,
            int currencyId,
            double amount,
            String stripeStatus,
            int creditCardId
    ) throws SQLException {

        StatutTransaction statut;

        // 🔹 Déterminer le statut de la transaction à partir du statut Stripe
        switch (stripeStatus) {
            case "succeeded" -> statut = StatutTransaction.Completed;
            case "processing", "requires_action" -> statut = StatutTransaction.Processing;
            case "canceled" -> statut = StatutTransaction.Cancelled;
            case "requires_payment_method", "requires_confirmation" -> statut = StatutTransaction.Pending;
            default -> statut = StatutTransaction.Failed;
        }

        boolean previousAutoCommit = cnx.getAutoCommit();
        try {
            cnx.setAutoCommit(false);

            // 🔹 Créditer le wallet destination
            wallet_currency destCurrency = walletCurrencyService
                    .getWalletCurrencyByWalletAndId(walletDestinationId, currencyId);

            if (destCurrency == null) {
                destCurrency = new wallet_currency();
                destCurrency.setId_wallet(walletDestinationId);
                destCurrency.setId_currency(currencyId);
                destCurrency.setSolde(amount);
                walletCurrencyService.insertOne(destCurrency);
            } else {
                destCurrency.setSolde(destCurrency.getSolde() + amount);
                walletCurrencyService.updateOne(destCurrency);
            }

            // 🔹 Insérer la transaction RECHARGE (id_wallet_source = NULL, id_card = carte utilisée)
            String sql = """
            INSERT INTO transaction
            (id_wallet_source, id_card, id_wallet_destination, montant, `type`, statut, date_transaction, id_currency)
            VALUES (?, ?, ?, ?, ?, ?, NOW(), ?)
        """;

            try (PreparedStatement pst = cnx.prepareStatement(sql)) {
                pst.setNull(1, Types.INTEGER);       // Wallet source = NULL pour recharge
                pst.setInt(2, creditCardId);         // Carte bancaire utilisée
                pst.setInt(3, walletDestinationId);  // Wallet destination
                pst.setDouble(4, amount);            // Montant
                pst.setString(5, typeTransaction.RECHARGE.name()); // Type
                pst.setString(6, statut.name());     // Statut
                pst.setInt(7, currencyId);           // Devise
                pst.executeUpdate();
            }

            // 🔹 Mettre à jour la date de modification du wallet destination
            String updateWalletSql = "UPDATE wallet SET date_derniere_modification = NOW() WHERE id_wallet = ?";
            try (PreparedStatement pst = cnx.prepareStatement(updateWalletSql)) {
                pst.setInt(1, walletDestinationId);
                pst.executeUpdate();
            }

            cnx.commit();
            System.out.println("Recharge enregistrée avec succès dans transaction.");

        } catch (SQLException e) {
            cnx.rollback();
            throw e;
        } finally {
            cnx.setAutoCommit(previousAutoCommit);
        }
    }
}
