package kz.kimep.ticketing.messaging.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.util.Map;
import kz.kimep.ticketing.messaging.domain.OutboxMessage;
import kz.kimep.ticketing.messaging.repo.OutboxMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OutboxWriter {

    private final OutboxMessageRepository outboxMessageRepository;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    @Transactional(propagation = Propagation.MANDATORY)
    public void write(String aggregateType, String aggregateId, String eventType, Map<String, Object> payload) {
        OutboxMessage message = new OutboxMessage();
        message.setAggregateType(aggregateType);
        message.setAggregateId(aggregateId);
        message.setEventType(eventType);
        message.setPayload(toJson(payload));
        message.setCreatedAt(clock.instant());
        outboxMessageRepository.save(message);
    }

    private String toJson(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("Outbox payload could not be written as JSON", ex);
        }
    }
}
