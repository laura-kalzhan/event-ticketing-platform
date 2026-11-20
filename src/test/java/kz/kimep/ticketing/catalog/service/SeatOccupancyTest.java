package kz.kimep.ticketing.catalog.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import org.junit.jupiter.api.Test;

class SeatOccupancyTest {

    private final SeatOccupancy occupancy = new SeatOccupancy(Set.of(1L, 2L), Set.of(2L, 3L));

    @Test
    void soldWinsOverHeldAndEverythingElseIsFree() {
        assertThat(occupancy.statusOf(1L)).isEqualTo(SeatOccupancy.HELD);
        assertThat(occupancy.statusOf(2L)).isEqualTo(SeatOccupancy.SOLD);
        assertThat(occupancy.statusOf(4L)).isEqualTo(SeatOccupancy.FREE);
    }

    @Test
    void allTakenIsTheUnionOfHeldAndSold() {
        assertThat(occupancy.allTaken()).containsExactlyInAnyOrder(1L, 2L, 3L);
    }
}
