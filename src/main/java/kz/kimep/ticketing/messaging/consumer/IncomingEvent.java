package kz.kimep.ticketing.messaging.consumer;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Map;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;

public record IncomingEvent(String messageId, Map<String, Object> payload) {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    public static IncomingEvent read(Message message, ObjectMapper objectMapper) {
        String messageId = message.getMessageProperties().getMessageId();
        if (messageId == null) {
            throw new AmqpRejectAndDontRequeueException("Message has no message id and cannot be deduplicated");
        }
        try {
            return new IncomingEvent(messageId, objectMapper.readValue(message.getBody(), MAP_TYPE));
        } catch (IOException ex) {
            throw new AmqpRejectAndDontRequeueException("Message body is not valid JSON", ex);
        }
    }

    public String text(String key) {
        Object value = payload.get(key);
        if (value == null) {
            throw new AmqpRejectAndDontRequeueException("Message " + messageId + " has no field " + key);
        }
        return value.toString();
    }
}
