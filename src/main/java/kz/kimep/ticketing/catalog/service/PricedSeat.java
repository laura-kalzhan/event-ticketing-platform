package kz.kimep.ticketing.catalog.service;

import java.math.BigDecimal;

public record PricedSeat(
        Long seatId,
        Long sectionId,
        String sectionName,
        String rowLabel,
        int seatNumber,
        BigDecimal price,
        String currency) {
}
