package ofc.bot.handlers;

import net.dv8tion.jda.internal.utils.Checks;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public final class ThrottledAction<T> implements AutoCloseable {
    private final ScheduledExecutorService scheduler;
    private final AtomicReference<T> latestValue = new AtomicReference<>();
    private final AtomicBoolean scheduled = new AtomicBoolean(false);
    private final Consumer<? super T> action;
    private final long intervalMillis;
    private volatile boolean closed;

    public ThrottledAction(@NotNull Duration interval, @NotNull Consumer<? super T> action) {
        this(interval, action, Executors.newSingleThreadScheduledExecutor());
    }

    ThrottledAction(
            @NotNull Duration interval,
            @NotNull Consumer<? super T> action,
            @NotNull ScheduledExecutorService scheduler
    ) {
        Checks.notNull(interval, "Interval");
        Checks.notNull(action, "Action");
        Checks.notNull(scheduler, "Scheduler");
        Checks.check(!interval.isZero() && !interval.isNegative(), "Interval must be positive");

        this.intervalMillis = interval.toMillis();
        this.action = action;
        this.scheduler = scheduler;
    }

    public static ThrottledAction<Runnable> forRunnable(@NotNull Duration interval) {
        return new ThrottledAction<>(interval, Runnable::run);
    }

    public void post(@NotNull T value) {
        Objects.requireNonNull(value, "value");

        if (closed) {
            throw new IllegalStateException("ThrottledAction is closed");
        }

        latestValue.set(value);
        scheduleIfNeeded();
    }

    public void shutdown() {
        closed = true;
        latestValue.set(null);
        scheduler.shutdown();
    }

    @Override
    public void close() {
        shutdown();
    }

    private void scheduleIfNeeded() {
        if (scheduled.compareAndSet(false, true)) {
            scheduler.schedule(this::flush, intervalMillis, TimeUnit.MILLISECONDS);
        }
    }

    private void flush() {
        T value = latestValue.getAndSet(null);
        try {
            if (value != null && !closed) {
                action.accept(value);
            }
        } finally {
            scheduled.set(false);

            if (!closed && latestValue.get() != null) {
                scheduleIfNeeded();
            }
        }
    }
}
