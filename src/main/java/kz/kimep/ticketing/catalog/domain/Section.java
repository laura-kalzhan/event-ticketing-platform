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
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "sections")
@Getter
@Setter
@NoArgsConstructor
public class Section {

    private static final int LETTERS = 26;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private int rowCount;

    private int seatsPerRow;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PriceTier priceTier;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "venue_id")
    private Venue venue;

    @OneToMany(mappedBy = "section", cascade = CascadeType.ALL)
    private List<Seat> seats = new ArrayList<>();

    public int capacity() {
        return rowCount * seatsPerRow;
    }

    public void generateSeats() {
        for (int row = 0; row < rowCount; row++) {
            for (int number = 1; number <= seatsPerRow; number++) {
                Seat seat = new Seat();
                seat.setRowLabel(rowLabel(row));
                seat.setSeatNumber(number);
                seat.setSection(this);
                seats.add(seat);
            }
        }
    }

    public static String rowLabel(int index) {
        StringBuilder label = new StringBuilder();
        int value = index;
        do {
            label.insert(0, (char) ('A' + value % LETTERS));
            value = value / LETTERS - 1;
        } while (value >= 0);
        return label.toString();
    }
}
