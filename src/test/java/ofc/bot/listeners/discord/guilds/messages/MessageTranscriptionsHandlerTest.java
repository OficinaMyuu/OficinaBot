package ofc.bot.listeners.discord.guilds.messages;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class MessageTranscriptionsHandlerTest {
    @Test
    void shouldAcceptSupportedAudioExtensionsCaseInsensitively() {
        assertTrue(MessageTranscriptionsHandler.isValidAudioExtension("ogg"));
        assertTrue(MessageTranscriptionsHandler.isValidAudioExtension("MP3"));
        assertTrue(MessageTranscriptionsHandler.isValidAudioExtension("WebM"));
    }

    @Test
    void shouldRejectMissingOrUnsupportedAudioExtensions() {
        assertFalse(MessageTranscriptionsHandler.isValidAudioExtension(null));
        assertFalse(MessageTranscriptionsHandler.isValidAudioExtension(""));
        assertFalse(MessageTranscriptionsHandler.isValidAudioExtension("txt"));
    }

    @Test
    void shouldAllowOnlyOneInFlightTranscriptionPerMessage() throws Exception {
        MessageTranscriptionsHandler.TranscriptionInFlight inFlight =
                new MessageTranscriptionsHandler.TranscriptionInFlight();
        ExecutorService executor = Executors.newFixedThreadPool(16);
        CountDownLatch ready = new CountDownLatch(16);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger accepted = new AtomicInteger();
        List<Future<?>> futures = new ArrayList<>();

        try {
            for (int i = 0; i < 16; i++) {
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    assertTrue(start.await(1, TimeUnit.SECONDS));

                    if (inFlight.tryStart(42L)) {
                        accepted.incrementAndGet();
                    }

                    return null;
                }));
            }

            assertTrue(ready.await(1, TimeUnit.SECONDS));
            start.countDown();

            for (Future<?> future : futures) {
                future.get(1, TimeUnit.SECONDS);
            }
        } finally {
            executor.shutdownNow();
        }

        assertEquals(1, accepted.get());
        assertTrue(inFlight.isRunning(42L));

        inFlight.finish(42L);

        assertFalse(inFlight.isRunning(42L));
        assertTrue(inFlight.tryStart(42L));
    }
}
