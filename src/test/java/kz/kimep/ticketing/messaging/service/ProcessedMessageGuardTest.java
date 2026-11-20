package kz.kimep.ticketing.messaging.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.atomic.AtomicInteger;
import kz.kimep.ticketing.messaging.repo.ProcessedMessageRepository;
import kz.kimep.ticketing.testing.MutableClock;
import kz.kimep.ticketing.testing.TestData;
import org.junit.jupiter.api.Test;

class ProcessedMessageGuardTest {

    private final ProcessedMessageRepository repository = mock(ProcessedMessageRepository.class);
    private final ProcessedMessageGuard guard = new ProcessedMessageGuard(repository, new MutableClock(TestData.NOW));
    private final AtomicInteger runs = new AtomicInteger();

    @Test
    void newMessageIsRecordedAndHandled() {
        when(repository.existsByMessageId("outbox-1")).thenReturn(false);

        boolean handled = guard.runOnce("outbox-1", runs::incrementAndGet);

        assertThat(handled).isTrue();
        assertThat(runs).hasValue(1);
        verify(repository).saveAndFlush(any());
    }

    @Test
    void redeliveredMessageIsIgnored() {
        when(repository.existsByMessageId("outbox-1")).thenReturn(true);

        boolean handled = guard.runOnce("outbox-1", runs::incrementAndGet);

        assertThat(handled).isFalse();
        assertThat(runs).hasValue(0);
        verify(repository, never()).saveAndFlush(any());
    }
}
