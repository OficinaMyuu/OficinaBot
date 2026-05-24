package ofc.bot.domain.tables;

import ofc.bot.domain.abstractions.InitializableTable;
import ofc.bot.domain.entity.Reminder;
import org.jetbrains.annotations.NotNull;
import org.jooq.*;

public class RemindersTable extends InitializableTable<Reminder> {
    public static final RemindersTable REMINDERS = new RemindersTable();

    public final Field<Integer> ID             = newField("id",                  INT.identity(true));
    public final Field<Long> USER_ID           = newField("user_id",             BIGINT.notNull());
    public final Field<Long> CHANNEL_ID        = newField("channel_id",          BIGINT.notNull());
    public final Field<String> CHANNEL_TYPE    = newField("channel_type",        CHAR.notNull());
    public final Field<String> MESSAGE         = newField("message",             CHAR.notNull());
    public final Field<String> TYPE            = newField("type",                CHAR.notNull());
    public final Field<Long> REMINDER_VALUE    = newField("reminder_value",      BIGINT);
    public final Field<String> EXPRESSION      = newField("schedule_expression", CHAR);
    public final Field<Integer> TRIGGER_TIMES  = newField("trigger_times",       INT.notNull());
    public final Field<Integer> TRIGGERS_LEFT  = newField("triggers_left",       INT.notNull());
    public final Field<Long> LAST_TRIGGERED_AT = newField("last_triggered_at",   BIGINT.notNull().defaultValue(0L));
    public final Field<Boolean> EXPIRED        = newField("expired",             BOOL.notNull().defaultValue(false));
    public final Field<Long> CREATED_AT        = newField("created_at",          BIGINT.notNull());
    public final Field<Long> UPDATED_AT        = newField("updated_at",          BIGINT.notNull());

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