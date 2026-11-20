package kz.kimep.ticketing.messaging.repo;

import java.util.List;
import kz.kimep.ticketing.messaging.domain.OutboxMessage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OutboxMessageRepository extends JpaRepository<OutboxMessage, Long> {

    List<OutboxMessage> findTop50ByPublishedAtIsNullOrderByCreatedAtAsc();

    List<OutboxMessage> findByAggregateIdOrderByIdAsc(String aggregateId);
}
