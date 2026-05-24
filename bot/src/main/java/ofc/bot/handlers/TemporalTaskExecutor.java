package ofc.bot.handlers;

import java.util.Queue;
import java.util.concurrent.*;

public class TemporalTaskExecutor {
    private final ScheduledExecutorService scheduler;
    private final Queue<Runnable> taskQueue;
    private final int interval;

    public TemporalTaskExecutor(int intervalMillis, int threads) {
        if (intervalMillis <= 0)
            throw new IllegalArgumentException("Task Interval cannot be less than or equal to 0, provided: " + intervalMillis);

        this.scheduler = Executors.newScheduledThreadPool(threads);
        this.taskQueue = new LinkedBlockingQueue<>();
        this.interval = intervalMillis;
    }

    public TemporalTaskExecutor(int intervalMillis) {
        this(intervalMillis, 1);
    }

    public void addTask(Runnable task) {
        if (task == null)
            throw new IllegalArgumentException("Task may not be null");

        this.taskQueue.add(task);
    }

    public void run() {
        int time = interval;

        while (taskQueue.peek() != null) {
            Runnable task = taskQueue.poll();

            scheduler.schedule(task, time, TimeUnit.MILLISECONDS);
            time += interval;
        }
        this.scheduler.shutdown();
    }
}