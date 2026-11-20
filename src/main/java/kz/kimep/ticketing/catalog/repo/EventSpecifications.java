package kz.kimep.ticketing.catalog.repo;

import java.time.Instant;
import kz.kimep.ticketing.catalog.domain.Event;
import kz.kimep.ticketing.catalog.domain.EventStatus;
import org.springframework.data.jpa.domain.Specification;

public final class EventSpecifications {

    private EventSpecifications() {
    }

    public static Specification<Event> inCity(String city) {
        if (city == null || city.isBlank()) {
            return null;
        }
        return (root, query, cb) -> cb.equal(cb.lower(root.join("venue").get("city")), city.toLowerCase());
    }

    public static Specification<Event> startsFrom(Instant from) {
        if (from == null) {
            return null;
        }
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("startsAt"), from);
    }

    public static Specification<Event> startsBefore(Instant to) {
        if (to == null) {
            return null;
        }
        return (root, query, cb) -> cb.lessThan(root.get("startsAt"), to);
    }

    public static Specification<Event> isPublic() {
        return (root, query, cb) -> cb.notEqual(root.get("status"), EventStatus.DRAFT);
    }

    public static Specification<Event> hasStatus(EventStatus status) {
        if (status == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }
}
