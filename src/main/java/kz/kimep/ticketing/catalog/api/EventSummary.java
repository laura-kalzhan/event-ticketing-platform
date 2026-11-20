package kz.kimep.ticketing.catalog.api;

import java.io.Serializable;
import java.time.Instant;
import kz.kimep.ticketing.catalog.domain.Event;

public record EventSummary(
        Long id,
        String title,
        Instant startsAt,
        String status,
        Long venueId,
        String venueName,
        String city) implements Serializable {

    public static EventSummary from(Event event) {
        return new EventSummary(event.getId(), event.getTitle(), event.getStartsAt(), event.getStatus().name(),
                event.getVenue().getId(), event.getVenue().getName(), event.getVenue().getCity());
    }
}
