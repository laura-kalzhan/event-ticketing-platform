package kz.kimep.ticketing.messaging.repo;

import kz.kimep.ticketing.messaging.domain.ProcessedMessage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessedMessageRepository extends JpaRepository<ProcessedMessage, Long> {

    boolean existsByMessageId(String messageId);
}
