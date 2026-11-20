package kz.kimep.ticketing.catalog.api;

import java.io.Serializable;
import java.util.List;
import kz.kimep.ticketing.catalog.domain.Venue;

public record VenueResponse(
        Long id,
        String name,
        String addressLine,
        String city,
        String timezone,
        int totalCapacity,
        List<SectionResponse> sections) implements Serializable {

    public static VenueResponse from(Venue venue) {
        List<SectionResponse> sections = venue.getSections().stream().map(SectionResponse::from).toList();
        return new VenueResponse(venue.getId(), venue.getName(), venue.getAddressLine(), venue.getCity(),
                venue.getTimezone(), venue.getTotalCapacity(), sections);
    }
}
