package kz.kimep.ticketing.booking.service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import kz.kimep.ticketing.booking.domain.Booking;
import kz.kimep.ticketing.booking.domain.BookingStatus;
import kz.kimep.ticketing.booking.repo.BookingRepository;
import kz.kimep.ticketing.catalog.service.EventSeatCatalog;
import kz.kimep.ticketing.identity.service.AuthenticatedUser;
import kz.kimep.ticketing.messaging.config.RabbitTopology;
import kz.kimep.ticketing.messaging.service.OutboxWriter;
import kz.kimep.ticketing.shared.audit.AuditService;
import kz.kimep.ticketing.shared.config.TicketingProperties;
import kz.kimep.ticketing.shared.error.ConflictException;
import kz.kimep.ticketing.shared.error.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BookingLifecycleService {

    private static final String AGGREGATE = "Booking";
    private static final String REFERENCE = "bookingReference";

    private final BookingRepository bookingRepository;
    private final EventSeatCatalog eventSeatCatalog;
    private final CancellationPolicy cancellationPolicy;
    private final OutboxWriter outboxWriter;
    private final AuditService auditService;
    private final TicketingProperties properties;
    private final Clock clock;

    @Transactional(readOnly = true)
    public PayableBooking requirePayable(String reference, Long userId) {
        Booking booking = findOwned(reference, userId);
        if (booking.getStatus() != BookingStatus.PENDING_PAYMENT) {
            throw new ConflictException("Booking " + reference + " is " + booking.getStatus() + " and cannot be paid.");
        }
        return new PayableBooking(booking.getBookingReference(), booking.getId(), booking.getTotal(),
                booking.getCurrency(), booking.getVersion());
    }

    @Transactional
    public void confirmPaid(String reference, long expectedVersion) {
        Booking booking = bookingRepository.findOneByBookingReference(reference)
                .orElseThrow(() -> new NotFoundException("Booking " + reference + " was not found."));
        if (!Objects.equals(booking.getVersion(), expectedVersion)) {
            throw new ObjectOptimisticLockingFailureException(Booking.class, booking.getId());
        }
        booking.confirm(clock.instant());
        bookingRepository.saveAndFlush(booking);
        outboxWriter.write(AGGREGATE, reference, RabbitTopology.BOOKING_CONFIRMED,
                Map.of(REFERENCE, reference, "userId", booking.getUserId()));
    }

    @Transactional
    public void cancel(String reference, AuthenticatedUser user) {
        Booking booking = findOwned(reference, user.id());
        Instant now = clock.instant();
        if (!cancellationPolicy.allows(booking, eventSeatCatalog.startTimeOf(booking.getEventId()), now)) {
            throw new ConflictException("Booking " + reference + " can no longer be cancelled. Only active bookings "
                    + "can be cancelled, and not later than " + properties.cancellationCutoff().toHours()
                    + " hours before the event.");
        }
        booking.cancel(now);
        bookingRepository.saveAndFlush(booking);
        outboxWriter.write(AGGREGATE, reference, RabbitTopology.BOOKING_CANCELLED,
                Map.of(REFERENCE, reference, "reason", "CANCELLED_BY_CUSTOMER"));
        auditService.record(user.id(), "BOOKING_CANCELLED", AGGREGATE, reference);
    }

    @Transactional
    public int expireUnpaid() {
        Instant now = clock.instant();
        List<Booking> unpaid = bookingRepository.findByStatusAndCreatedAtBefore(BookingStatus.PENDING_PAYMENT,
                now.minus(properties.paymentWindow()));
        for (Booking booking : unpaid) {
            booking.expire(now);
            outboxWriter.write(AGGREGATE, booking.getBookingReference(), RabbitTopology.BOOKING_CANCELLED,
                    Map.of(REFERENCE, booking.getBookingReference(), "reason", "PAYMENT_WINDOW_PASSED"));
        }
        return unpaid.size();
    }

    private Booking findOwned(String reference, Long userId) {
        return bookingRepository.findOneByBookingReference(reference)
                .filter(booking -> booking.isOwnedBy(userId))
                .orElseThrow(() -> new NotFoundException("Booking " + reference + " was not found."));
    }
}
