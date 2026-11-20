package kz.kimep.ticketing.catalog.api;

import java.io.Serializable;

public record SectionAvailability(Long sectionId, String sectionName, long totalSeats, long freeSeats)
        implements Serializable {
}
