package main.models;

import lombok.Data;

@Data
public class PaymentConfirmRequest {
    private String paymentIntentId;
}
