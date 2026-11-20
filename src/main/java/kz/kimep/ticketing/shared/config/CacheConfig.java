package kz.kimep.ticketing.shared.config;

import java.time.Duration;
import org.springframework.boot.autoconfigure.cache.RedisCacheManagerBuilderCustomizer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;

@Configuration
@EnableCaching
public class CacheConfig {

    public static final String EVENT = "event";
    public static final String EVENT_LIST = "eventList";
    public static final String VENUE = "venue";
    public static final String AVAILABILITY = "availability";

    @Bean
    public RedisCacheManagerBuilderCustomizer cacheTimeToLive(CacheTtlProperties ttl) {
        return builder -> builder
                .withCacheConfiguration(EVENT, withTtl(ttl.event()))
                .withCacheConfiguration(EVENT_LIST, withTtl(ttl.eventList()))
                .withCacheConfiguration(VENUE, withTtl(ttl.venue()))
                .withCacheConfiguration(AVAILABILITY, withTtl(ttl.availability()));
    }

    private RedisCacheConfiguration withTtl(Duration ttl) {
        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(ttl)
                .disableCachingNullValues();
    }
}
