package kz.kimep.ticketing.catalog.api;

import java.io.Serializable;
import java.time.Instant;
import kz.kimep.ticketing.catalog.domain.EventStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;

public record EventFilter(
        String city,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
        EventStatus status) implements Serializable {

    public static EventFilter none() {
        return new EventFilter(null, null, null, null);
    }

    public String cacheKey(Pageable pageable) {
        return city + "|" + from + "|" + to + "|" + status + "|" + pageable.getPageNumber() + "|"
                + pageable.getPageSize();
    }
}
