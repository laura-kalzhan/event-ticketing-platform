package kz.kimep.ticketing.shared.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ticketing")
public record TicketingProperties(
        Duration holdTtl,
        int maxSeatsPerBooking,
        Duration paymentWindow,
        Duration paymentTimeout,
        Duration cancellationCutoff) {
}
