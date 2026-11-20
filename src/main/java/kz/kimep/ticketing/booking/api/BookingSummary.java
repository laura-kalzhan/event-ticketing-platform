package kz.kimep.ticketing.booking.api;

import java.math.BigDecimal;
import java.time.Instant;
import kz.kimep.ticketing.booking.domain.Booking;

public record BookingSummary(
        String bookingReference,
        Long eventId,
        String status,
        BigDecimal total,
        String currency,
        Instant createdAt) {

    public static BookingSummary from(Booking booking) {
        return new BookingSummary(booking.getBookingReference(), booking.getEventId(), booking.getStatus().name(),
                booking.getTotal(), booking.getCurrency(), booking.getCreatedAt());
    }
}
