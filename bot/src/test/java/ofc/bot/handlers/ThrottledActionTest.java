package ofc.bot.handlers;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class ThrottledActionTest {
    @Test
    void shouldExecuteOnlyLatestPostedValueForOneWindow() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        List<String> executed = new ArrayList<>();

        try (ThrottledAction<String> action = new ThrottledAction<>(Duration.ofMillis(25), value -> {
            executed.add(value);
            latch.countDown();
        })) {
            action.post("first");
            action.post("second");
            action.post("third");

            assertTrue(latch.await(1, TimeUnit.SECONDS));
            Thread.sleep(80);

            assertEquals(List.of("third"), executed);
        }
    }

    @Test
    void shouldScheduleAnotherWindowWhenValueArrivesDuringExecution() throws Exception {
        CountDownLatch firstStarted = new CountDownLatch(1);
        CountDownLatch allowFirstToFinish = new CountDownLatch(1);
        CountDownLatch secondFinished = new CountDownLatch(1);
        List<String> executed = new ArrayList<>();

        try (ThrottledAction<String> action = new ThrottledAction<>(Duration.ofMillis(20), value -> {
            executed.add(value);
            firstStarted.countDown();
            if ("first".equals(value)) {
                try {
                    assertTrue(allowFirstToFinish.await(1, TimeUnit.SECONDS));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            } else {
                secondFinished.countDown();
            }
        })) {
            action.post("first");
            assertTrue(firstStarted.await(1, TimeUnit.SECONDS));

            action.post("second");
            allowFirstToFinish.countDown();

            assertTrue(secondFinished.await(1, TimeUnit.SECONDS));
            assertEquals(List.of("first", "second"), executed);
        }
    }
}
