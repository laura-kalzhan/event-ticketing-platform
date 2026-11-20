package kz.kimep.ticketing.catalog.api;

import kz.kimep.ticketing.catalog.domain.Seat;
import kz.kimep.ticketing.catalog.service.SeatOccupancy;

public record SeatView(Long seatId, String rowLabel, int seatNumber, boolean accessible, String status) {

    public static SeatView of(Seat seat, SeatOccupancy occupancy) {
        return new SeatView(seat.getId(), seat.getRowLabel(), seat.getSeatNumber(), seat.isAccessible(),
                occupancy.statusOf(seat.getId()));
    }
}
