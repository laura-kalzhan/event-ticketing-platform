package kz.kimep.ticketing.catalog.repo;

import kz.kimep.ticketing.catalog.domain.Section;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SectionRepository extends JpaRepository<Section, Long> {

    Page<Section> findByVenueIdOrderById(Long venueId, Pageable pageable);
}
