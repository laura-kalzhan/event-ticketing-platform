package kz.kimep.ticketing.catalog.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class EventTest {

    private static final Instant OPEN = Instant.parse("2026-05-01T00:00:00Z");
    private static final Instant CLOSE = Instant.parse("2026-06-01T00:00:00Z");

    @Test
    void eventIsOnSaleOnlyInsideItsSalesWindow() {
        Event event = event(EventStatus.ON_SALE);

        assertThat(event.isOnSale(OPEN.minusSeconds(1))).isFalse();
        assertThat(event.isOnSale(OPEN)).isTrue();
        assertThat(event.isOnSale(CLOSE.minusSeconds(1))).isTrue();
        assertThat(event.isOnSale(CLOSE)).isFalse();
    }

    @Test
    void draftEventIsNeverOnSale() {
        assertThat(event(EventStatus.DRAFT).isOnSale(OPEN.plusSeconds(10))).isFalse();
    }

    private Event event(EventStatus status) {
        Event event = new Event();
        event.setStatus(status);
        event.setSalesOpenAt(OPEN);
        event.setSalesCloseAt(CLOSE);
        return event;
    }
}
