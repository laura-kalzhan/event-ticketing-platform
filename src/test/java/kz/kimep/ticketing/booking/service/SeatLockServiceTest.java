package kz.kimep.ticketing.booking.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.List;
import kz.kimep.ticketing.testing.InMemorySeatLockStore;
import kz.kimep.ticketing.testing.MutableClock;
import kz.kimep.ticketing.testing.TestData;
import org.junit.jupiter.api.Test;

class SeatLockServiceTest {

    private static final Duration TTL = Duration.ofMinutes(10);

    private final MutableClock clock = new MutableClock(TestData.NOW);
    private final InMemorySeatLockStore store = new InMemorySeatLockStore(clock);
    private final SeatLockService seatLockService = new SeatLockService(store);

    @Test
    void failedSeatInTheMiddleReleasesTheSeatsAlreadyTaken() {
        store.tryLock(SeatLockService.key(4L, 3L), "other-hold", TTL);

        boolean locked = seatLockService.lockAll(4L, List.of(1L, 2L, 3L, 4L), "my-hold", TTL);

        assertThat(locked).isFalse();
        assertThat(store.ownerOf(SeatLockService.key(4L, 1L))).isNull();
        assertThat(store.ownerOf(SeatLockService.key(4L, 2L))).isNull();
        assertThat(store.ownerOf(SeatLockService.key(4L, 3L))).isEqualTo("other-hold");
        assertThat(store.activeCount()).isEqualTo(1);
    }

    @Test
    void lockDisappearsExactlyWhenTheTtlRunsOut() {
        seatLockService.lockAll(4L, List.of(1L), "my-hold", TTL);

        clock.advance(TTL.minusSeconds(1));
        assertThat(seatLockService.ownsAll(4L, List.of(1L), "my-hold")).isTrue();

        clock.advance(Duration.ofSeconds(1));
        assertThat(seatLockService.ownsAll(4L, List.of(1L), "my-hold")).isFalse();
        assertThat(seatLockService.lockAll(4L, List.of(1L), "next-hold", TTL)).isTrue();
    }

    @Test
    void releaseDoesNotRemoveALockOwnedBySomeoneElse() {
        seatLockService.lockAll(4L, List.of(1L), "owner-hold", TTL);

        seatLockService.releaseAll(4L, List.of(1L), "intruder-hold");

        assertThat(store.ownerOf(SeatLockService.key(4L, 1L))).isEqualTo("owner-hold");
    }

    @Test
    void restoreRecreatesLocksThatWereLostButKeepsExistingOnes() {
        seatLockService.lockAll(4L, List.of(1L), "my-hold", TTL);

        boolean restored = seatLockService.restore(4L, List.of(1L, 2L), "my-hold", TTL);

        assertThat(restored).isTrue();
        assertThat(seatLockService.ownsAll(4L, List.of(1L, 2L), "my-hold")).isTrue();
    }

    @Test
    void restoreReportsSeatsTakenByAnotherHold() {
        store.tryLock(SeatLockService.key(4L, 2L), "other-hold", TTL);

        boolean restored = seatLockService.restore(4L, List.of(1L, 2L), "my-hold", TTL);

        assertThat(restored).isFalse();
    }
}
