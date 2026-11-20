package kz.kimep.ticketing.booking.service;

import java.time.Duration;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RedisSeatLockStore implements SeatLockStore {

    private static final RedisScript<Long> RELEASE_IF_OWNER = new DefaultRedisScript<>(
            "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end",
            Long.class);

    private final StringRedisTemplate redisTemplate;

    @Override
    public boolean tryLock(String key, String owner, Duration ttl) {
        return Boolean.TRUE.equals(redisTemplate.opsForValue().setIfAbsent(key, owner, ttl));
    }

    @Override
    public void release(String key, String owner) {
        redisTemplate.execute(RELEASE_IF_OWNER, List.of(key), owner);
    }

    @Override
    public boolean isLockedBy(String key, String owner) {
        return owner.equals(redisTemplate.opsForValue().get(key));
    }
}
