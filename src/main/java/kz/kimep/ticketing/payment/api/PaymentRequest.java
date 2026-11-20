package kz.kimep.ticketing.payment.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import kz.kimep.ticketing.payment.domain.PaymentMethod;

public record PaymentRequest(@NotBlank String bookingReference, @NotNull PaymentMethod method,
                             @NotBlank String cardToken) {

    @Override
    public String toString() {
        return "PaymentRequest[bookingReference=" + bookingReference + ", method=" + method + "]";
    }
}
