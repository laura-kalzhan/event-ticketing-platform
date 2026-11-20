package kz.kimep.ticketing.payment.service;

import java.time.Clock;
import java.util.Map;
import kz.kimep.ticketing.booking.service.BookingLifecycleService;
import kz.kimep.ticketing.booking.service.PayableBooking;
import kz.kimep.ticketing.messaging.config.RabbitTopology;
import kz.kimep.ticketing.messaging.service.OutboxWriter;
import kz.kimep.ticketing.payment.api.PaymentResponse;
import kz.kimep.ticketing.payment.domain.Payment;
import kz.kimep.ticketing.payment.domain.PaymentMethod;
import kz.kimep.ticketing.payment.domain.PaymentStatus;
import kz.kimep.ticketing.payment.repo.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PaymentRecorder {

    private final PaymentRepository paymentRepository;
    private final BookingLifecycleService bookingLifecycleService;
    private final OutboxWriter outboxWriter;
    private final Clock clock;

    @Transactional
    public PaymentResponse recordSuccess(PayableBooking booking, PaymentMethod method, GatewayResult result) {
        Payment payment = paymentFor(booking, method);
        payment.setStatus(PaymentStatus.SUCCEEDED);
        payment.setTransactionId(result.transactionId());
        payment.setFailureCode(null);
        payment.setPaidAt(clock.instant());
        paymentRepository.save(payment);
        bookingLifecycleService.confirmPaid(booking.reference(), booking.version());
        return PaymentResponse.from(payment, booking.reference());
    }

    @Transactional
    public void recordDecline(PayableBooking booking, PaymentMethod method, GatewayResult result) {
        Payment payment = paymentFor(booking, method);
        payment.setStatus(PaymentStatus.DECLINED);
        payment.setFailureCode(result.failureCode());
        paymentRepository.save(payment);
        outboxWriter.write("Booking", booking.reference(), RabbitTopology.PAYMENT_FAILED,
                Map.of("bookingReference", booking.reference(), "failureCode", result.failureCode()));
    }

    private Payment paymentFor(PayableBooking booking, PaymentMethod method) {
        Payment payment = paymentRepository.findByBookingId(booking.bookingId()).orElseGet(Payment::new);
        payment.setBookingId(booking.bookingId());
        payment.setMethod(method);
        payment.setAmount(booking.total());
        payment.setCurrency(booking.currency());
        return payment;
    }
}
