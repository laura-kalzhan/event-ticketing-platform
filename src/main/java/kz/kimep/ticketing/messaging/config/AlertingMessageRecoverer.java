package kz.kimep.ticketing.messaging.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.retry.RejectAndDontRequeueRecoverer;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class AlertingMessageRecoverer extends RejectAndDontRequeueRecoverer {

    @Override
    public void recover(Message message, Throwable cause) {
        log.error("ALERT: message {} with routing key {} failed after all retries and was parked in {}",
                message.getMessageProperties().getMessageId(),
                message.getMessageProperties().getReceivedRoutingKey(),
                RabbitTopology.DEAD_LETTER_QUEUE, cause);
        super.recover(message, cause);
    }
}
