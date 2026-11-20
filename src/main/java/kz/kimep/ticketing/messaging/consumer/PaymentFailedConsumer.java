package kz.kimep.ticketing.messaging.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import kz.kimep.ticketing.booking.service.TicketService;
import kz.kimep.ticketing.messaging.config.RabbitTopology;
import kz.kimep.ticketing.messaging.service.ProcessedMessageGuard;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PaymentFailedConsumer {

    private final ProcessedMessageGuard guard;
    private final TicketService ticketService;
    private final ObjectMapper objectMapper;

    @RabbitListener(queues = RabbitTopology.PAYMENT_FAILED_QUEUE)
    public void onMessage(Message message) {
        IncomingEvent event = IncomingEvent.read(message, objectMapper);
        String reference = event.text("bookingReference");
        String failureCode = event.text("failureCode");
        guard.runOnce(event.messageId(), () -> ticketService.notifyPaymentFailed(reference, failureCode));
    }
}
