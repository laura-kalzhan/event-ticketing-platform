package kz.kimep.ticketing.catalog.api;

import java.io.Serializable;
import kz.kimep.ticketing.catalog.domain.Venue;

public record VenueSummary(Long id, String name, String city, int totalCapacity) implements Serializable {

    public static VenueSummary from(Venue venue) {
        return new VenueSummary(venue.getId(), venue.getName(), venue.getCity(), venue.getTotalCapacity());
    }
}
