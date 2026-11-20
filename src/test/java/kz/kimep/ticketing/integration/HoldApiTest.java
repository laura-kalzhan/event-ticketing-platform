package kz.kimep.ticketing.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import kz.kimep.ticketing.testing.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;

class HoldApiTest extends IntegrationTestBase {

    HoldApiTest(TestRestTemplate rest, JdbcTemplate jdbc) {
        super(rest, jdbc);
    }

    @Test
    void holdIsCreatedWith201AndALocationHeader() {
        ResponseEntity<JsonNode> response = hold(login(CUSTOMER_A), freeSeats(2));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        String reference = response.getBody().get("holdReference").asText();
        assertThat(response.getHeaders().getLocation()).hasToString("/api/v1/holds/" + reference);
        assertThat(response.getBody().get("secondsRemaining").asLong()).isBetween(590L, 600L);
    }

    @Test
    void holdingASeatSomeoneElseHoldsGives409() {
        List<Long> seats = freeSeats(1);
        hold(login(CUSTOMER_A), seats);

        assertThat(hold(login(CUSTOMER_B), seats).getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void holdingASoldSeatGives409() {
        Long soldSeat = jdbc.queryForObject("select seat_id from tickets where event_id = ? limit 1", Long.class,
                EVENT_ON_SALE);

        assertThat(hold(login(CUSTOMER_A), List.of(soldSeat)).getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void moreSeatsThanAllowedGives400() {
        assertThat(hold(login(CUSTOMER_A), freeSeats(9)).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void anotherCustomersHoldGives404() {
        String reference = hold(login(CUSTOMER_A), freeSeats(1)).getBody().get("holdReference").asText();

        assertThat(call(HttpMethod.GET, "/api/v1/holds/" + reference, null, login(CUSTOMER_B)).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void releasedHoldPutsTheSeatBackOnSale() {
        List<Long> seats = freeSeats(1);
        String tokenA = login(CUSTOMER_A);
        String reference = hold(tokenA, seats).getBody().get("holdReference").asText();

        assertThat(call(HttpMethod.DELETE, "/api/v1/holds/" + reference, null, tokenA).getStatusCode())
                .isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(hold(login(CUSTOMER_B), seats).getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }
}
