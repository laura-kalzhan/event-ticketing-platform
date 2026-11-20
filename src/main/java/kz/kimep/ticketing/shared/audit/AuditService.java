package kz.kimep.ticketing.shared.audit;

import java.time.Clock;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditEntryRepository auditEntryRepository;
    private final Clock clock;

    @Transactional
    public void record(Long actorId, String action, String entityType, Object entityId) {
        AuditEntry entry = new AuditEntry();
        entry.setActorId(actorId);
        entry.setAction(action);
        entry.setEntityType(entityType);
        entry.setEntityId(String.valueOf(entityId));
        entry.setCreatedAt(clock.instant());
        auditEntryRepository.save(entry);
    }
}
