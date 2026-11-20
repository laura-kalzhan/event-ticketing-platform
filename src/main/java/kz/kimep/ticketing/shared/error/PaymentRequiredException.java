package kz.kimep.ticketing.shared.error;

import org.springframework.http.HttpStatus;

public class PaymentRequiredException extends ApiException {

    public PaymentRequiredException(String message) {
        super(HttpStatus.PAYMENT_REQUIRED, message);
    }
}
