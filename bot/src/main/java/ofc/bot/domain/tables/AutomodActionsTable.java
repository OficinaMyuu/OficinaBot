package ofc.bot.domain.tables;

import ofc.bot.domain.abstractions.InitializableTable;
import ofc.bot.domain.entity.AutomodAction;
import org.jetbrains.annotations.NotNull;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Query;
import org.jooq.impl.SQLDataType;

public class AutomodActionsTable extends InitializableTable<AutomodAction> {
    public static final AutomodActionsTable AUTOMOD_ACTIONS = new AutomodActionsTable();

    public final Field<Integer> ID        = newField("id",         SQLDataType.INTEGER.identity(true));
    public final Field<Integer> THRESHOLD = newField("threshold",  SQLDataType.INTEGER.notNull());
    public final Field<Integer> DURATION  = newField("duration",   SQLDataType.INTEGER.notNull());
    public final Field<String> ACTION     = newField("action",     SQLDataType.VARCHAR(255).notNull());
    public final Field<Long> CREATED_AT   = newField("created_at", SQLDataType.BIGINT.notNull());
    public final Field<Long> UPDATED_AT   = newField("updated_at", SQLDataType.BIGINT.notNull());

    public AutomodActionsTable() {
        super("automod_actions");
    }

    @NotNull
    @Override
    public Class<AutomodAction> getRecordType() {
        return AutomodAction.class;
    }

    @Override
    public Query getSchema(@NotNull DSLContext ctx) {
        return ctx.createTableIfNotExists(this)
                .primaryKey(ID)
                .columns(fields())
                .unique(THRESHOLD)
                .unique(DURATION, ACTION);
    }
}