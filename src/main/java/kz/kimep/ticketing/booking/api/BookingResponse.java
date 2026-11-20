package kz.kimep.ticketing.booking.api;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import kz.kimep.ticketing.booking.domain.Booking;

public record BookingResponse(
        String bookingReference,
        Long eventId,
        String status,
        BigDecimal subtotal,
        BigDecimal fees,
        BigDecimal total,
        String currency,
        Instant createdAt,
        Instant confirmedAt,
        Instant cancelledAt,
        List<TicketResponse> tickets) {

    public static BookingResponse from(Booking booking) {
        return new BookingResponse(booking.getBookingReference(), booking.getEventId(), booking.getStatus().name(),
                booking.getSubtotal(), booking.getFees(), booking.getTotal(), booking.getCurrency(),
                booking.getCreatedAt(), booking.getConfirmedAt(), booking.getCancelledAt(),
                booking.getTickets().stream().map(TicketResponse::from).toList());
    }
}
