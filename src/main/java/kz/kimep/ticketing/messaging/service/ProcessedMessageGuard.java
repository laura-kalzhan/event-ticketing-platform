package kz.kimep.ticketing.messaging.service;

import java.time.Clock;
import kz.kimep.ticketing.messaging.domain.ProcessedMessage;
import kz.kimep.ticketing.messaging.repo.ProcessedMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProcessedMessageGuard {

    private final ProcessedMessageRepository processedMessageRepository;
    private final Clock clock;

    @Transactional
    public boolean runOnce(String messageId, Runnable action) {
        if (processedMessageRepository.existsByMessageId(messageId)) {
            log.info("Message {} was already processed, ignoring the repeat", messageId);
            return false;
        }
        ProcessedMessage processed = new ProcessedMessage();
        processed.setMessageId(messageId);
        processed.setConsumedAt(clock.instant());
        processedMessageRepository.saveAndFlush(processed);
        action.run();
        return true;
    }
}
