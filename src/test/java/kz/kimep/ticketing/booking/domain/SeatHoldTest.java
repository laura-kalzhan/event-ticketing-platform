package kz.kimep.ticketing.booking.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import kz.kimep.ticketing.testing.TestData;
import org.junit.jupiter.api.Test;

class SeatHoldTest {

    private final Instant expiresAt = TestData.NOW.plusSeconds(600);
    private final SeatHold hold = TestData.hold("H-1", 4L, expiresAt, HoldStatus.ACTIVE, 1L);

    @Test
    void holdIsStillActiveOneSecondBeforeItsTtl() {
        Instant now = TestData.NOW.plusSeconds(599);

        assertThat(hold.effectiveStatus(now)).isEqualTo(HoldStatus.ACTIVE);
        assertThat(hold.secondsRemaining(now)).isEqualTo(1);
    }

    @Test
    void holdIsExpiredExactlyAtItsTtl() {
        Instant now = TestData.NOW.plusSeconds(600);

        assertThat(hold.effectiveStatus(now)).isEqualTo(HoldStatus.EXPIRED);
        assertThat(hold.secondsRemaining(now)).isZero();
    }

    @Test
    void consumedHoldHasNoSecondsRemaining() {
        hold.setStatus(HoldStatus.CONSUMED);

        assertThat(hold.secondsRemaining(TestData.NOW)).isZero();
        assertThat(hold.effectiveStatus(TestData.NOW)).isEqualTo(HoldStatus.CONSUMED);
    }
}
