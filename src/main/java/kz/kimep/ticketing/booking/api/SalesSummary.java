package kz.kimep.ticketing.booking.api;

import java.math.BigDecimal;

public record SalesSummary(Long eventId, long confirmedBookings, long ticketsSold, BigDecimal revenue) {
}
