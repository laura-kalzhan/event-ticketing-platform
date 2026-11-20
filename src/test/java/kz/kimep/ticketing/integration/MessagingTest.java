package kz.kimep.ticketing.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;
import kz.kimep.ticketing.messaging.config.RabbitTopology;
import kz.kimep.ticketing.messaging.consumer.BookingConfirmedConsumer;
import kz.kimep.ticketing.messaging.service.Notifier;
import kz.kimep.ticketing.testing.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.core.QueueInformation;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

class MessagingTest extends IntegrationTestBase {

    @MockitoSpyBean
    private Notifier notifier;

    private final BookingConfirmedConsumer consumer;
    private final RabbitTemplate rabbitTemplate;
    private final AmqpAdmin amqpAdmin;

    MessagingTest(TestRestTemplate rest, JdbcTemplate jdbc, BookingConfirmedConsumer consumer,
                  RabbitTemplate rabbitTemplate, AmqpAdmin amqpAdmin) {
        super(rest, jdbc);
        this.consumer = consumer;
        this.rabbitTemplate = rabbitTemplate;
        this.amqpAdmin = amqpAdmin;
    }

    @Test
    void redeliveredMessageChangesNothing() {
        String messageId = "test-duplicate-" + UUID.randomUUID();
        Message message = message(messageId, "B-SEED0007");
        Integer ticketsBefore = issuedTickets("B-SEED0007");

        consumer.onMessage(message);
        consumer.onMessage(message);

        verify(notifier, times(1)).send(anyString(), contains("B-SEED0007"), anyString());
        assertThat(issuedTickets("B-SEED0007")).isEqualTo(ticketsBefore);
        assertThat(jdbc.queryForObject("select count(*) from processed_messages where message_id = ?",
                Integer.class, messageId)).isEqualTo(1);
    }

    @Test
    void messageThatKeepsFailingIsParkedInTheDeadLetterQueue() {
        long before = deadLetters();

        rabbitTemplate.send(RabbitTopology.EXCHANGE, RabbitTopology.BOOKING_CONFIRMED,
                message("test-poison-" + UUID.randomUUID(), "B-DOES-NOT-EXIST"));

        await().atMost(Duration.ofSeconds(40)).until(() -> deadLetters() > before);
    }

    private long deadLetters() {
        QueueInformation info = amqpAdmin.getQueueInfo(RabbitTopology.DEAD_LETTER_QUEUE);
        return info == null ? 0 : info.getMessageCount();
    }

    private Integer issuedTickets(String bookingReference) {
        return jdbc.queryForObject("select count(*) from tickets t join bookings b on b.id = t.booking_id "
                + "where b.booking_reference = ? and t.status = 'ISSUED'", Integer.class, bookingReference);
    }

    private Message message(String messageId, String bookingReference) {
        String body = "{\"bookingReference\":\"" + bookingReference + "\"}";
        return MessageBuilder.withBody(body.getBytes(StandardCharsets.UTF_8))
                .setContentType(MessageProperties.CONTENT_TYPE_JSON)
                .setMessageId(messageId)
                .build();
    }
}
