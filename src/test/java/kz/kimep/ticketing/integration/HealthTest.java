package kz.kimep.ticketing.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import kz.kimep.ticketing.testing.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.jdbc.core.JdbcTemplate;

class HealthTest extends IntegrationTestBase {

    HealthTest(TestRestTemplate rest, JdbcTemplate jdbc) {
        super(rest, jdbc);
    }

    @Test
    void healthReportsDatabaseRedisAndRabbitSeparately() {
        JsonNode health = call(HttpMethod.GET, "/actuator/health", null, null).getBody();

        assertThat(health.get("status").asText()).isEqualTo("UP");
        assertThat(health.get("components").get("db").get("status").asText()).isEqualTo("UP");
        assertThat(health.get("components").get("redis").get("status").asText()).isEqualTo("UP");
        assertThat(health.get("components").get("rabbit").get("status").asText()).isEqualTo("UP");
    }

    @Test
    void everyResponseCarriesACorrelationId() {
        assertThat(call(HttpMethod.GET, "/api/v1/events", null, null).getHeaders().getFirst("X-Correlation-Id"))
                .isNotBlank();
    }
}
