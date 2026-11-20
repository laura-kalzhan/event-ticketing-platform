package kz.kimep.ticketing.catalog.repo;

import java.util.Collection;
import java.util.List;
import kz.kimep.ticketing.catalog.domain.Seat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SeatRepository extends JpaRepository<Seat, Long> {

    @Query("select s from Seat s join fetch s.section where s.id in :ids")
    List<Seat> findWithSectionByIdIn(@Param("ids") Collection<Long> ids);

    @Query("select s from Seat s where s.section.id in :sectionIds "
            + "order by s.section.id, length(s.rowLabel), s.rowLabel, s.seatNumber")
    List<Seat> findBySectionIds(@Param("sectionIds") Collection<Long> sectionIds);

    @Query("select s.section.id as sectionId, s.section.name as sectionName, count(s) as seatCount "
            + "from Seat s where s.section.venue.id = :venueId "
            + "group by s.section.id, s.section.name order by s.section.id")
    List<SectionSeatCount> countByVenue(@Param("venueId") Long venueId);

    @Query("select s.section.id as sectionId, s.section.name as sectionName, count(s) as seatCount "
            + "from Seat s where s.id in :ids group by s.section.id, s.section.name")
    List<SectionSeatCount> countBySeatIds(@Param("ids") Collection<Long> ids);
}
