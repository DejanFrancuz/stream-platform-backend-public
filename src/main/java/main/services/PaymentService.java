package main.services;

import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import main.models.CreatePayment;
import main.models.CreatePaymentItem;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class PaymentService {
    public String createPaymentIntent(CreatePayment payment) throws StripeException {
        PaymentIntentCreateParams params =
                PaymentIntentCreateParams.builder()
                        .setAmount(Long.valueOf(calculateOrderAmount(payment.getItems())))
                        .setCurrency("usd")
                        .setAutomaticPaymentMethods(
                                PaymentIntentCreateParams.AutomaticPaymentMethods
                                        .builder()
                                        .setEnabled(true)
                                        .build()
                        )
                        .build();

        PaymentIntent paymentIntent = PaymentIntent.create(params);
        return paymentIntent.getClientSecret();
    }


    private static int calculateOrderAmount(CreatePaymentItem[] items) {
        // Calculate the order total on the server to prevent
        // people from directly manipulating the amount on the client
        int total = 0;
        for (CreatePaymentItem item : items) {
            total += item.getAmount();
        }
        return total;
    }
}
