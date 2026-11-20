package kz.kimep.ticketing.messaging.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class LoggingNotifier implements Notifier {

    @Override
    public void send(String email, String subject, String body) {
        log.info("Email to {} | {} | {}", email, subject, body);
    }
}
