package kz.kimep.ticketing.payment.api;

import java.math.BigDecimal;
import java.time.Instant;
import kz.kimep.ticketing.payment.domain.Payment;

public record PaymentResponse(
        String bookingReference,
        String status,
        String method,
        BigDecimal amount,
        String currency,
        String transactionId,
        Instant paidAt) {

    public static PaymentResponse from(Payment payment, String bookingReference) {
        return new PaymentResponse(bookingReference, payment.getStatus().name(), payment.getMethod().name(),
                payment.getAmount(), payment.getCurrency(), payment.getTransactionId(), payment.getPaidAt());
    }
}
