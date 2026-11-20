package kz.kimep.ticketing.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import kz.kimep.ticketing.testing.IntegrationTestBase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;

class SeatConcurrencyTest extends IntegrationTestBase {

    private static final int RACES = 10;

    private final ExecutorService pool = Executors.newFixedThreadPool(2);

    SeatConcurrencyTest(TestRestTemplate rest, JdbcTemplate jdbc) {
        super(rest, jdbc);
    }

    @AfterEach
    void tearDown() {
        pool.shutdownNow();
    }

    @Test
    void twoCustomersBuyingOneSeatGetOneBookingAndOne409() throws Exception {
        String tokenA = login(CUSTOMER_A);
        String tokenB = login(CUSTOMER_B);
        List<Long> seat = freeSeats(1);

        List<ResponseEntity<JsonNode>> results = race(tokenA, tokenB, seat);

        assertThat(results).extracting(ResponseEntity::getStatusCode)
                .containsExactlyInAnyOrder(HttpStatus.CREATED, HttpStatus.CONFLICT);
        String winnerToken = results.get(0).getStatusCode() == HttpStatus.CREATED ? tokenA : tokenB;
        ResponseEntity<JsonNode> winner = results.get(0).getStatusCode() == HttpStatus.CREATED
                ? results.get(0) : results.get(1);
        assertThat(book(winnerToken, winner.getBody().get("holdReference").asText()).getStatusCode())
                .isEqualTo(HttpStatus.CREATED);
        assertThat(ticketsFor(seat.get(0))).isEqualTo(1);
    }

    @Test
    void repeatedRacesNeverGiveTheSameSeatToBoth() throws Exception {
        String tokenA = login(CUSTOMER_A);
        String tokenB = login(CUSTOMER_B);
        for (int i = 0; i < RACES; i++) {
            List<ResponseEntity<JsonNode>> results = race(tokenA, tokenB, freeSeats(1));
            assertThat(results).extracting(ResponseEntity::getStatusCode)
                    .containsExactlyInAnyOrder(HttpStatus.CREATED, HttpStatus.CONFLICT);
        }
    }

    private List<ResponseEntity<JsonNode>> race(String tokenA, String tokenB, List<Long> seat) throws Exception {
        CountDownLatch start = new CountDownLatch(1);
        Future<ResponseEntity<JsonNode>> first = pool.submit(() -> {
            start.await();
            return hold(tokenA, seat);
        });
        Future<ResponseEntity<JsonNode>> second = pool.submit(() -> {
            start.await();
            return hold(tokenB, seat);
        });
        start.countDown();
        return List.of(first.get(), second.get());
    }

    private Integer ticketsFor(Long seatId) {
        return jdbc.queryForObject("select count(*) from tickets where event_id = ? and seat_id = ?", Integer.class,
                EVENT_ON_SALE, seatId);
    }
}
