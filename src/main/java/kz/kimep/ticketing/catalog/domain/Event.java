package kz.kimep.ticketing.catalog.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "events")
@Getter
@Setter
@NoArgsConstructor
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    private String description;

    @Column(nullable = false)
    private Instant startsAt;

    private Instant doorsOpenAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EventStatus status;

    @Column(nullable = false)
    private Instant salesOpenAt;

    @Column(nullable = false)
    private Instant salesCloseAt;

    @Column(nullable = false)
    private Instant createdAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "venue_id")
    private Venue venue;

    @Column(name = "organizer_id", nullable = false)
    private Long organizerId;

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<EventSectionPrice> prices = new LinkedHashSet<>();

    public boolean isOnSale(Instant now) {
        return status == EventStatus.ON_SALE && !now.isBefore(salesOpenAt) && now.isBefore(salesCloseAt);
    }

    public boolean isOrganizedBy(Long userId) {
        return organizerId.equals(userId);
    }

    public void addPrice(EventSectionPrice price) {
        price.setEvent(this);
        prices.add(price);
    }
}
