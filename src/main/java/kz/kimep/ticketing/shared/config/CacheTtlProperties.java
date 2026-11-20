package kz.kimep.ticketing.shared.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ticketing.cache")
public record CacheTtlProperties(Duration event, Duration eventList, Duration venue, Duration availability) {
}
