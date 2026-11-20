package kz.kimep.ticketing.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.List;
import kz.kimep.ticketing.booking.service.SeatLockService;
import kz.kimep.ticketing.testing.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

@TestPropertySource(properties = "ticketing.hold-ttl=2s")
class HoldExpiryTest extends IntegrationTestBase {

    private final StringRedisTemplate redis;

    HoldExpiryTest(TestRestTemplate rest, JdbcTemplate jdbc, StringRedisTemplate redis) {
        super(rest, jdbc);
        this.redis = redis;
    }

    @Test
    void expiredHoldReturnsTheSeatToSaleWithoutAnyCleanupJob() {
        String tokenA = login(CUSTOMER_A);
        List<Long> seats = freeSeats(1);
        String reference = hold(tokenA, seats).getBody().get("holdReference").asText();

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() ->
                assertThat(call(HttpMethod.GET, "/api/v1/holds/" + reference, null, tokenA).getBody()
                        .get("status").asText()).isEqualTo("EXPIRED"));

        assertThat(redis.hasKey(SeatLockService.key(EVENT_ON_SALE, seats.get(0)))).isFalse();
        assertThat(book(tokenA, reference).getStatusCode()).isEqualTo(HttpStatus.GONE);
        assertThat(hold(login(CUSTOMER_B), seats).getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }
}
