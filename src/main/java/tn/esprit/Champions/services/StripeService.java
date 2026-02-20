package tn.esprit.Champions.services;

import com.stripe.Stripe;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import tn.esprit.Champions.models.OrderItem;

import java.util.ArrayList;
import java.util.List;

public class StripeService {

        private static final String STRIPE_SECRET_KEY = "sk_test_51QslvaCFuk7NYR1jhBdNcg2vX9jCRZIPayvJEzbafmfBlg7Sx0xtzrYZlvCttZQ9lFJ8O9DwY2mYTdAJ1eSL92ed00JV2J9vvp";

        public StripeService() {
                Stripe.apiKey = STRIPE_SECRET_KEY;
        }

        public String createCheckoutSession(List<OrderItem> items) throws Exception {
                List<SessionCreateParams.LineItem> lineItems = new ArrayList<>();

                for (OrderItem item : items) {
                        lineItems.add(
                                        SessionCreateParams.LineItem.builder()
                                                        .setQuantity((long) item.getQuantity())
                                                        .setPriceData(
                                                                        SessionCreateParams.LineItem.PriceData.builder()
                                                                                        .setCurrency("usd")
                                                                                        .setUnitAmount((long) (item
                                                                                                        .getUnitPrice()
                                                                                                        * 100))
                                                                                        .setProductData(
                                                                                                        SessionCreateParams.LineItem.PriceData.ProductData
                                                                                                                        .builder()
                                                                                                                        .setName(item.getProduct()
                                                                                                                                        .getName())
                                                                                                                        .build())
                                                                                        .build())
                                                        .build());
                }

                SessionCreateParams params = SessionCreateParams.builder()
                                .addPaymentMethodType(SessionCreateParams.PaymentMethodType.CARD)
                                .setMode(SessionCreateParams.Mode.PAYMENT)
                                .setSuccessUrl("https://example.com/success?session_id={CHECKOUT_SESSION_ID}")
                                .setCancelUrl("https://example.com/cancel")
                                .addAllLineItem(lineItems)
                                .build();

                Session session = Session.create(params);
                return session.getUrl();
        }
}
