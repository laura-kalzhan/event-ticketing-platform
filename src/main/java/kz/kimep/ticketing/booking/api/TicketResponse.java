package kz.kimep.ticketing.booking.api;

import java.math.BigDecimal;
import java.time.Instant;
import kz.kimep.ticketing.booking.domain.Ticket;

public record TicketResponse(
        String ticketCode,
        Long eventId,
        Long seatId,
        String status,
        BigDecimal pricePaid,
        Instant issuedAt,
        Instant checkedInAt) {

    public static TicketResponse from(Ticket ticket) {
        return new TicketResponse(ticket.getTicketCode(), ticket.getEventId(), ticket.getSeatId(),
                ticket.getStatus().name(), ticket.getPricePaid(), ticket.getIssuedAt(), ticket.getCheckedInAt());
    }
}
