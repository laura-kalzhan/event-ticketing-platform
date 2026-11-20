package kz.kimep.ticketing.booking.repo;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import kz.kimep.ticketing.booking.domain.Booking;
import kz.kimep.ticketing.booking.domain.BookingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    @EntityGraph(attributePaths = "tickets")
    Optional<Booking> findByBookingReference(String bookingReference);

    Optional<Booking> findOneByBookingReference(String bookingReference);

    Page<Booking> findByUserId(Long userId, Pageable pageable);

    List<Booking> findByStatusAndCreatedAtBefore(BookingStatus status, Instant before);

    @Query("select count(b) as bookings, sum(b.total) as revenue from Booking b "
            + "where b.eventId = :eventId and b.status = :status")
    SalesTotals salesTotals(@Param("eventId") Long eventId, @Param("status") BookingStatus status);
}
