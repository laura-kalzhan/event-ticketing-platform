package kz.kimep.ticketing.testing;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.lifecycle.Startables;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
public abstract class IntegrationTestBase {

    public static final String PASSWORD = "Password123!";
    public static final String CUSTOMER_A = "customer1@kimep.kz";
    public static final String CUSTOMER_B = "customer2@kimep.kz";
    public static final String ORGANIZER_ONE = "organizer.one@kimep.kz";
    public static final String ADMIN = "admin@kimep.kz";
    public static final long EVENT_ON_SALE = 4L;
    public static final long OTHER_ORGANIZERS_EVENT = 6L;

    private static final long TEST_SECTION = 3L;
    private static final AtomicInteger SEAT_OFFSET = new AtomicInteger();

    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16");
    static final GenericContainer<?> REDIS = new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);
    static final RabbitMQContainer RABBIT = new RabbitMQContainer("rabbitmq:3-management");

    protected final TestRestTemplate rest;
    protected final JdbcTemplate jdbc;

    protected IntegrationTestBase(TestRestTemplate rest, JdbcTemplate jdbc) {
        this.rest = rest;
        this.jdbc = jdbc;
    }

    @DynamicPropertySource
    static void containerProperties(DynamicPropertyRegistry registry) {
        Startables.deepStart(POSTGRES, REDIS, RABBIT).join();
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
        registry.add("spring.rabbitmq.host", RABBIT::getHost);
        registry.add("spring.rabbitmq.port", RABBIT::getAmqpPort);
        registry.add("spring.rabbitmq.username", RABBIT::getAdminUsername);
        registry.add("spring.rabbitmq.password", RABBIT::getAdminPassword);
    }

    protected String login(String email) {
        ResponseEntity<JsonNode> response = call(HttpMethod.POST, "/api/v1/auth/login",
                Map.of("email", email, "password", PASSWORD), null);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return response.getBody().get("accessToken").asText();
    }

    protected ResponseEntity<JsonNode> call(HttpMethod method, String url, Object body, String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (token != null) {
            headers.setBearerAuth(token);
        }
        return rest.exchange(url, method, new HttpEntity<>(body, headers), JsonNode.class);
    }

    protected List<Long> freeSeats(int count) {
        return jdbc.queryForList("select id from seats where section_id = ? order by id offset ? limit ?",
                Long.class, TEST_SECTION, SEAT_OFFSET.getAndAdd(count), count);
    }

    protected ResponseEntity<JsonNode> hold(String token, List<Long> seatIds) {
        return call(HttpMethod.POST, "/api/v1/holds", Map.of("eventId", EVENT_ON_SALE, "seatIds", seatIds), token);
    }

    protected ResponseEntity<JsonNode> book(String token, String holdReference) {
        return call(HttpMethod.POST, "/api/v1/bookings", Map.of("holdReference", holdReference), token);
    }

    protected String holdAndBook(String token, List<Long> seatIds) {
        ResponseEntity<JsonNode> hold = hold(token, seatIds);
        assertThat(hold.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        ResponseEntity<JsonNode> booking = book(token, hold.getBody().get("holdReference").asText());
        assertThat(booking.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return booking.getBody().get("bookingReference").asText();
    }

    protected ResponseEntity<JsonNode> pay(String token, String bookingReference, String cardToken) {
        return call(HttpMethod.POST, "/api/v1/payments",
                Map.of("bookingReference", bookingReference, "method", "CARD", "cardToken", cardToken), token);
    }
}
