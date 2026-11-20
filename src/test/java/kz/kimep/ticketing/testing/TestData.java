package kz.kimep.ticketing.testing;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import kz.kimep.ticketing.booking.domain.Booking;
import kz.kimep.ticketing.booking.domain.BookingStatus;
import kz.kimep.ticketing.booking.domain.HoldStatus;
import kz.kimep.ticketing.booking.domain.SeatHold;
import kz.kimep.ticketing.catalog.service.PricedSeat;
import kz.kimep.ticketing.identity.service.AuthenticatedUser;
import kz.kimep.ticketing.shared.config.TicketingProperties;

public final class TestData {

    public static final Instant NOW = Instant.parse("2026-05-01T10:00:00Z");
    public static final AuthenticatedUser CUSTOMER = new AuthenticatedUser(4L, "customer1@kimep.kz", "CUSTOMER");
    public static final AuthenticatedUser OTHER_CUSTOMER = new AuthenticatedUser(5L, "customer2@kimep.kz", "CUSTOMER");
    public static final AuthenticatedUser ORGANIZER_ONE = new AuthenticatedUser(2L, "organizer.one@kimep.kz", "ORGANIZER");
    public static final AuthenticatedUser ORGANIZER_TWO = new AuthenticatedUser(3L, "organizer.two@kimep.kz", "ORGANIZER");
    public static final AuthenticatedUser ADMIN = new AuthenticatedUser(1L, "admin@kimep.kz", "ADMIN");

    private TestData() {
    }

    public static TicketingProperties properties() {
        return new TicketingProperties(Duration.ofMinutes(10), 8, Duration.ofMinutes(15), Duration.ofSeconds(5),
                Duration.ofHours(24));
    }

    public static SeatHold hold(String reference, Long userId, Instant expiresAt, HoldStatus status, Long... seatIds) {
        SeatHold hold = new SeatHold();
        hold.setHoldReference(reference);
        hold.setUserId(userId);
        hold.setEventId(4L);
        hold.setExpiresAt(expiresAt);
        hold.setStatus(status);
        hold.setCreatedAt(expiresAt.minus(Duration.ofMinutes(10)));
        hold.setSeatIds(new LinkedHashSet<>(Set.of(seatIds)));
        return hold;
    }

    public static Booking booking(String reference, Long userId, BookingStatus status, long version) {
        Booking booking = new Booking();
        booking.setId(100L);
        booking.setBookingReference(reference);
        booking.setUserId(userId);
        booking.setEventId(4L);
        booking.setStatus(status);
        booking.setVersion(version);
        booking.setSubtotal(new BigDecimal("24000.00"));
        booking.setFees(new BigDecimal("1200.00"));
        booking.setTotal(new BigDecimal("25200.00"));
        booking.setCurrency("KZT");
        booking.setCreatedAt(NOW);
        return booking;
    }

    public static PricedSeat seat(Long seatId, String price) {
        return new PricedSeat(seatId, 3L, "South Tribune", "A", seatId.intValue(), new BigDecimal(price), "KZT");
    }
}
