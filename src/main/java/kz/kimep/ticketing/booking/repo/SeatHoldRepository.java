package kz.kimep.ticketing.booking.repo;

import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import kz.kimep.ticketing.booking.domain.HoldStatus;
import kz.kimep.ticketing.booking.domain.SeatHold;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SeatHoldRepository extends JpaRepository<SeatHold, Long> {

    @EntityGraph(attributePaths = "seatIds")
    Optional<SeatHold> findByHoldReference(String holdReference);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select h from SeatHold h where h.holdReference = :reference")
    Optional<SeatHold> findForUpdate(@Param("reference") String reference);

    @EntityGraph(attributePaths = "seatIds")
    List<SeatHold> findByStatusAndExpiresAtAfter(HoldStatus status, Instant now);

    @Query("select s from SeatHold h join h.seatIds s "
            + "where h.eventId = :eventId and h.status = :status and h.expiresAt > :now")
    List<Long> findActiveSeatIds(@Param("eventId") Long eventId, @Param("status") HoldStatus status,
                                 @Param("now") Instant now);

    @Modifying
    @Query("update SeatHold h set h.status = :expired where h.status = :active and h.expiresAt <= :now")
    int markExpired(@Param("active") HoldStatus active, @Param("expired") HoldStatus expired,
                    @Param("now") Instant now);
}
