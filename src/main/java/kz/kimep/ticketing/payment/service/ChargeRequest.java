package kz.kimep.ticketing.payment.service;

import java.math.BigDecimal;
import kz.kimep.ticketing.payment.domain.PaymentMethod;

public record ChargeRequest(String reference, BigDecimal amount, String currency, PaymentMethod method,
                            String cardToken) {

    @Override
    public String toString() {
        return "ChargeRequest[reference=" + reference + ", amount=" + amount + " " + currency + "]";
    }
}
