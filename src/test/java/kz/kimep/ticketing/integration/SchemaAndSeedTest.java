package kz.kimep.ticketing.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import kz.kimep.ticketing.testing.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

class SchemaAndSeedTest extends IntegrationTestBase {

    SchemaAndSeedTest(TestRestTemplate rest, JdbcTemplate jdbc) {
        super(rest, jdbc);
    }

    @Test
    void uniqueConstraintOnEventAndSeatExists() {
        Integer constraints = jdbc.queryForObject("select count(*) from information_schema.table_constraints "
                + "where table_name = 'tickets' and constraint_name = 'uq_ticket_event_seat'", Integer.class);

        assertThat(constraints).isEqualTo(1);
    }

    @Test
    void databaseRefusesASecondTicketForTheSameSeat() {
        assertThatThrownBy(() -> jdbc.update("insert into tickets (ticket_code, status, price_paid, booking_id, "
                + "seat_id, event_id) select 'T-DUPLICATE', 'RESERVED', price_paid, booking_id, seat_id, event_id "
                + "from tickets where event_id = 4 limit 1"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void seedDataHasRealVolume() {
        assertThat(count("seats")).isBetween(1900, 2100);
        assertThat(count("venues")).isGreaterThanOrEqualTo(3);
        assertThat(count("events")).isGreaterThanOrEqualTo(10);
        assertThat(count("bookings")).isGreaterThanOrEqualTo(15);
        assertThat(count("users")).isGreaterThanOrEqualTo(8);
    }

    private Integer count(String table) {
        return jdbc.queryForObject("select count(*) from " + table, Integer.class);
    }
}
