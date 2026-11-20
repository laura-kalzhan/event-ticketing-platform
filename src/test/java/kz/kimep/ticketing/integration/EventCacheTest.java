package kz.kimep.ticketing.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;
import java.util.Set;
import kz.kimep.ticketing.shared.config.CacheConfig;
import kz.kimep.ticketing.testing.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.cache.CacheManager;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;

class EventCacheTest extends IntegrationTestBase {

    private static final long EVENT = 5L;

    private final CacheManager cacheManager;

    EventCacheTest(TestRestTemplate rest, JdbcTemplate jdbc, CacheManager cacheManager) {
        super(rest, jdbc);
        this.cacheManager = cacheManager;
    }

    @Test
    void cachedEventIsServedFromRedisUntilAnUpdateEvictsIt() {
        cacheManager.getCache(CacheConfig.EVENT).evict(EVENT);
        JsonNode first = getEvent();
        assertThat(cacheManager.getCache(CacheConfig.EVENT).get(EVENT)).isNotNull();

        jdbc.update("update events set description = 'changed behind the cache' where id = ?", EVENT);
        assertThat(getEvent().get("description").asText()).isEqualTo(first.get("description").asText());

        Map<String, Object> update = Map.of("title", "Classical Evening (new time)",
                "description", "Updated through the API",
                "startsAt", first.get("startsAt").asText(),
                "salesOpenAt", first.get("salesOpenAt").asText(),
                "salesCloseAt", first.get("salesCloseAt").asText());
        assertThat(call(HttpMethod.PUT, "/api/v1/events/" + EVENT, update, login(ORGANIZER_ONE)).getStatusCode())
                .isEqualTo(HttpStatus.OK);

        JsonNode afterUpdate = getEvent();
        assertThat(afterUpdate.get("title").asText()).isEqualTo("Classical Evening (new time)");
        assertThat(afterUpdate.get("description").asText()).isEqualTo("Updated through the API");
    }

    @Test
    void onlyTheFourAllowedCachesExist() {
        getEvent();
        call(HttpMethod.GET, "/api/v1/events/" + EVENT + "/seats", null, null);

        assertThat(cacheManager.getCacheNames()).isSubsetOf(Set.of(CacheConfig.EVENT, CacheConfig.EVENT_LIST,
                CacheConfig.VENUE, CacheConfig.AVAILABILITY));
    }

    private JsonNode getEvent() {
        return call(HttpMethod.GET, "/api/v1/events/" + EVENT, null, null).getBody();
    }
}
