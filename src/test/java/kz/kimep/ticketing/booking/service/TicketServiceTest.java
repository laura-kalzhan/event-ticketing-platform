package kz.kimep.ticketing.booking.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import kz.kimep.ticketing.booking.domain.Booking;
import kz.kimep.ticketing.booking.domain.BookingStatus;
import kz.kimep.ticketing.booking.domain.Ticket;
import kz.kimep.ticketing.booking.domain.TicketStatus;
import kz.kimep.ticketing.booking.repo.BookingRepository;
import kz.kimep.ticketing.booking.repo.TicketRepository;
import kz.kimep.ticketing.catalog.service.EventSeatCatalog;
import kz.kimep.ticketing.identity.service.UserDirectory;
import kz.kimep.ticketing.messaging.service.Notifier;
import kz.kimep.ticketing.shared.error.NotFoundException;
import kz.kimep.ticketing.testing.MutableClock;
import kz.kimep.ticketing.testing.TestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TicketServiceTest {

    @Mock
    private TicketRepository ticketRepository;
    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private EventSeatCatalog eventSeatCatalog;
    @Mock
    private UserDirectory userDirectory;
    @Mock
    private Notifier notifier;

    private TicketService ticketService;

    @BeforeEach
    void setUp() {
        ticketService = new TicketService(ticketRepository, bookingRepository, eventSeatCatalog, userDirectory,
                notifier, new MutableClock(TestData.NOW));
    }

    @Test
    void ticketsAreNotIssuedForABookingThatIsNotConfirmed() {
        Booking booking = TestData.booking("B-1", 4L, BookingStatus.PENDING_PAYMENT, 0);
        when(bookingRepository.findOneByBookingReference("B-1")).thenReturn(Optional.of(booking));

        ticketService.issueTickets("B-1");

        verify(ticketRepository, never()).issueForBooking(anyLong(), any(), any(), any());
        verify(notifier, never()).send(anyString(), anyString(), anyString());
    }

    @Test
    void confirmedBookingGetsItsTicketsIssuedAndOneEmail() {
        Booking booking = TestData.booking("B-1", 4L, BookingStatus.CONFIRMED, 1);
        when(bookingRepository.findOneByBookingReference("B-1")).thenReturn(Optional.of(booking));
        when(userDirectory.findEmail(4L)).thenReturn("customer1@kimep.kz");

        ticketService.issueTickets("B-1");

        verify(ticketRepository).issueForBooking(100L, TicketStatus.RESERVED, TicketStatus.ISSUED, TestData.NOW);
        verify(notifier).send(org.mockito.ArgumentMatchers.eq("customer1@kimep.kz"), anyString(), anyString());
    }

    @Test
    void ticketOfAnotherCustomerIsNotFound() {
        when(ticketRepository.findByTicketCode("T-1")).thenReturn(Optional.of(ticketOwnedBy(5L)));
        when(eventSeatCatalog.isOrganizedBy(4L, 3L)).thenReturn(false);

        assertThatThrownBy(() -> ticketService.getTicket("T-1", TestData.CUSTOMER))
                .isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> ticketService.getTicket("T-1", TestData.ORGANIZER_TWO))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void organizerOfTheEventCanSeeTheTicket() {
        when(ticketRepository.findByTicketCode("T-1")).thenReturn(Optional.of(ticketOwnedBy(5L)));
        when(eventSeatCatalog.isOrganizedBy(4L, 2L)).thenReturn(true);

        assertThat(ticketService.getTicket("T-1", TestData.ORGANIZER_ONE).ticketCode()).isEqualTo("T-1");
    }

    private Ticket ticketOwnedBy(Long userId) {
        Ticket ticket = new Ticket();
        ticket.setTicketCode("T-1");
        ticket.setStatus(TicketStatus.ISSUED);
        ticket.setEventId(4L);
        ticket.setSeatId(10L);
        ticket.setBooking(TestData.booking("B-9", userId, BookingStatus.CONFIRMED, 1));
        return ticket;
    }
}
