package ofc.bot.handlers;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;

public final class TemporaryStorage<V> {
    private static final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    private final Map<String, V> values;

    public TemporaryStorage() {
        this.values = new HashMap<>();
    }

    public boolean hasItem(@NotNull String key) {
        return values.containsKey(key);
    }

    @NotNull
    public ScheduledFuture<?> put(@NotNull String key, V value, long delay, @NotNull TimeUnit unit) {
        values.put(key, value);
        return executor.schedule(() -> {
            values.remove(key);
        }, delay, unit);
    }

    @NotNull
    public ScheduledFuture<?> put(@NotNull String key, V value, long delayMillis) {
        return put(key, value, delayMillis, TimeUnit.MILLISECONDS);
    }

    @NotNull
    public TemporaryStorage<V> remove(@NotNull String key) {
        values.remove(key);
        return this;
    }

    @Nullable
    public V find(@NotNull String key) {
        return values.get(key);
    }

    @NotNull
    public V get(@NotNull String key) {
        V value = values.get(key);
        if (value == null)
            throw new IllegalStateException("No elements found for key " + key);

        return value;
    }

    public V getOrDef(@NotNull String key, V def) {
        V value = values.get(key);
        return value == null ? def : value;
    }
}