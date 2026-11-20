package kz.kimep.ticketing.payment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import kz.kimep.ticketing.booking.service.BookingLifecycleService;
import kz.kimep.ticketing.booking.service.PayableBooking;
import kz.kimep.ticketing.payment.api.PaymentRequest;
import kz.kimep.ticketing.payment.api.PaymentResponse;
import kz.kimep.ticketing.payment.domain.PaymentMethod;
import kz.kimep.ticketing.shared.config.TicketingProperties;
import kz.kimep.ticketing.shared.error.PaymentRequiredException;
import kz.kimep.ticketing.testing.TestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class PaymentServiceTest {

    private static final PayableBooking BOOKING = new PayableBooking("B-1", 100L, new BigDecimal("25200"), "KZT", 0);

    private final BookingLifecycleService lifecycle = mock(BookingLifecycleService.class);
    private final PaymentRecorder recorder = mock(PaymentRecorder.class);

    @BeforeEach
    void setUp() {
        when(lifecycle.requirePayable("B-1", 4L)).thenReturn(BOOKING);
    }

    @Test
    void declinedCardRecordsTheFailureAndAnswersPaymentRequired() {
        PaymentService service = service(request -> GatewayResult.declined("CARD_DECLINED"), TestData.properties());

        assertThatThrownBy(() -> service.pay(TestData.CUSTOMER, request("decline_card")))
                .isInstanceOf(PaymentRequiredException.class);
        verify(recorder).recordDecline(eq(BOOKING), eq(PaymentMethod.CARD), any());
        verify(recorder, never()).recordSuccess(any(), any(), any());
    }

    @Test
    void approvedCardConfirmsTheBooking() {
        PaymentResponse expected = new PaymentResponse("B-1", "SUCCEEDED", "CARD", BOOKING.total(), "KZT", "tx", null);
        when(recorder.recordSuccess(eq(BOOKING), eq(PaymentMethod.CARD), any())).thenReturn(expected);
        PaymentService service = service(request -> GatewayResult.approved("tx"), TestData.properties());

        assertThat(service.pay(TestData.CUSTOMER, request("tok_visa"))).isEqualTo(expected);
    }

    @Test
    void gatewayThatNeverAnswersTimesOutAsADecline() {
        CountDownLatch never = new CountDownLatch(1);
        PaymentGateway hanging = request -> {
            try {
                never.await();
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
            return GatewayResult.approved("too-late");
        };
        TicketingProperties fastTimeout = new TicketingProperties(Duration.ofMinutes(10), 8, Duration.ofMinutes(15),
                Duration.ofMillis(100), Duration.ofHours(24));
        try {
            assertThatThrownBy(() -> service(hanging, fastTimeout).pay(TestData.CUSTOMER, request("tok_visa")))
                    .isInstanceOf(PaymentRequiredException.class);
            ArgumentCaptor<GatewayResult> result = ArgumentCaptor.forClass(GatewayResult.class);
            verify(recorder).recordDecline(eq(BOOKING), eq(PaymentMethod.CARD), result.capture());
            assertThat(result.getValue().failureCode()).isEqualTo("GATEWAY_TIMEOUT");
        } finally {
            never.countDown();
        }
    }

    private PaymentService service(PaymentGateway gateway, TicketingProperties properties) {
        return new PaymentService(lifecycle, gateway, recorder, properties);
    }

    private PaymentRequest request(String token) {
        return new PaymentRequest("B-1", PaymentMethod.CARD, token);
    }
}
