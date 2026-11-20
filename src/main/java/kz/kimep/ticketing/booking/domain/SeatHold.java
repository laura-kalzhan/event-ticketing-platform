package kz.kimep.ticketing.booking.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "seat_holds")
@Getter
@Setter
@NoArgsConstructor
public class SeatHold {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String holdReference;

    @Column(nullable = false)
    private Instant expiresAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private HoldStatus status;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(name = "event_id", nullable = false)
    private Long eventId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @ElementCollection
    @CollectionTable(name = "seat_hold_seats", joinColumns = @JoinColumn(name = "hold_id"))
    @Column(name = "seat_id")
    private Set<Long> seatIds = new LinkedHashSet<>();

    public boolean isOwnedBy(Long otherUserId) {
        return userId.equals(otherUserId);
    }

    public boolean isExpired(Instant now) {
        return !now.isBefore(expiresAt);
    }

    public HoldStatus effectiveStatus(Instant now) {
        if (status == HoldStatus.ACTIVE && isExpired(now)) {
            return HoldStatus.EXPIRED;
        }
        return status;
    }

    public long secondsRemaining(Instant now) {
        if (effectiveStatus(now) != HoldStatus.ACTIVE) {
            return 0;
        }
        return Duration.between(now, expiresAt).toSeconds();
    }
}
