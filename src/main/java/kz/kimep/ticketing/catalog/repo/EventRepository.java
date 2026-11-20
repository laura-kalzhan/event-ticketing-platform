package kz.kimep.ticketing.catalog.repo;

import java.util.Optional;
import kz.kimep.ticketing.catalog.domain.Event;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EventRepository extends JpaRepository<Event, Long>, JpaSpecificationExecutor<Event> {

    @Override
    @EntityGraph(attributePaths = "venue")
    Page<Event> findAll(Specification<Event> spec, Pageable pageable);

    @Query("select distinct e from Event e join fetch e.venue "
            + "left join fetch e.prices p left join fetch p.section where e.id = :id")
    Optional<Event> findDetailedById(@Param("id") Long id);

    @EntityGraph(attributePaths = "venue")
    Optional<Event> findWithVenueById(Long id);
}
