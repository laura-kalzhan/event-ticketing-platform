package kz.kimep.ticketing.catalog.service;

import java.time.Clock;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import kz.kimep.ticketing.catalog.domain.Event;
import kz.kimep.ticketing.catalog.domain.EventSectionPrice;
import kz.kimep.ticketing.catalog.domain.Seat;
import kz.kimep.ticketing.catalog.repo.EventRepository;
import kz.kimep.ticketing.catalog.repo.EventSectionPriceRepository;
import kz.kimep.ticketing.catalog.repo.SeatRepository;
import kz.kimep.ticketing.identity.service.AuthenticatedUser;
import kz.kimep.ticketing.shared.error.BadRequestException;
import kz.kimep.ticketing.shared.error.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EventSeatCatalog {

    private final EventRepository eventRepository;
    private final SeatRepository seatRepository;
    private final EventSectionPriceRepository priceRepository;
    private final EventOwnership eventOwnership;
    private final Clock clock;

    @Transactional(readOnly = true)
    public List<PricedSeat> priceSeatsForSale(Long eventId, Collection<Long> seatIds) {
        Event event = eventRepository.findWithVenueById(eventId)
                .orElseThrow(() -> new BadRequestException("Event " + eventId + " does not exist."));
        if (!event.isOnSale(clock.instant())) {
            throw new BadRequestException("Event " + eventId + " is not on sale right now.");
        }
        List<Seat> seats = seatRepository.findWithSectionByIdIn(seatIds);
        Long venueId = event.getVenue().getId();
        boolean allInVenue = seats.size() == seatIds.size()
                && seats.stream().allMatch(seat -> seat.getSection().getVenue().getId().equals(venueId));
        if (!allInVenue) {
            throw new BadRequestException("Some seats do not exist or are not in this event's venue. "
                    + "Check the seat ids.");
        }
        Map<Long, EventSectionPrice> prices = priceRepository.findByEventId(eventId).stream()
                .collect(Collectors.toMap(price -> price.getSection().getId(), Function.identity()));
        return seats.stream().map(seat -> toPricedSeat(seat, prices)).toList();
    }

    @Transactional(readOnly = true)
    public Instant startTimeOf(Long eventId) {
        return eventRepository.findById(eventId)
                .map(Event::getStartsAt)
                .orElseThrow(() -> new NotFoundException("Event " + eventId + " was not found."));
    }

    @Transactional(readOnly = true)
    public boolean isOrganizedBy(Long eventId, Long userId) {
        return eventRepository.findById(eventId).map(event -> event.isOrganizedBy(userId)).orElse(false);
    }

    @Transactional(readOnly = true)
    public void requireOwnedEvent(Long eventId, AuthenticatedUser user) {
        eventOwnership.requireOwned(eventId, user);
    }

    private PricedSeat toPricedSeat(Seat seat, Map<Long, EventSectionPrice> prices) {
        EventSectionPrice price = prices.get(seat.getSection().getId());
        if (price == null) {
            throw new BadRequestException("Section " + seat.getSection().getName() + " has no price for this event.");
        }
        return new PricedSeat(seat.getId(), seat.getSection().getId(), seat.getSection().getName(),
                seat.getRowLabel(), seat.getSeatNumber(), price.getPrice(), price.getCurrency());
    }
}
