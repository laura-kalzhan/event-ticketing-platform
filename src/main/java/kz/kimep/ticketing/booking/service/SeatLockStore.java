package kz.kimep.ticketing.booking.service;

import java.time.Duration;

public interface SeatLockStore {

    boolean tryLock(String key, String owner, Duration ttl);

    void release(String key, String owner);

    boolean isLockedBy(String key, String owner);
}
