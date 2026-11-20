package kz.kimep.ticketing.payment.service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import kz.kimep.ticketing.booking.service.BookingLifecycleService;
import kz.kimep.ticketing.booking.service.PayableBooking;
import kz.kimep.ticketing.identity.service.AuthenticatedUser;
import kz.kimep.ticketing.payment.api.PaymentRequest;
import kz.kimep.ticketing.payment.api.PaymentResponse;
import kz.kimep.ticketing.shared.config.TicketingProperties;
import kz.kimep.ticketing.shared.error.PaymentRequiredException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final BookingLifecycleService bookingLifecycleService;
    private final PaymentGateway paymentGateway;
    private final PaymentRecorder paymentRecorder;
    private final TicketingProperties properties;

    public PaymentResponse pay(AuthenticatedUser user, PaymentRequest request) {
        PayableBooking booking = bookingLifecycleService.requirePayable(request.bookingReference(), user.id());
        ChargeRequest charge = new ChargeRequest(booking.reference(), booking.total(), booking.currency(),
                request.method(), request.cardToken());
        GatewayResult result = chargeWithTimeout(charge);
        if (!result.approved()) {
            paymentRecorder.recordDecline(booking, request.method(), result);
            throw new PaymentRequiredException("Payment for booking " + booking.reference() + " was declined ("
                    + result.failureCode() + "). Try another card or payment method.");
        }
        return paymentRecorder.recordSuccess(booking, request.method(), result);
    }

    private GatewayResult chargeWithTimeout(ChargeRequest charge) {
        try {
            return CompletableFuture.supplyAsync(() -> paymentGateway.charge(charge))
                    .get(properties.paymentTimeout().toMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException ex) {
            log.warn("Payment gateway timed out for {}", charge.reference());
            return GatewayResult.declined("GATEWAY_TIMEOUT");
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return GatewayResult.declined("INTERRUPTED");
        } catch (ExecutionException ex) {
            log.warn("Payment gateway failed for {}", charge.reference(), ex.getCause());
            return GatewayResult.declined("GATEWAY_ERROR");
        }
    }
}
