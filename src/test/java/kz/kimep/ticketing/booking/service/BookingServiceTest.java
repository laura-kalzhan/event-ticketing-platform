package kz.kimep.ticketing.booking.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import kz.kimep.ticketing.booking.api.BookingResponse;
import kz.kimep.ticketing.booking.api.CreateBookingRequest;
import kz.kimep.ticketing.booking.domain.HoldStatus;
import kz.kimep.ticketing.booking.domain.SeatHold;
import kz.kimep.ticketing.booking.repo.BookingRepository;
import kz.kimep.ticketing.booking.repo.SeatHoldRepository;
import kz.kimep.ticketing.catalog.service.EventSeatCatalog;
import kz.kimep.ticketing.shared.audit.AuditService;
import kz.kimep.ticketing.shared.error.GoneException;
import kz.kimep.ticketing.shared.error.NotFoundException;
import kz.kimep.ticketing.shared.tx.AfterCommit;
import kz.kimep.ticketing.testing.InMemorySeatLockStore;
import kz.kimep.ticketing.testing.MutableClock;
import kz.kimep.ticketing.testing.TestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock
    private SeatHoldRepository seatHoldRepository;
    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private EventSeatCatalog eventSeatCatalog;
    @Mock
    private AuditService auditService;

    private final MutableClock clock = new MutableClock(TestData.NOW);
    private final InMemorySeatLockStore store = new InMemorySeatLockStore(clock);
    private BookingService bookingService;

    @BeforeEach
    void setUp() {
        bookingService = new BookingService(seatHoldRepository, bookingRepository, new SeatLockService(store),
                eventSeatCatalog, new BookingPricing(List.of(new ServiceFeeRule())), new AfterCommit(), auditService,
                clock);
    }

    @Test
    void expiredHoldIsGone() {
        SeatHold hold = TestData.hold("H-1", 4L, TestData.NOW.minusSeconds(1), HoldStatus.ACTIVE, 1L);
        when(seatHoldRepository.findForUpdate("H-1")).thenReturn(Optional.of(hold));

        assertThatThrownBy(() -> bookingService.createBooking(TestData.CUSTOMER, new CreateBookingRequest("H-1")))
                .isInstanceOf(GoneException.class);
        verify(bookingRepository, never()).saveAndFlush(any());
    }

    @Test
    void holdOfAnotherCustomerIsNotFound() {
        SeatHold hold = TestData.hold("H-1", 5L, TestData.NOW.plusSeconds(60), HoldStatus.ACTIVE, 1L);
        when(seatHoldRepository.findForUpdate("H-1")).thenReturn(Optional.of(hold));

        assertThatThrownBy(() -> bookingService.createBooking(TestData.CUSTOMER, new CreateBookingRequest("H-1")))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void holdThatWasAlreadyUsedIsGone() {
        SeatHold hold = TestData.hold("H-1", 4L, TestData.NOW.plusSeconds(60), HoldStatus.CONSUMED, 1L);
        when(seatHoldRepository.findForUpdate("H-1")).thenReturn(Optional.of(hold));

        assertThatThrownBy(() -> bookingService.createBooking(TestData.CUSTOMER, new CreateBookingRequest("H-1")))
                .isInstanceOf(GoneException.class);
    }

    @Test
    void holdWhoseRedisLockVanishedIsGone() {
        SeatHold hold = TestData.hold("H-1", 4L, TestData.NOW.plusSeconds(60), HoldStatus.ACTIVE, 1L);
        when(seatHoldRepository.findForUpdate("H-1")).thenReturn(Optional.of(hold));

        assertThatThrownBy(() -> bookingService.createBooking(TestData.CUSTOMER, new CreateBookingRequest("H-1")))
                .isInstanceOf(GoneException.class);
    }

    @Test
    void validHoldBecomesAPendingBookingWithReservedTickets() {
        SeatHold hold = TestData.hold("H-1", 4L, TestData.NOW.plusSeconds(60), HoldStatus.ACTIVE, 1L, 2L);
        store.tryLock(SeatLockService.key(4L, 1L), "H-1", Duration.ofMinutes(1));
        store.tryLock(SeatLockService.key(4L, 2L), "H-1", Duration.ofMinutes(1));
        when(seatHoldRepository.findForUpdate("H-1")).thenReturn(Optional.of(hold));
        when(eventSeatCatalog.priceSeatsForSale(eq(4L), anyCollection()))
                .thenReturn(List.of(TestData.seat(1L, "12000"), TestData.seat(2L, "12000")));

        BookingResponse booking = bookingService.createBooking(TestData.CUSTOMER, new CreateBookingRequest("H-1"));

        assertThat(booking.status()).isEqualTo("PENDING_PAYMENT");
        assertThat(booking.total()).isEqualByComparingTo(new BigDecimal("25200"));
        assertThat(booking.tickets()).hasSize(2).allMatch(ticket -> ticket.status().equals("RESERVED"));
        assertThat(hold.getStatus()).isEqualTo(HoldStatus.CONSUMED);
        assertThat(store.activeCount()).isZero();
    }
}
