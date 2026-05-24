package ofc.bot.domain.entity;

import ofc.bot.domain.abstractions.InitializableTable;
import ofc.bot.util.Bot;
import org.jetbrains.annotations.NotNull;
import org.jooq.impl.TableRecordImpl;

public abstract class OficinaRecord<T extends OficinaRecord<T>> extends TableRecordImpl<T> {

    public OficinaRecord(InitializableTable<T> table) {
        super(table);
    }

    public long getLastUpdated() {
        return 0;
    }

    @NotNull
    public T setLastUpdated(long timestamp) {
        throw new UnsupportedOperationException("Not implemented");
    }

    /**
     * Shortcut for {@link #setLastUpdated(long)}.
     * Here you don't have to manually provide a timestamp value,
     * the field is updated through the {@link Bot#unixNow()} method call.
     *
     * @return The current instance, for chaining convenience.
     */
    @NotNull
    public final T tickUpdate() {
        return setLastUpdated(Bot.unixNow());
    }
}