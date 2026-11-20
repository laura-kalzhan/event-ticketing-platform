package kz.kimep.ticketing.messaging.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitTopology {

    public static final String EXCHANGE = "ticketing.events";
    public static final String DEAD_LETTER_EXCHANGE = "ticketing.dlx";
    public static final String DEAD_LETTER_QUEUE = "ticketing.dlq";

    public static final String BOOKING_CONFIRMED = "booking.confirmed";
    public static final String BOOKING_CANCELLED = "booking.cancelled";
    public static final String PAYMENT_FAILED = "payment.failed";

    public static final String BOOKING_CONFIRMED_QUEUE = "ticketing.booking.confirmed";
    public static final String BOOKING_CANCELLED_QUEUE = "ticketing.booking.cancelled";
    public static final String PAYMENT_FAILED_QUEUE = "ticketing.payment.failed";

    @Bean
    public Declarables ticketingTopology() {
        TopicExchange exchange = new TopicExchange(EXCHANGE, true, false);
        FanoutExchange deadLetterExchange = new FanoutExchange(DEAD_LETTER_EXCHANGE, true, false);
        Queue deadLetterQueue = QueueBuilder.durable(DEAD_LETTER_QUEUE).build();
        Queue confirmed = workQueue(BOOKING_CONFIRMED_QUEUE);
        Queue cancelled = workQueue(BOOKING_CANCELLED_QUEUE);
        Queue failed = workQueue(PAYMENT_FAILED_QUEUE);
        return new Declarables(exchange, deadLetterExchange, deadLetterQueue, confirmed, cancelled, failed,
                bind(confirmed, exchange, BOOKING_CONFIRMED),
                bind(cancelled, exchange, BOOKING_CANCELLED),
                bind(failed, exchange, PAYMENT_FAILED),
                BindingBuilder.bind(deadLetterQueue).to(deadLetterExchange));
    }

    private Queue workQueue(String name) {
        return QueueBuilder.durable(name).deadLetterExchange(DEAD_LETTER_EXCHANGE).build();
    }

    private Binding bind(Queue queue, TopicExchange exchange, String routingKey) {
        return BindingBuilder.bind(queue).to(exchange).with(routingKey);
    }
}
