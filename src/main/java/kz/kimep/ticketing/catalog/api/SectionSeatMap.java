package kz.kimep.ticketing.catalog.api;

import java.util.List;

public record SectionSeatMap(Long sectionId, String sectionName, List<SeatView> seats) {
}
