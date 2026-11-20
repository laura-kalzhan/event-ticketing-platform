package kz.kimep.ticketing.booking.service;

import java.time.Clock;
import kz.kimep.ticketing.booking.api.TicketResponse;
import kz.kimep.ticketing.booking.domain.Booking;
import kz.kimep.ticketing.booking.domain.BookingStatus;
import kz.kimep.ticketing.booking.domain.Ticket;
import kz.kimep.ticketing.booking.domain.TicketStatus;
import kz.kimep.ticketing.booking.repo.BookingRepository;
import kz.kimep.ticketing.booking.repo.TicketRepository;
import kz.kimep.ticketing.catalog.service.EventSeatCatalog;
import kz.kimep.ticketing.identity.service.AuthenticatedUser;
import kz.kimep.ticketing.identity.service.UserDirectory;
import kz.kimep.ticketing.messaging.service.Notifier;
import kz.kimep.ticketing.shared.error.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TicketService {

    private final TicketRepository ticketRepository;
    private final BookingRepository bookingRepository;
    private final EventSeatCatalog eventSeatCatalog;
    private final UserDirectory userDirectory;
    private final Notifier notifier;
    private final Clock clock;

    @Transactional(readOnly = true)
    public TicketResponse getTicket(String code, AuthenticatedUser user) {
        return ticketRepository.findByTicketCode(code)
                .filter(ticket -> canView(ticket, user))
                .map(TicketResponse::from)
                .orElseThrow(() -> new NotFoundException("Ticket " + code + " was not found."));
    }

    @Transactional
    public void issueTickets(String bookingReference) {
        Booking booking = findBooking(bookingReference);
        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            log.info("Booking {} is {}, no tickets issued", bookingReference, booking.getStatus());
            return;
        }
        int issued = ticketRepository.issueForBooking(booking.getId(), TicketStatus.RESERVED, TicketStatus.ISSUED,
                clock.instant());
        notifier.send(userDirectory.findEmail(booking.getUserId()), "Your tickets for booking " + bookingReference,
                issued + " tickets were issued.");
    }

    @Transactional
    public void releaseSeats(String bookingReference) {
        Booking booking = findBooking(bookingReference);
        if (booking.getStatus() != BookingStatus.CANCELLED && booking.getStatus() != BookingStatus.EXPIRED) {
            log.warn("Booking {} is {}, seats are kept", bookingReference, booking.getStatus());
            return;
        }
        int released = ticketRepository.deleteByBookingId(booking.getId());
        notifier.send(userDirectory.findEmail(booking.getUserId()), "Booking " + bookingReference + " was cancelled",
                released + " seats went back on sale.");
    }

    @Transactional(readOnly = true)
    public void notifyPaymentFailed(String bookingReference, String failureCode) {
        Booking booking = findBooking(bookingReference);
        notifier.send(userDirectory.findEmail(booking.getUserId()), "Payment failed for booking " + bookingReference,
                "Your payment was declined (" + failureCode + "). You can try again with another card.");
    }

    private boolean canView(Ticket ticket, AuthenticatedUser user) {
        return user.isAdmin()
                || ticket.getBooking().isOwnedBy(user.id())
                || user.isOrganizer() && eventSeatCatalog.isOrganizedBy(ticket.getEventId(), user.id());
    }

    private Booking findBooking(String reference) {
        return bookingRepository.findOneByBookingReference(reference)
                .orElseThrow(() -> new IllegalStateException("Booking " + reference + " does not exist"));
    }
}
