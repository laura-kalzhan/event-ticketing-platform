package kz.kimep.ticketing.booking.service;

import java.time.Clock;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import kz.kimep.ticketing.booking.api.BookingResponse;
import kz.kimep.ticketing.booking.api.BookingSummary;
import kz.kimep.ticketing.booking.api.CreateBookingRequest;
import kz.kimep.ticketing.booking.domain.Booking;
import kz.kimep.ticketing.booking.domain.BookingStatus;
import kz.kimep.ticketing.booking.domain.HoldStatus;
import kz.kimep.ticketing.booking.domain.SeatHold;
import kz.kimep.ticketing.booking.domain.Ticket;
import kz.kimep.ticketing.booking.domain.TicketStatus;
import kz.kimep.ticketing.booking.repo.BookingRepository;
import kz.kimep.ticketing.booking.repo.SeatHoldRepository;
import kz.kimep.ticketing.catalog.service.EventSeatCatalog;
import kz.kimep.ticketing.catalog.service.PricedSeat;
import kz.kimep.ticketing.identity.service.AuthenticatedUser;
import kz.kimep.ticketing.shared.api.PageResponse;
import kz.kimep.ticketing.shared.audit.AuditService;
import kz.kimep.ticketing.shared.error.GoneException;
import kz.kimep.ticketing.shared.error.NotFoundException;
import kz.kimep.ticketing.shared.tx.AfterCommit;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BookingService {

    private static final int REFERENCE_LENGTH = 10;

    private final SeatHoldRepository seatHoldRepository;
    private final BookingRepository bookingRepository;
    private final SeatLockService seatLockService;
    private final EventSeatCatalog eventSeatCatalog;
    private final BookingPricing bookingPricing;
    private final AfterCommit afterCommit;
    private final AuditService auditService;
    private final Clock clock;

    @Transactional
    public BookingResponse createBooking(AuthenticatedUser user, CreateBookingRequest request) {
        SeatHold hold = seatHoldRepository.findForUpdate(request.holdReference())
                .filter(found -> found.isOwnedBy(user.id()))
                .orElseThrow(() -> new NotFoundException("Hold " + request.holdReference() + " was not found."));
        requireAlive(hold);
        List<PricedSeat> seats = eventSeatCatalog.priceSeatsForSale(hold.getEventId(), hold.getSeatIds());
        Booking booking = buildBooking(hold, seats);
        bookingRepository.saveAndFlush(booking);
        hold.setStatus(HoldStatus.CONSUMED);
        auditService.record(user.id(), "BOOKING_CREATED", "Booking", booking.getBookingReference());
        Long eventId = hold.getEventId();
        Set<Long> seatIds = Set.copyOf(hold.getSeatIds());
        String holdReference = hold.getHoldReference();
        afterCommit.run(() -> seatLockService.releaseAll(eventId, seatIds, holdReference));
        return BookingResponse.from(booking);
    }

    @Transactional(readOnly = true)
    public PageResponse<BookingSummary> listMine(AuthenticatedUser user, Pageable pageable) {
        Pageable sorted = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        return PageResponse.from(bookingRepository.findByUserId(user.id(), sorted).map(BookingSummary::from));
    }

    @Transactional(readOnly = true)
    public BookingResponse getBooking(String reference, AuthenticatedUser user) {
        return bookingRepository.findByBookingReference(reference)
                .filter(booking -> booking.isOwnedBy(user.id()))
                .map(BookingResponse::from)
                .orElseThrow(() -> new NotFoundException("Booking " + reference + " was not found."));
    }

    private void requireAlive(SeatHold hold) {
        boolean isAlive = hold.getStatus() == HoldStatus.ACTIVE
                && !hold.isExpired(clock.instant())
                && seatLockService.ownsAll(hold.getEventId(), hold.getSeatIds(), hold.getHoldReference());
        if (!isAlive) {
            throw new GoneException("Hold " + hold.getHoldReference()
                    + " has expired or was already used. Hold the seats again.");
        }
    }

    private Booking buildBooking(SeatHold hold, List<PricedSeat> seats) {
        PriceTotals totals = bookingPricing.calculate(seats);
        Booking booking = new Booking();
        booking.setBookingReference("B-" + randomCode());
        booking.setStatus(BookingStatus.PENDING_PAYMENT);
        booking.setSubtotal(totals.subtotal());
        booking.setFees(totals.fees());
        booking.setTotal(totals.total());
        booking.setCurrency(totals.currency());
        booking.setCreatedAt(clock.instant());
        booking.setEventId(hold.getEventId());
        booking.setUserId(hold.getUserId());
        for (PricedSeat seat : seats) {
            Ticket ticket = new Ticket();
            ticket.setTicketCode("T-" + UUID.randomUUID());
            ticket.setStatus(TicketStatus.RESERVED);
            ticket.setPricePaid(seat.price());
            ticket.setSeatId(seat.seatId());
            ticket.setEventId(hold.getEventId());
            booking.addTicket(ticket);
        }
        return booking;
    }

    private String randomCode() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, REFERENCE_LENGTH).toUpperCase(Locale.ROOT);
    }
}
