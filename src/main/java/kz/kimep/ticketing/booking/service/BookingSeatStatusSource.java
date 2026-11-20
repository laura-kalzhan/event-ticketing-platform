package kz.kimep.ticketing.booking.service;

import java.time.Clock;
import java.util.HashSet;
import kz.kimep.ticketing.booking.domain.HoldStatus;
import kz.kimep.ticketing.booking.repo.SeatHoldRepository;
import kz.kimep.ticketing.booking.repo.TicketRepository;
import kz.kimep.ticketing.catalog.service.SeatOccupancy;
import kz.kimep.ticketing.catalog.service.SeatStatusSource;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BookingSeatStatusSource implements SeatStatusSource {

    private final TicketRepository ticketRepository;
    private final SeatHoldRepository seatHoldRepository;
    private final Clock clock;

    @Override
    @Transactional(readOnly = true)
    public SeatOccupancy occupancy(Long eventId) {
        return new SeatOccupancy(
                new HashSet<>(seatHoldRepository.findActiveSeatIds(eventId, HoldStatus.ACTIVE, clock.instant())),
                new HashSet<>(ticketRepository.findSeatIdsByEventId(eventId)));
    }
}
