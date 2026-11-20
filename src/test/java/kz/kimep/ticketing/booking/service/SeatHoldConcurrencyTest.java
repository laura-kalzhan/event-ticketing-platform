package kz.kimep.ticketing.booking.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import kz.kimep.ticketing.booking.api.CreateHoldRequest;
import kz.kimep.ticketing.booking.repo.SeatHoldRepository;
import kz.kimep.ticketing.booking.repo.TicketRepository;
import kz.kimep.ticketing.catalog.service.EventSeatCatalog;
import kz.kimep.ticketing.identity.service.AuthenticatedUser;
import kz.kimep.ticketing.shared.error.ConflictException;
import kz.kimep.ticketing.testing.InMemorySeatLockStore;
import kz.kimep.ticketing.testing.MutableClock;
import kz.kimep.ticketing.testing.TestData;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SeatHoldConcurrencyTest {

    private static final int RUNS = 300;

    private final ExecutorService pool = Executors.newFixedThreadPool(2);
    private final SeatHoldRepository seatHoldRepository = mock(SeatHoldRepository.class);
    private final MutableClock clock = new MutableClock(TestData.NOW);

    @BeforeEach
    void setUp() {
        when(seatHoldRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @AfterEach
    void tearDown() {
        pool.shutdownNow();
    }

    @Test
    void twoBuyersRacingForOneSeatAlwaysProduceExactlyOneWinner() throws Exception {
        for (int run = 0; run < RUNS; run++) {
            HoldService holdService = newHoldService();
            List<Boolean> results = race(holdService, List.of(7L), List.of(7L));
            assertThat(results).containsExactlyInAnyOrder(true, false);
        }
    }

    @Test
    void overlappingSeatSetsNeverBothSucceed() throws Exception {
        for (int run = 0; run < RUNS; run++) {
            HoldService holdService = newHoldService();
            List<Boolean> results = race(holdService, List.of(1L, 2L), List.of(2L, 3L));
            assertThat(results).containsExactlyInAnyOrder(true, false);
        }
    }

    private HoldService newHoldService() {
        InMemorySeatLockStore store = new InMemorySeatLockStore(clock);
        return new HoldService(seatHoldRepository, mock(TicketRepository.class), new SeatLockService(store),
                mock(EventSeatCatalog.class), TestData.properties(), clock);
    }

    private List<Boolean> race(HoldService holdService, List<Long> seatsA, List<Long> seatsB) throws Exception {
        CountDownLatch start = new CountDownLatch(1);
        Future<Boolean> first = pool.submit(() -> attempt(holdService, TestData.CUSTOMER, seatsA, start));
        Future<Boolean> second = pool.submit(() -> attempt(holdService, TestData.OTHER_CUSTOMER, seatsB, start));
        start.countDown();
        return List.of(first.get(), second.get());
    }

    private boolean attempt(HoldService holdService, AuthenticatedUser user, List<Long> seats, CountDownLatch start)
            throws InterruptedException {
        start.await();
        try {
            holdService.createHold(user, new CreateHoldRequest(4L, seats));
            return true;
        } catch (ConflictException ex) {
            return false;
        }
    }
}
