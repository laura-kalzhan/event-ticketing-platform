package kz.kimep.ticketing.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;
import java.util.UUID;
import kz.kimep.ticketing.testing.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;

class AuthApiTest extends IntegrationTestBase {

    AuthApiTest(TestRestTemplate rest, JdbcTemplate jdbc) {
        super(rest, jdbc);
    }

    @Test
    void registerLoginRefreshAndLogoutWorkTogether() {
        String email = "it-" + UUID.randomUUID() + "@kimep.kz";
        ResponseEntity<JsonNode> registered = call(HttpMethod.POST, "/api/v1/auth/register",
                Map.of("email", email, "password", PASSWORD, "displayName", "IT User"), null);
        assertThat(registered.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(registered.getBody().has("passwordHash")).isFalse();

        JsonNode tokens = call(HttpMethod.POST, "/api/v1/auth/login", Map.of("email", email, "password", PASSWORD),
                null).getBody();
        String firstRefresh = tokens.get("refreshToken").asText();
        ResponseEntity<JsonNode> refreshed = call(HttpMethod.POST, "/api/v1/auth/refresh",
                Map.of("refreshToken", firstRefresh), null);
        assertThat(refreshed.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(call(HttpMethod.POST, "/api/v1/auth/refresh", Map.of("refreshToken", firstRefresh), null)
                .getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        String secondRefresh = refreshed.getBody().get("refreshToken").asText();
        String access = refreshed.getBody().get("accessToken").asText();
        assertThat(call(HttpMethod.POST, "/api/v1/auth/logout", Map.of("refreshToken", secondRefresh), access)
                .getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(call(HttpMethod.POST, "/api/v1/auth/refresh", Map.of("refreshToken", secondRefresh), null)
                .getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void wrongPasswordGets401InTheStandardErrorShape() {
        ResponseEntity<JsonNode> response = call(HttpMethod.POST, "/api/v1/auth/login",
                Map.of("email", CUSTOMER_A, "password", "not-the-password"), null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody().get("status").asInt()).isEqualTo(401);
        assertThat(response.getBody().get("message").asText()).isNotBlank();
        assertThat(response.getBody().get("path").asText()).isEqualTo("/api/v1/auth/login");
    }

    @Test
    void protectedEndpointWithoutTokenGets401() {
        assertThat(call(HttpMethod.GET, "/api/v1/bookings", null, null).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void customerCannotCreateAVenue() {
        ResponseEntity<JsonNode> response = call(HttpMethod.POST, "/api/v1/venues", Map.of("name", "X",
                "addressLine", "Y", "city", "Almaty", "timezone", "Asia/Almaty", "sections", java.util.List.of(
                        Map.of("name", "S", "rowCount", 1, "seatsPerRow", 1, "priceTier", "STANDARD"))),
                login(CUSTOMER_A));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void invalidRegistrationListsTheBadFields() {
        ResponseEntity<JsonNode> response = call(HttpMethod.POST, "/api/v1/auth/register",
                Map.of("email", "not-an-email", "password", "short", "displayName", ""), null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().get("fieldErrors")).hasSize(3);
    }
}
