package kz.kimep.ticketing.booking.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import kz.kimep.ticketing.booking.domain.HoldStatus;
import kz.kimep.ticketing.booking.domain.SeatHold;
import kz.kimep.ticketing.booking.repo.SeatHoldRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class HoldReconciler {

    private final SeatHoldRepository seatHoldRepository;
    private final SeatLockService seatLockService;
    private final Clock clock;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void restoreLocksOnStartup() {
        Instant now = clock.instant();
        markExpiredHolds();
        List<SeatHold> active = seatHoldRepository.findByStatusAndExpiresAtAfter(HoldStatus.ACTIVE, now);
        for (SeatHold hold : active) {
            Duration remaining = Duration.between(now, hold.getExpiresAt());
            boolean restored = seatLockService.restore(hold.getEventId(), hold.getSeatIds(),
                    hold.getHoldReference(), remaining);
            if (!restored) {
                log.warn("Hold {} could not get all of its seat locks back", hold.getHoldReference());
            }
        }
        log.info("Checked {} active holds against Redis", active.size());
    }

    @Scheduled(fixedDelayString = "${ticketing.hold-cleanup-interval:60000}")
    @Transactional
    public void markExpiredHolds() {
        int expired = seatHoldRepository.markExpired(HoldStatus.ACTIVE, HoldStatus.EXPIRED, clock.instant());
        if (expired > 0) {
            log.info("Marked {} holds as expired", expired);
        }
    }
}
