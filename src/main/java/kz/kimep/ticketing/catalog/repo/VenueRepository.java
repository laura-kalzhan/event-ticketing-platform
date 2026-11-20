package kz.kimep.ticketing.catalog.repo;

import java.util.Optional;
import kz.kimep.ticketing.catalog.domain.Venue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VenueRepository extends JpaRepository<Venue, Long> {

    @Query("select distinct v from Venue v left join fetch v.sections where v.id = :id")
    Optional<Venue> findWithSectionsById(@Param("id") Long id);
}
