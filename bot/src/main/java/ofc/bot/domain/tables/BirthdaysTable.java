package ofc.bot.domain.tables;

import ofc.bot.domain.abstractions.InitializableTable;
import ofc.bot.domain.entity.Birthday;
import org.jetbrains.annotations.NotNull;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Query;
import org.jooq.exception.DataAccessException;

import java.time.LocalDate;

public class BirthdaysTable extends InitializableTable<Birthday> {
    public static final BirthdaysTable BIRTHDAYS = new BirthdaysTable();

    public final Field<Long> USER_ID       = newField("user_id",    BIGINT.notNull());
    public final Field<String> NAME        = newField("name",       CHAR.notNull());
    public final Field<LocalDate> BIRTHDAY = newField("birthday",   LOCALDATE.notNull());
    public final Field<Integer> ZONE_HOURS = newField("zone_hours", INT.notNull());
    public final Field<Long> CREATED_AT    = newField("created_at", BIGINT.notNull());
    public final Field<Long> UPDATED_AT    = newField("updated_at", BIGINT.notNull());

    public BirthdaysTable() {
        super("birthdays");
    }

    @NotNull
    @Override
    public Class<Birthday> getRecordType() {
        return Birthday.class;
    }

    @Override
    public Query getSchema(@NotNull DSLContext ctx) throws DataAccessException {
        return ctx.createTableIfNotExists(this)
                .primaryKey(USER_ID)
                .columns(fields());
    }
}