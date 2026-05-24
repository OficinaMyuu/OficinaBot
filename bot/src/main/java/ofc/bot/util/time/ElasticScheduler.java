package ofc.bot.util.time;

import java.util.concurrent.*;

public class ElasticScheduler {
    private final ScheduledExecutorService scheduler;
    private final Runnable task;
    private final long initialDelayMillis;
    private ScheduledFuture<?> future;

    public ElasticScheduler(Runnable task, long delayMillis) {
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
        this.task = task;
        this.initialDelayMillis = delayMillis;
    }

    public ElasticScheduler(Runnable task, long delay, TimeUnit unit) {
        this(task, unit.toMillis(delay));
    }

    public synchronized void start() {
        if (future != null && !future.isDone()) return;
        this.future = scheduler.schedule(task, initialDelayMillis, TimeUnit.MILLISECONDS);
    }

    /**
     * Resets the timer to its initial delay.
     */
    public synchronized void reset() {
        if (future != null) {
            future.cancel(false);
        }
        this.future = scheduler.schedule(task, initialDelayMillis, TimeUnit.MILLISECONDS);
    }

    public synchronized void add(long millis) {
        if (future != null) {
            long remaining = future.getDelay(TimeUnit.MILLISECONDS);
            future.cancel(false);
            future = scheduler.schedule(task, remaining + millis, TimeUnit.MILLISECONDS);
        } else {
            future = scheduler.schedule(task, millis, TimeUnit.MILLISECONDS);
        }
    }

    public synchronized void cancel() {
        if (future != null) {
            future.cancel(false);
        }
    }

    public void shutdown() {
        this.scheduler.shutdown();
    }
}