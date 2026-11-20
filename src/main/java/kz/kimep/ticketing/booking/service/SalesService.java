package kz.kimep.ticketing.booking.service;

import java.math.BigDecimal;
import kz.kimep.ticketing.booking.api.SalesSummary;
import kz.kimep.ticketing.booking.domain.BookingStatus;
import kz.kimep.ticketing.booking.repo.BookingRepository;
import kz.kimep.ticketing.booking.repo.SalesTotals;
import kz.kimep.ticketing.booking.repo.TicketRepository;
import kz.kimep.ticketing.catalog.service.EventSeatCatalog;
import kz.kimep.ticketing.identity.service.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SalesService {

    private final EventSeatCatalog eventSeatCatalog;
    private final BookingRepository bookingRepository;
    private final TicketRepository ticketRepository;

    @Transactional(readOnly = true)
    public SalesSummary summary(Long eventId, AuthenticatedUser user) {
        eventSeatCatalog.requireOwnedEvent(eventId, user);
        SalesTotals totals = bookingRepository.salesTotals(eventId, BookingStatus.CONFIRMED);
        long ticketsSold = ticketRepository.countSold(eventId, BookingStatus.CONFIRMED);
        BigDecimal revenue = totals.getRevenue() == null ? BigDecimal.ZERO : totals.getRevenue();
        return new SalesSummary(eventId, totals.getBookings(), ticketsSold, revenue);
    }
}
