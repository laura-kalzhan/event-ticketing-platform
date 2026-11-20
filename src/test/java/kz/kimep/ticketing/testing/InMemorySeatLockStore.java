package kz.kimep.ticketing.testing;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import kz.kimep.ticketing.booking.service.SeatLockStore;

public class InMemorySeatLockStore implements SeatLockStore {

    private record Lock(String owner, Instant expiresAt) {
    }

    private final Map<String, Lock> locks = new ConcurrentHashMap<>();
    private final Clock clock;

    public InMemorySeatLockStore(Clock clock) {
        this.clock = clock;
    }

    @Override
    public boolean tryLock(String key, String owner, Duration ttl) {
        Instant now = clock.instant();
        AtomicBoolean won = new AtomicBoolean(false);
        locks.compute(key, (k, existing) -> {
            if (existing == null || !now.isBefore(existing.expiresAt())) {
                won.set(true);
                return new Lock(owner, now.plus(ttl));
            }
            return existing;
        });
        return won.get();
    }

    @Override
    public void release(String key, String owner) {
        locks.computeIfPresent(key, (k, existing) -> existing.owner().equals(owner) ? null : existing);
    }

    @Override
    public boolean isLockedBy(String key, String owner) {
        return owner.equals(ownerOf(key));
    }

    public String ownerOf(String key) {
        Lock lock = locks.get(key);
        if (lock == null || !clock.instant().isBefore(lock.expiresAt())) {
            return null;
        }
        return lock.owner();
    }

    public long activeCount() {
        return locks.keySet().stream().filter(key -> ownerOf(key) != null).count();
    }
}
