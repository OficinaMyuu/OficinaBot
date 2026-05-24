package ofc.bot.listeners.console;

import org.jooq.ExecuteContext;
import org.jooq.ExecuteListener;

import java.util.concurrent.atomic.AtomicInteger;

public class QueryCounter implements ExecuteListener {
    private static final AtomicInteger queryCount = new AtomicInteger(0);

    @Override
    public void executeStart(ExecuteContext ctx) {
        queryCount.incrementAndGet();
    }

    /**
     * Returns the amount of executed queries so far and resets the counter.
     *
     * @return the amount of queries executed until now.
     */
    public static int poll() {
        return queryCount.getAndSet(0);
    }
}
