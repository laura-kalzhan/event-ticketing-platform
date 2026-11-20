package kz.kimep.ticketing.messaging.service;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.List;
import kz.kimep.ticketing.messaging.config.RabbitTopology;
import kz.kimep.ticketing.messaging.domain.OutboxMessage;
import kz.kimep.ticketing.messaging.repo.OutboxMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class OutboxPublisher {

    private final OutboxMessageRepository outboxMessageRepository;
    private final RabbitTemplate rabbitTemplate;
    private final Clock clock;

    @Scheduled(fixedDelayString = "${ticketing.outbox-interval:2000}")
    public void publishPending() {
        List<OutboxMessage> pending = outboxMessageRepository.findTop50ByPublishedAtIsNullOrderByCreatedAtAsc();
        for (OutboxMessage message : pending) {
            if (!publish(message)) {
                log.warn("RabbitMQ is not reachable, {} messages wait in the outbox", pending.size());
                return;
            }
        }
    }

    private boolean publish(OutboxMessage outboxMessage) {
        try {
            rabbitTemplate.send(RabbitTopology.EXCHANGE, outboxMessage.getEventType(), toAmqp(outboxMessage));
        } catch (AmqpException ex) {
            return false;
        }
        outboxMessage.setPublishedAt(clock.instant());
        outboxMessageRepository.save(outboxMessage);
        log.info("Published {} as {}", outboxMessage.getEventType(), outboxMessage.messageId());
        return true;
    }

    private Message toAmqp(OutboxMessage outboxMessage) {
        return MessageBuilder.withBody(outboxMessage.getPayload().getBytes(StandardCharsets.UTF_8))
                .setContentType(MessageProperties.CONTENT_TYPE_JSON)
                .setMessageId(outboxMessage.messageId())
                .setDeliveryMode(MessageDeliveryMode.PERSISTENT)
                .build();
    }
}
