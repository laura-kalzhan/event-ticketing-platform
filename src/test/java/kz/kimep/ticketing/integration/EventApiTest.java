package kz.kimep.ticketing.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.stream.StreamSupport;
import kz.kimep.ticketing.testing.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;

class EventApiTest extends IntegrationTestBase {

    EventApiTest(TestRestTemplate rest, JdbcTemplate jdbc) {
        super(rest, jdbc);
    }

    @Test
    void eventListIsPagedAndHidesDrafts() {
        JsonNode page = call(HttpMethod.GET, "/api/v1/events?size=50", null, null).getBody();

        assertThat(page.get("totalElements").asLong()).isGreaterThanOrEqualTo(9);
        assertThat(StreamSupport.stream(page.get("content").spliterator(), false))
                .noneMatch(event -> event.get("status").asText().equals("DRAFT"));
    }

    @Test
    void hugePageSizeIsCappedByTheServer() {
        JsonNode page = call(HttpMethod.GET, "/api/v1/events?size=10000", null, null).getBody();

        assertThat(page.get("size").asInt()).isEqualTo(50);
    }

    @Test
    void cityFilterOnlyReturnsEventsInThatCity() {
        JsonNode page = call(HttpMethod.GET, "/api/v1/events?city=Astana", null, null).getBody();

        assertThat(page.get("content")).isNotEmpty();
        assertThat(StreamSupport.stream(page.get("content").spliterator(), false))
                .allMatch(event -> event.get("city").asText().equals("Astana"));
    }

    @Test
    void unknownEventGives404() {
        ResponseEntity<JsonNode> response = call(HttpMethod.GET, "/api/v1/events/999999", null, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().get("error").asText()).isEqualTo("Not Found");
    }

    @Test
    void availabilityCountsEverySeatOfTheVenue() {
        JsonNode sections = call(HttpMethod.GET, "/api/v1/events/" + EVENT_ON_SALE + "/availability", null, null)
                .getBody();

        long total = StreamSupport.stream(sections.spliterator(), false)
                .mapToLong(section -> section.get("totalSeats").asLong()).sum();
        assertThat(sections).hasSize(3);
        assertThat(total).isEqualTo(1500);
    }

    @Test
    void seatMapMarksSeededTicketsAsSold() {
        JsonNode page = call(HttpMethod.GET, "/api/v1/events/" + EVENT_ON_SALE + "/seats?page=0&size=1", null, null)
                .getBody();
        JsonNode floor = page.get("content").get(0);

        assertThat(floor.get("sectionName").asText()).isEqualTo("Floor");
        assertThat(floor.get("seats").get(0).get("status").asText()).isEqualTo("SOLD");
    }
}
