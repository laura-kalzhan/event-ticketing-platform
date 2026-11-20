package kz.kimep.ticketing.booking.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import kz.kimep.ticketing.booking.api.CreateHoldRequest;
import kz.kimep.ticketing.booking.api.HoldResponse;
import kz.kimep.ticketing.booking.domain.HoldStatus;
import kz.kimep.ticketing.booking.domain.SeatHold;
import kz.kimep.ticketing.booking.repo.SeatHoldRepository;
import kz.kimep.ticketing.booking.repo.TicketRepository;
import kz.kimep.ticketing.catalog.service.EventSeatCatalog;
import kz.kimep.ticketing.shared.error.BadRequestException;
import kz.kimep.ticketing.shared.error.ConflictException;
import kz.kimep.ticketing.shared.error.NotFoundException;
import kz.kimep.ticketing.testing.InMemorySeatLockStore;
import kz.kimep.ticketing.testing.MutableClock;
import kz.kimep.ticketing.testing.TestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;

@ExtendWith(MockitoExtension.class)
class HoldServiceTest {

    @Mock
    private SeatHoldRepository seatHoldRepository;
    @Mock
    private TicketRepository ticketRepository;
    @Mock
    private EventSeatCatalog eventSeatCatalog;

    private final MutableClock clock = new MutableClock(TestData.NOW);
    private final InMemorySeatLockStore store = new InMemorySeatLockStore(clock);
    private HoldService holdService;

    @BeforeEach
    void setUp() {
        holdService = new HoldService(seatHoldRepository, ticketRepository, new SeatLockService(store),
                eventSeatCatalog, TestData.properties(), clock);
    }

    @Test
    void moreSeatsThanTheLimitIsABadRequest() {
        CreateHoldRequest request = new CreateHoldRequest(4L, List.of(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L));

        assertThatThrownBy(() -> holdService.createHold(TestData.CUSTOMER, request))
                .isInstanceOf(BadRequestException.class);
        verifyNoInteractions(eventSeatCatalog);
        assertThat(store.activeCount()).isZero();
    }

    @Test
    void seatAlreadyHeldGivesConflictAndLeavesNoPartialHold() {
        store.tryLock(SeatLockService.key(4L, 2L), "other-hold", Duration.ofMinutes(10));

        assertThatThrownBy(() -> holdService.createHold(TestData.CUSTOMER, new CreateHoldRequest(4L, List.of(1L, 2L, 3L))))
                .isInstanceOf(ConflictException.class);
        assertThat(store.activeCount()).isEqualTo(1);
    }

    @Test
    void seatAlreadySoldGivesConflictAndReleasesTheNewLocks() {
        when(ticketRepository.existsByEventIdAndSeatIdIn(eq(4L), anyCollection())).thenReturn(true);

        assertThatThrownBy(() -> holdService.createHold(TestData.CUSTOMER, new CreateHoldRequest(4L, List.of(1L, 2L))))
                .isInstanceOf(ConflictException.class);
        assertThat(store.activeCount()).isZero();
    }

    @Test
    void databaseFailureReleasesTheRedisLocks() {
        when(seatHoldRepository.save(any())).thenThrow(new DataAccessResourceFailureException("db down"));

        assertThatThrownBy(() -> holdService.createHold(TestData.CUSTOMER, new CreateHoldRequest(4L, List.of(1L, 2L))))
                .isInstanceOf(DataAccessResourceFailureException.class);
        assertThat(store.activeCount()).isZero();
    }

    @Test
    void successfulHoldLocksEverySeatForTheConfiguredTtl() {
        when(seatHoldRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        HoldResponse hold = holdService.createHold(TestData.CUSTOMER, new CreateHoldRequest(4L, List.of(1L, 2L)));

        assertThat(hold.status()).isEqualTo("ACTIVE");
        assertThat(hold.secondsRemaining()).isEqualTo(600);
        assertThat(store.ownerOf(SeatLockService.key(4L, 1L))).isEqualTo(hold.holdReference());
        assertThat(store.ownerOf(SeatLockService.key(4L, 2L))).isEqualTo(hold.holdReference());
    }

    @Test
    void holdOfAnotherCustomerIsNotFound() {
        SeatHold hold = TestData.hold("H-1", 5L, TestData.NOW.plusSeconds(300), HoldStatus.ACTIVE, 1L);
        when(seatHoldRepository.findByHoldReference("H-1")).thenReturn(Optional.of(hold));

        assertThatThrownBy(() -> holdService.getHold("H-1", TestData.CUSTOMER))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void releasingAHoldFreesItsSeats() {
        SeatHold hold = TestData.hold("H-2", 4L, TestData.NOW.plusSeconds(300), HoldStatus.ACTIVE, 1L);
        store.tryLock(SeatLockService.key(4L, 1L), "H-2", Duration.ofMinutes(5));
        when(seatHoldRepository.findByHoldReference("H-2")).thenReturn(Optional.of(hold));

        holdService.releaseHold("H-2", TestData.CUSTOMER);

        assertThat(hold.getStatus()).isEqualTo(HoldStatus.RELEASED);
        assertThat(store.activeCount()).isZero();
    }
}
