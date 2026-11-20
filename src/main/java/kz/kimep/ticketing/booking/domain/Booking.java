package kz.kimep.ticketing.booking.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import kz.kimep.ticketing.shared.error.ConflictException;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "bookings")
@Getter
@Setter
@NoArgsConstructor
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String bookingReference;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BookingStatus status;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal subtotal;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal fees;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal total;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(nullable = false)
    private Instant createdAt;

    private Instant confirmedAt;

    private Instant cancelledAt;

    @Version
    private Long version;

    @Column(name = "event_id", nullable = false)
    private Long eventId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL)
    @OrderBy("id")
    private List<Ticket> tickets = new ArrayList<>();

    public void addTicket(Ticket ticket) {
        ticket.setBooking(this);
        tickets.add(ticket);
    }

    public boolean isOwnedBy(Long otherUserId) {
        return userId.equals(otherUserId);
    }

    public void confirm(Instant now) {
        if (status != BookingStatus.PENDING_PAYMENT) {
            throw new ConflictException("Booking " + bookingReference + " is " + status + " and cannot be confirmed.");
        }
        status = BookingStatus.CONFIRMED;
        confirmedAt = now;
    }

    public void cancel(Instant now) {
        status = BookingStatus.CANCELLED;
        cancelledAt = now;
    }

    public void expire(Instant now) {
        status = BookingStatus.EXPIRED;
        cancelledAt = now;
    }
}
