package kz.kimep.ticketing.booking.api;

import java.time.Instant;
import java.util.List;
import kz.kimep.ticketing.booking.domain.SeatHold;

public record HoldResponse(
        String holdReference,
        Long eventId,
        List<Long> seatIds,
        String status,
        Instant expiresAt,
        long secondsRemaining) {

    public static HoldResponse from(SeatHold hold, Instant now) {
        return new HoldResponse(hold.getHoldReference(), hold.getEventId(), hold.getSeatIds().stream().sorted().toList(),
                hold.effectiveStatus(now).name(), hold.getExpiresAt(), hold.secondsRemaining(now));
    }
}
