package kz.kimep.ticketing.catalog.api;

import java.io.Serializable;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import kz.kimep.ticketing.catalog.domain.Event;

public record EventResponse(
        Long id,
        String title,
        String description,
        Instant startsAt,
        Instant doorsOpenAt,
        Instant salesOpenAt,
        Instant salesCloseAt,
        String status,
        Long organizerId,
        VenueSummary venue,
        List<PriceResponse> prices) implements Serializable {

    public static EventResponse from(Event event) {
        List<PriceResponse> prices = event.getPrices().stream()
                .map(PriceResponse::from)
                .sorted(Comparator.comparing(PriceResponse::sectionId))
                .toList();
        return new EventResponse(event.getId(), event.getTitle(), event.getDescription(), event.getStartsAt(),
                event.getDoorsOpenAt(), event.getSalesOpenAt(), event.getSalesCloseAt(), event.getStatus().name(),
                event.getOrganizerId(), VenueSummary.from(event.getVenue()), prices);
    }
}
