package kz.kimep.ticketing.booking.service;

import java.time.Instant;
import kz.kimep.ticketing.booking.domain.Booking;
import kz.kimep.ticketing.booking.domain.BookingStatus;
import kz.kimep.ticketing.shared.config.TicketingProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CancellationPolicy {

    private final TicketingProperties properties;

    public boolean allows(Booking booking, Instant eventStartsAt, Instant now) {
        boolean isActive = booking.getStatus() == BookingStatus.PENDING_PAYMENT
                || booking.getStatus() == BookingStatus.CONFIRMED;
        return isActive && now.plus(properties.cancellationCutoff()).isBefore(eventStartsAt);
    }
}
