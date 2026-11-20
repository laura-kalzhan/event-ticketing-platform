package kz.kimep.ticketing.booking.repo;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import kz.kimep.ticketing.booking.domain.BookingStatus;
import kz.kimep.ticketing.booking.domain.Ticket;
import kz.kimep.ticketing.booking.domain.TicketStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TicketRepository extends JpaRepository<Ticket, Long> {

    @EntityGraph(attributePaths = "booking")
    Optional<Ticket> findByTicketCode(String ticketCode);

    @Query("select t.seatId from Ticket t where t.eventId = :eventId")
    List<Long> findSeatIdsByEventId(@Param("eventId") Long eventId);

    boolean existsByEventIdAndSeatIdIn(Long eventId, Collection<Long> seatIds);

    long countByBookingId(Long bookingId);

    @Query("select count(t) from Ticket t where t.eventId = :eventId and t.booking.status = :status")
    long countSold(@Param("eventId") Long eventId, @Param("status") BookingStatus status);

    @Modifying
    @Query("update Ticket t set t.status = :issued, t.issuedAt = :now "
            + "where t.booking.id = :bookingId and t.status = :reserved")
    int issueForBooking(@Param("bookingId") Long bookingId, @Param("reserved") TicketStatus reserved,
                        @Param("issued") TicketStatus issued, @Param("now") Instant now);

    @Modifying
    @Query("delete from Ticket t where t.booking.id = :bookingId")
    int deleteByBookingId(@Param("bookingId") Long bookingId);
}
