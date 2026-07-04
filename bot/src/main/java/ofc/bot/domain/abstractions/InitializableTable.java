package ofc.bot.domain.abstractions;

import ofc.bot.domain.entity.OficinaRecord;
import org.jetbrains.annotations.NotNull;
import org.jooq.*;
import org.jooq.impl.DSL;
import org.jooq.impl.TableImpl;

public abstract class InitializableTable<R extends OficinaRecord<?>> extends TableImpl<R> {
    public InitializableTable(@NotNull String tableName) {
        super(DSL.name(tableName));
    }

    protected final <T> TableField<R, T> newField(String name, DataType<T> type) {
        return createField(DSL.name(name), type);
    }

    /**
     * Initializes (or attempts to) the current table in the database
     * for the provided context.
     * <p>
     * This method should not suppress, ignore or handle any exceptions.
     *
     * @param ctx The executable database context.
     * @return An executable {@link Query} scoped to the provided context to create the table.
     */
    abstract public Query getSchema(@NotNull DSLContext ctx);

    protected <T> ConstraintForeignKeyReferencesStep1<T> foreignKey(Field<T> key) {
         return DSL.foreignKey(key);
    }

    @NotNull
    public abstract Class<R> getRecordType();
}
