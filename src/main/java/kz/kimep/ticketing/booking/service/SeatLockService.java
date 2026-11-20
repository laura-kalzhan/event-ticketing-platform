package kz.kimep.ticketing.booking.service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SeatLockService {

    private final SeatLockStore seatLockStore;

    public static String key(Long eventId, Long seatId) {
        return "hold:" + eventId + ":" + seatId;
    }

    public boolean lockAll(Long eventId, Collection<Long> seatIds, String owner, Duration ttl) {
        List<Long> locked = new ArrayList<>();
        for (Long seatId : seatIds.stream().sorted().toList()) {
            if (!seatLockStore.tryLock(key(eventId, seatId), owner, ttl)) {
                releaseAll(eventId, locked, owner);
                return false;
            }
            locked.add(seatId);
        }
        return true;
    }

    public void releaseAll(Long eventId, Collection<Long> seatIds, String owner) {
        for (Long seatId : seatIds) {
            seatLockStore.release(key(eventId, seatId), owner);
        }
    }

    public boolean ownsAll(Long eventId, Collection<Long> seatIds, String owner) {
        return seatIds.stream().allMatch(seatId -> seatLockStore.isLockedBy(key(eventId, seatId), owner));
    }

    public boolean restore(Long eventId, Collection<Long> seatIds, String owner, Duration ttl) {
        boolean complete = true;
        for (Long seatId : seatIds) {
            String key = key(eventId, seatId);
            if (!seatLockStore.isLockedBy(key, owner) && !seatLockStore.tryLock(key, owner, ttl)) {
                complete = false;
            }
        }
        return complete;
    }
}
