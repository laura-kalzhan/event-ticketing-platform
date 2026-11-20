package kz.kimep.ticketing.catalog.repo;

import java.util.List;
import kz.kimep.ticketing.catalog.domain.EventSectionPrice;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventSectionPriceRepository extends JpaRepository<EventSectionPrice, Long> {

    List<EventSectionPrice> findByEventId(Long eventId);
}
