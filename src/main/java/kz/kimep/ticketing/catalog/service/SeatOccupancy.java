package kz.kimep.ticketing.catalog.service;

import java.util.HashSet;
import java.util.Set;

public record SeatOccupancy(Set<Long> heldSeatIds, Set<Long> soldSeatIds) {

    public static final String FREE = "FREE";
    public static final String HELD = "HELD";
    public static final String SOLD = "SOLD";

    public String statusOf(Long seatId) {
        if (soldSeatIds.contains(seatId)) {
            return SOLD;
        }
        return heldSeatIds.contains(seatId) ? HELD : FREE;
    }

    public Set<Long> allTaken() {
        Set<Long> taken = new HashSet<>(heldSeatIds);
        taken.addAll(soldSeatIds);
        return taken;
    }
}
