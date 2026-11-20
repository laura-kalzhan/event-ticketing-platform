package kz.kimep.ticketing.catalog.service;

public interface SeatStatusSource {

    SeatOccupancy occupancy(Long eventId);
}
