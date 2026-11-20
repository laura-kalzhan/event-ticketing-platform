package kz.kimep.ticketing.messaging.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.ConnectException;
import java.util.List;
import kz.kimep.ticketing.messaging.domain.OutboxMessage;
import kz.kimep.ticketing.messaging.repo.OutboxMessageRepository;
import kz.kimep.ticketing.testing.MutableClock;
import kz.kimep.ticketing.testing.TestData;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

class OutboxPublisherTest {

    private final OutboxMessageRepository repository = mock(OutboxMessageRepository.class);
    private final RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
    private final OutboxPublisher publisher = new OutboxPublisher(repository, rabbitTemplate,
            new MutableClock(TestData.NOW));

    @Test
    void rabbitBeingDownLeavesMessagesWaitingInTheOutbox() {
        OutboxMessage message = message(7L);
        when(repository.findTop50ByPublishedAtIsNullOrderByCreatedAtAsc()).thenReturn(List.of(message));
        doThrow(new AmqpConnectException(new ConnectException("refused")))
                .when(rabbitTemplate).send(anyString(), anyString(), any(Message.class));

        publisher.publishPending();

        assertThat(message.getPublishedAt()).isNull();
        verify(repository, never()).save(any());
    }

    @Test
    void publishedMessageCarriesItsIdAndIsMarkedPublished() {
        OutboxMessage message = message(7L);
        when(repository.findTop50ByPublishedAtIsNullOrderByCreatedAtAsc()).thenReturn(List.of(message));

        publisher.publishPending();

        ArgumentCaptor<Message> sent = ArgumentCaptor.forClass(Message.class);
        verify(rabbitTemplate).send(eq("ticketing.events"), eq("booking.confirmed"), sent.capture());
        assertThat(sent.getValue().getMessageProperties().getMessageId()).isEqualTo("outbox-7");
        assertThat(message.getPublishedAt()).isEqualTo(TestData.NOW);
    }

    private OutboxMessage message(Long id) {
        OutboxMessage message = new OutboxMessage();
        message.setId(id);
        message.setEventType("booking.confirmed");
        message.setPayload("{\"bookingReference\":\"B-1\"}");
        return message;
    }
}
