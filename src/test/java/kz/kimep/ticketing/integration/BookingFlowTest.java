package kz.kimep.ticketing.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Duration;
import java.util.List;
import java.util.stream.StreamSupport;
import kz.kimep.ticketing.testing.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;

class BookingFlowTest extends IntegrationTestBase {

    BookingFlowTest(TestRestTemplate rest, JdbcTemplate jdbc) {
        super(rest, jdbc);
    }

    @Test
    void declinedPaymentGives402AndLeavesTheBookingUnconfirmed() {
        String token = login(CUSTOMER_A);
        String reference = holdAndBook(token, freeSeats(2));

        assertThat(pay(token, reference, "decline_card").getStatusCode()).isEqualTo(HttpStatus.PAYMENT_REQUIRED);
        assertThat(booking(token, reference).get("status").asText()).isEqualTo("PENDING_PAYMENT");
    }

    @Test
    void paidBookingIsConfirmedAndItsTicketsAreIssuedAfterwards() {
        String token = login(CUSTOMER_A);
        String reference = holdAndBook(token, freeSeats(2));

        ResponseEntity<JsonNode> payment = pay(token, reference, "tok_visa");

        assertThat(payment.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(booking(token, reference).get("status").asText()).isEqualTo("CONFIRMED");
        await().atMost(Duration.ofSeconds(20)).untilAsserted(() ->
                assertThat(StreamSupport.stream(booking(token, reference).get("tickets").spliterator(), false))
                        .hasSize(2)
                        .allMatch(ticket -> ticket.get("status").asText().equals("ISSUED")));
    }

    @Test
    void confirmationIsWrittenToTheOutboxInTheSameTransaction() {
        String token = login(CUSTOMER_A);
        String reference = holdAndBook(token, freeSeats(1));
        pay(token, reference, "tok_visa");

        Integer rows = jdbc.queryForObject("select count(*) from outbox_messages where aggregate_id = ? "
                + "and event_type = 'booking.confirmed'", Integer.class, reference);
        assertThat(rows).isEqualTo(1);
    }

    @Test
    void anotherCustomersBookingGives404Not403() {
        String reference = holdAndBook(login(CUSTOMER_A), freeSeats(1));

        assertThat(call(HttpMethod.GET, "/api/v1/bookings/" + reference, null, login(CUSTOMER_B)).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void usingTheSameHoldTwiceGives410() {
        String token = login(CUSTOMER_A);
        String holdReference = hold(token, freeSeats(1)).getBody().get("holdReference").asText();
        book(token, holdReference);

        assertThat(book(token, holdReference).getStatusCode()).isEqualTo(HttpStatus.GONE);
    }

    @Test
    void cancelledBookingGives204AndItsSeatsGoBackOnSale() {
        String token = login(CUSTOMER_A);
        List<Long> seats = freeSeats(1);
        String reference = holdAndBook(token, seats);

        assertThat(call(HttpMethod.POST, "/api/v1/bookings/" + reference + "/cancel", null, token).getStatusCode())
                .isEqualTo(HttpStatus.NO_CONTENT);
        await().atMost(Duration.ofSeconds(20)).untilAsserted(() ->
                assertThat(hold(login(CUSTOMER_B), seats).getStatusCode()).isEqualTo(HttpStatus.CREATED));
    }

    @Test
    void myBookingsListIsPaged() {
        JsonNode page = call(HttpMethod.GET, "/api/v1/bookings?size=1000", null, login(CUSTOMER_A)).getBody();

        assertThat(page.get("size").asInt()).isEqualTo(50);
        assertThat(page.get("totalElements").asLong()).isPositive();
    }

    private JsonNode booking(String token, String reference) {
        return call(HttpMethod.GET, "/api/v1/bookings/" + reference, null, token).getBody();
    }
}
