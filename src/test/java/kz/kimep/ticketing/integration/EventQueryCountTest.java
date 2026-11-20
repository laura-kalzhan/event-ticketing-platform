package kz.kimep.ticketing.integration;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManagerFactory;
import kz.kimep.ticketing.catalog.api.EventFilter;
import kz.kimep.ticketing.catalog.service.EventCatalogService;
import kz.kimep.ticketing.shared.config.CacheConfig;
import kz.kimep.ticketing.testing.IntegrationTestBase;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;

class EventQueryCountTest extends IntegrationTestBase {

    private final EntityManagerFactory emf;
    private final EventCatalogService events;
    private final CacheManager cacheManager;

    EventQueryCountTest(TestRestTemplate rest, JdbcTemplate jdbc, EntityManagerFactory emf,
                        EventCatalogService events, CacheManager cacheManager) {
        super(rest, jdbc);
        this.emf = emf;
        this.events = events;
        this.cacheManager = cacheManager;
    }

    @BeforeEach
    void makeSureThereAreMoreEventsThanTheBiggestPage() {
        Integer bulk = jdbc.queryForObject("select count(*) from events where title like 'Bulk event %'",
                Integer.class);
        if (bulk == 0) {
            jdbc.update("insert into venues (name, address_line, city, timezone, total_capacity) "
                    + "select 'Bulk venue ' || g, 'Street ' || g, 'Almaty', 'Asia/Almaty', 0 "
                    + "from generate_series(1, 60) g");
            jdbc.update("insert into events (title, description, starts_at, status, sales_open_at, sales_close_at, "
                    + "venue_id, organizer_id) select 'Bulk event ' || g, 'Load for the query count test', "
                    + "now() - (g || ' days')::interval, 'COMPLETED', now() - interval '400 days', "
                    + "now() - interval '399 days', v.id, 2 from generate_series(1, 60) g "
                    + "join venues v on v.name = 'Bulk venue ' || g");
        }
        cacheManager.getCache(CacheConfig.EVENT_LIST).clear();
    }

    @Test
    void listingEventsDoesNotGrowWithPageSize() {
        Statistics stats = emf.unwrap(SessionFactory.class).getStatistics();

        stats.clear();
        events.list(EventFilter.none(), PageRequest.of(0, 10));
        long forTen = stats.getPrepareStatementCount();

        stats.clear();
        events.list(EventFilter.none(), PageRequest.of(0, 50));
        long forFifty = stats.getPrepareStatementCount();

        assertThat(forTen).isLessThanOrEqualTo(3);
        assertThat(forFifty).isEqualTo(forTen);
    }
}
