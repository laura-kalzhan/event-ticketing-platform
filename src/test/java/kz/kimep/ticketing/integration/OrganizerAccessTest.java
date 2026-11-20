package kz.kimep.ticketing.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;
import kz.kimep.ticketing.testing.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;

class OrganizerAccessTest extends IntegrationTestBase {

    OrganizerAccessTest(TestRestTemplate rest, JdbcTemplate jdbc) {
        super(rest, jdbc);
    }

    @Test
    void organizerAGets404ForOrganizerBsSalesSummary() {
        ResponseEntity<JsonNode> response = call(HttpMethod.GET,
                "/api/v1/organizer/events/" + OTHER_ORGANIZERS_EVENT + "/sales", null, login(ORGANIZER_ONE));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void organizerSeesTheSalesOfTheirOwnEvent() {
        ResponseEntity<JsonNode> response = call(HttpMethod.GET,
                "/api/v1/organizer/events/" + EVENT_ON_SALE + "/sales", null, login(ORGANIZER_ONE));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().get("confirmedBookings").asLong()).isGreaterThanOrEqualTo(4);
        assertThat(response.getBody().get("ticketsSold").asLong()).isGreaterThanOrEqualTo(8);
    }

    @Test
    void customerCannotSeeSalesAtAll() {
        assertThat(call(HttpMethod.GET, "/api/v1/organizer/events/" + EVENT_ON_SALE + "/sales", null,
                login(CUSTOMER_A)).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void organizerCannotPublishAnotherOrganizersEvent() {
        assertThat(call(HttpMethod.POST, "/api/v1/events/10/publish", Map.of(), login(ORGANIZER_ONE))
                .getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
