package kz.kimep.ticketing.catalog.api;

import java.io.Serializable;
import kz.kimep.ticketing.catalog.domain.Section;

public record SectionResponse(Long id, String name, int rowCount, int seatsPerRow, String priceTier)
        implements Serializable {

    public static SectionResponse from(Section section) {
        return new SectionResponse(section.getId(), section.getName(), section.getRowCount(),
                section.getSeatsPerRow(), section.getPriceTier().name());
    }
}
