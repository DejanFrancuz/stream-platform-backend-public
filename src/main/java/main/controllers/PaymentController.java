package main.controllers;

import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import main.models.CreatePayment;
import main.models.CreatePaymentResponse;
import main.models.PaymentConfirmRequest;
import main.services.PaymentService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {
    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }


    @PostMapping(value = "/create-intent", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CreatePaymentResponse> createPaymentIntent(@RequestBody CreatePayment request) {

        try {
            String secret = paymentService.createPaymentIntent(request);
            return ResponseEntity.ok(new CreatePaymentResponse(secret));
        }catch (StripeException ex){
            return ResponseEntity.badRequest().build();
        }

    }

    @PostMapping("/confirm-payment")
    public ResponseEntity<?> confirmPayment(@RequestBody PaymentConfirmRequest request) throws StripeException {
        System.out.println("Request DTO: " + request);
        System.out.println("PaymentIntentId: " + request.getPaymentIntentId());
        PaymentIntent intent = PaymentIntent.retrieve(request.getPaymentIntentId());

        PaymentIntent confirmed = intent.confirm();
        return ResponseEntity.ok(Map.of(
                "status", confirmed.getStatus(),
                "paymentIntentId", confirmed.getId()
        ));
    }
}
