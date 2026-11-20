package kz.kimep.ticketing.booking.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import kz.kimep.ticketing.booking.domain.Booking;
import kz.kimep.ticketing.booking.domain.BookingStatus;
import kz.kimep.ticketing.booking.repo.BookingRepository;
import kz.kimep.ticketing.catalog.service.EventSeatCatalog;
import kz.kimep.ticketing.messaging.service.OutboxWriter;
import kz.kimep.ticketing.shared.audit.AuditService;
import kz.kimep.ticketing.shared.error.ConflictException;
import kz.kimep.ticketing.shared.error.NotFoundException;
import kz.kimep.ticketing.testing.MutableClock;
import kz.kimep.ticketing.testing.TestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

@ExtendWith(MockitoExtension.class)
class BookingLifecycleServiceTest {

    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private EventSeatCatalog eventSeatCatalog;
    @Mock
    private OutboxWriter outboxWriter;
    @Mock
    private AuditService auditService;

    private final MutableClock clock = new MutableClock(TestData.NOW);
    private BookingLifecycleService lifecycle;

    @BeforeEach
    void setUp() {
        lifecycle = new BookingLifecycleService(bookingRepository, eventSeatCatalog,
                new CancellationPolicy(TestData.properties()), outboxWriter, auditService, TestData.properties(), clock);
    }

    @Test
    void confirmingWithAStaleVersionIsRejected() {
        Booking booking = TestData.booking("B-1", 4L, BookingStatus.PENDING_PAYMENT, 3);
        when(bookingRepository.findOneByBookingReference("B-1")).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> lifecycle.confirmPaid("B-1", 2))
                .isInstanceOf(ObjectOptimisticLockingFailureException.class);
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.PENDING_PAYMENT);
        verifyNoInteractions(outboxWriter);
    }

    @Test
    void confirmingWritesBookingConfirmedToTheOutbox() {
        Booking booking = TestData.booking("B-1", 4L, BookingStatus.PENDING_PAYMENT, 2);
        when(bookingRepository.findOneByBookingReference("B-1")).thenReturn(Optional.of(booking));

        lifecycle.confirmPaid("B-1", 2);

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(booking.getConfirmedAt()).isEqualTo(TestData.NOW);
        verify(outboxWriter).write(eq("Booking"), eq("B-1"), eq("booking.confirmed"), anyMap());
    }

    @Test
    void confirmingAnAlreadyConfirmedBookingIsAConflict() {
        Booking booking = TestData.booking("B-1", 4L, BookingStatus.CONFIRMED, 2);
        when(bookingRepository.findOneByBookingReference("B-1")).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> lifecycle.confirmPaid("B-1", 2)).isInstanceOf(ConflictException.class);
    }

    @Test
    void cancellingCloseToTheEventIsRejected() {
        Booking booking = TestData.booking("B-1", 4L, BookingStatus.CONFIRMED, 0);
        when(bookingRepository.findOneByBookingReference("B-1")).thenReturn(Optional.of(booking));
        when(eventSeatCatalog.startTimeOf(4L)).thenReturn(TestData.NOW.plus(Duration.ofHours(2)));

        assertThatThrownBy(() -> lifecycle.cancel("B-1", TestData.CUSTOMER)).isInstanceOf(ConflictException.class);
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
    }

    @Test
    void cancellingEarlyEnoughWritesBookingCancelled() {
        Booking booking = TestData.booking("B-1", 4L, BookingStatus.CONFIRMED, 0);
        when(bookingRepository.findOneByBookingReference("B-1")).thenReturn(Optional.of(booking));
        when(eventSeatCatalog.startTimeOf(4L)).thenReturn(TestData.NOW.plus(Duration.ofDays(10)));

        lifecycle.cancel("B-1", TestData.CUSTOMER);

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CANCELLED);
        verify(outboxWriter).write(eq("Booking"), eq("B-1"), eq("booking.cancelled"), anyMap());
    }

    @Test
    void payingForAConfirmedBookingIsAConflict() {
        Booking booking = TestData.booking("B-1", 4L, BookingStatus.CONFIRMED, 0);
        when(bookingRepository.findOneByBookingReference("B-1")).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> lifecycle.requirePayable("B-1", 4L)).isInstanceOf(ConflictException.class);
    }

    @Test
    void anotherCustomersBookingIsNotFound() {
        Booking booking = TestData.booking("B-1", 4L, BookingStatus.PENDING_PAYMENT, 0);
        when(bookingRepository.findOneByBookingReference("B-1")).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> lifecycle.requirePayable("B-1", 5L)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void unpaidBookingsPastThePaymentWindowExpire() {
        Booking first = TestData.booking("B-1", 4L, BookingStatus.PENDING_PAYMENT, 0);
        Booking second = TestData.booking("B-2", 5L, BookingStatus.PENDING_PAYMENT, 0);
        when(bookingRepository.findByStatusAndCreatedAtBefore(BookingStatus.PENDING_PAYMENT,
                TestData.NOW.minus(Duration.ofMinutes(15)))).thenReturn(List.of(first, second));

        int expired = lifecycle.expireUnpaid();

        assertThat(expired).isEqualTo(2);
        assertThat(first.getStatus()).isEqualTo(BookingStatus.EXPIRED);
        verify(outboxWriter, times(2)).write(eq("Booking"), org.mockito.ArgumentMatchers.anyString(),
                eq("booking.cancelled"), anyMap());
    }
}
