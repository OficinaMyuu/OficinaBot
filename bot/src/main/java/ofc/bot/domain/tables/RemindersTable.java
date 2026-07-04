package ofc.bot.domain.tables;

import ofc.bot.domain.abstractions.InitializableTable;
import ofc.bot.domain.entity.Reminder;
import org.jetbrains.annotations.NotNull;
import org.jooq.*;
import org.jooq.impl.SQLDataType;

public class RemindersTable extends InitializableTable<Reminder> {
    public static final RemindersTable REMINDERS = new RemindersTable();

    public final Field<Integer> ID             = newField("id",                  SQLDataType.INTEGER.identity(true));
    public final Field<Long> USER_ID           = newField("user_id",             SQLDataType.BIGINT.notNull());
    public final Field<Long> CHANNEL_ID        = newField("channel_id",          SQLDataType.BIGINT.notNull());
    public final Field<String> CHANNEL_TYPE    = newField("channel_type",        SQLDataType.VARCHAR(255).notNull());
    public final Field<String> MESSAGE         = newField("message",             SQLDataType.VARCHAR(255).notNull());
    public final Field<String> TYPE            = newField("type",                SQLDataType.VARCHAR(255).notNull());
    public final Field<Long> REMINDER_VALUE    = newField("reminder_value",      SQLDataType.BIGINT);
    public final Field<String> EXPRESSION      = newField("schedule_expression", SQLDataType.VARCHAR(255));
    public final Field<Integer> TRIGGER_TIMES  = newField("trigger_times",       SQLDataType.INTEGER.notNull());
    public final Field<Integer> TRIGGERS_LEFT  = newField("triggers_left",       SQLDataType.INTEGER.notNull());
    public final Field<Long> LAST_TRIGGERED_AT = newField("last_triggered_at",   SQLDataType.BIGINT.notNull().defaultValue(0L));
    public final Field<Boolean> EXPIRED        = newField("expired",             SQLDataType.BOOLEAN.notNull().defaultValue(false));
    public final Field<Long> CREATED_AT        = newField("created_at",          SQLDataType.BIGINT.notNull());
    public final Field<Long> UPDATED_AT        = newField("updated_at",          SQLDataType.BIGINT.notNull());

    public RemindersTable() {
        super("users_reminders");
    }

    @NotNull
    @Override
    public Class<Reminder> getRecordType() {
        return Reminder.class;
    }

    @Override
    public Query getSchema(@NotNull DSLContext ctx) {
        return ctx.createTableIfNotExists(this)
                .primaryKey(ID)
                .columns(fields())
                .check(REMINDER_VALUE.isNotNull().or(EXPRESSION.isNotNull()));
    }
}