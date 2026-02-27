package tn.esprit.Champions.services;

import tn.esprit.Champions.models.CreditCard;
import tn.esprit.Champions.models.StatutCreditCard;
import tn.esprit.Champions.utils.DbConnection;

import java.sql.*;

public class CreditCardService {
    private Connection cnx;

    public CreditCardService() {
        cnx = DbConnection.getInstance().getCnx();
    }


    public CreditCard getActiveCardByUserId(int userId) {
        String sql = "SELECT * FROM credit_card WHERE id_user = ? AND statut = 'ACTIVE'";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                CreditCard card = new CreditCard();
                card.setIdCard(rs.getInt("id_card"));
                card.setIdUser(rs.getInt("id_user"));
                card.setCardHolderName(rs.getString("card_holder_name"));
                card.setLast4Digits(rs.getString("last_4_digits"));
                card.setExpiryMonth(rs.getInt("expiry_month"));
                card.setExpiryYear(rs.getInt("expiry_year"));
                card.setStripeCustomerId(rs.getString("stripe_customer_id"));
                card.setStripePaymentMethodId(rs.getString("stripe_payment_method_id"));
                card.setStatut(StatutCreditCard.valueOf(rs.getString("statut")));
                return card;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public CreditCard getCardByUserId(int userId) {
        String sql = "SELECT * FROM credit_card WHERE id_user = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                CreditCard card = new CreditCard();
                card.setIdCard(rs.getInt("id_card"));
                card.setIdUser(rs.getInt("id_user"));
                card.setCardHolderName(rs.getString("card_holder_name"));
                card.setLast4Digits(rs.getString("last_4_digits"));
                card.setExpiryMonth(rs.getInt("expiry_month"));
                card.setExpiryYear(rs.getInt("expiry_year"));
                card.setStripeCustomerId(rs.getString("stripe_customer_id"));
                card.setStripePaymentMethodId(rs.getString("stripe_payment_method_id"));
                card.setDateAjout(String.valueOf(rs.getTimestamp("date_ajout").toLocalDateTime()));
                return card;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
    public CreditCard getCardById(int idCard) {
        String sql = "SELECT * FROM credit_card WHERE id_card = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, idCard);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                CreditCard card = new CreditCard();
                card.setIdCard(rs.getInt("id_card"));
                card.setIdUser(rs.getInt("id_user"));
                card.setCardHolderName(rs.getString("card_holder_name"));
                card.setLast4Digits(rs.getString("last_4_digits"));
                card.setExpiryMonth(rs.getInt("expiry_month"));
                card.setExpiryYear(rs.getInt("expiry_year"));
                card.setStripeCustomerId(rs.getString("stripe_customer_id"));
                card.setStripePaymentMethodId(rs.getString("stripe_payment_method_id"));
                card.setDateAjout(String.valueOf(rs.getTimestamp("date_ajout").toLocalDateTime()));
                return card;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }




    public CreditCard insertCard(CreditCard card) {
        try {
            String insert = "INSERT INTO credit_card " +
                    "(id_user, card_holder_name, last_4_digits, expiry_month, expiry_year, stripe_customer_id, stripe_payment_method_id, statut) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, 'ACTIVE')";
            try (PreparedStatement pst = cnx.prepareStatement(insert, Statement.RETURN_GENERATED_KEYS)) {
                pst.setInt(1, card.getIdUser());
                pst.setString(2, card.getCardHolderName());
                pst.setString(3, card.getLast4Digits());
                pst.setInt(4, card.getExpiryMonth());
                pst.setInt(5, card.getExpiryYear());
                pst.setString(6, card.getStripeCustomerId());
                pst.setString(7, card.getStripePaymentMethodId());
                pst.executeUpdate();

                try (ResultSet rs = pst.getGeneratedKeys()) {
                    if (rs.next()) card.setIdCard(rs.getInt(1));
                }
            }
            return card;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void updateCard(CreditCard card) {
        String sql = "UPDATE credit_card SET expiry_month = ?, expiry_year = ?, stripe_customer_id = ?, stripe_payment_method_id = ? WHERE id_card = ?";
        try (PreparedStatement pst = cnx.prepareStatement(sql)) {
            pst.setInt(1, card.getExpiryMonth());
            pst.setInt(2, card.getExpiryYear());
            pst.setString(3, card.getStripeCustomerId());
            pst.setString(4, card.getStripePaymentMethodId());
            pst.setInt(5, card.getIdCard());
            pst.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    public void deleteCard(int idCard) {
        try {
            String update = "UPDATE credit_card SET statut = 'DELETED' WHERE id_card = ?";
            try (PreparedStatement pst = cnx.prepareStatement(update)) {
                pst.setInt(1, idCard);
                pst.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}