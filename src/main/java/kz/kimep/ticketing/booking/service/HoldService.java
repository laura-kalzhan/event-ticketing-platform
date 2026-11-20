package kz.kimep.ticketing.booking.service;

import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import kz.kimep.ticketing.booking.api.CreateHoldRequest;
import kz.kimep.ticketing.booking.api.HoldResponse;
import kz.kimep.ticketing.booking.domain.HoldStatus;
import kz.kimep.ticketing.booking.domain.SeatHold;
import kz.kimep.ticketing.booking.repo.SeatHoldRepository;
import kz.kimep.ticketing.booking.repo.TicketRepository;
import kz.kimep.ticketing.catalog.service.EventSeatCatalog;
import kz.kimep.ticketing.identity.service.AuthenticatedUser;
import kz.kimep.ticketing.shared.config.TicketingProperties;
import kz.kimep.ticketing.shared.error.BadRequestException;
import kz.kimep.ticketing.shared.error.ConflictException;
import kz.kimep.ticketing.shared.error.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class HoldService {

    private static final String SEATS_TAKEN = "One or more of these seats are already held or sold. Pick other seats.";

    private final SeatHoldRepository seatHoldRepository;
    private final TicketRepository ticketRepository;
    private final SeatLockService seatLockService;
    private final EventSeatCatalog eventSeatCatalog;
    private final TicketingProperties properties;
    private final Clock clock;

    public HoldResponse createHold(AuthenticatedUser user, CreateHoldRequest request) {
        Set<Long> seatIds = new LinkedHashSet<>(request.seatIds());
        checkSeatCount(seatIds);
        eventSeatCatalog.priceSeatsForSale(request.eventId(), seatIds);
        String reference = "H-" + UUID.randomUUID();
        Instant expiresAt = clock.instant().plus(properties.holdTtl());
        if (!seatLockService.lockAll(request.eventId(), seatIds, reference, properties.holdTtl())) {
            throw new ConflictException(SEATS_TAKEN);
        }
        try {
            SeatHold hold = saveHold(user, request.eventId(), seatIds, reference, expiresAt);
            return HoldResponse.from(hold, clock.instant());
        } catch (RuntimeException ex) {
            seatLockService.releaseAll(request.eventId(), seatIds, reference);
            throw ex;
        }
    }

    @Transactional(readOnly = true)
    public HoldResponse getHold(String reference, AuthenticatedUser user) {
        return HoldResponse.from(findOwned(reference, user), clock.instant());
    }

    @Transactional
    public void releaseHold(String reference, AuthenticatedUser user) {
        SeatHold hold = findOwned(reference, user);
        if (hold.effectiveStatus(clock.instant()) != HoldStatus.ACTIVE) {
            return;
        }
        seatLockService.releaseAll(hold.getEventId(), hold.getSeatIds(), hold.getHoldReference());
        hold.setStatus(HoldStatus.RELEASED);
    }

    private void checkSeatCount(Set<Long> seatIds) {
        if (seatIds.isEmpty() || seatIds.size() > properties.maxSeatsPerBooking()) {
            throw new BadRequestException("You can hold between 1 and " + properties.maxSeatsPerBooking()
                    + " seats at once.");
        }
    }

    private SeatHold saveHold(AuthenticatedUser user, Long eventId, Set<Long> seatIds, String reference,
                              Instant expiresAt) {
        if (ticketRepository.existsByEventIdAndSeatIdIn(eventId, seatIds)) {
            throw new ConflictException(SEATS_TAKEN);
        }
        SeatHold hold = new SeatHold();
        hold.setHoldReference(reference);
        hold.setEventId(eventId);
        hold.setUserId(user.id());
        hold.setSeatIds(new LinkedHashSet<>(seatIds));
        hold.setStatus(HoldStatus.ACTIVE);
        hold.setCreatedAt(clock.instant());
        hold.setExpiresAt(expiresAt);
        return seatHoldRepository.save(hold);
    }

    private SeatHold findOwned(String reference, AuthenticatedUser user) {
        return seatHoldRepository.findByHoldReference(reference)
                .filter(hold -> hold.isOwnedBy(user.id()))
                .orElseThrow(() -> new NotFoundException("Hold " + reference + " was not found."));
    }
}
