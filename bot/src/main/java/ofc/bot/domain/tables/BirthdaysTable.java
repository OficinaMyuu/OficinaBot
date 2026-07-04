package ofc.bot.domain.tables;

import ofc.bot.domain.abstractions.InitializableTable;
import ofc.bot.domain.entity.Birthday;
import org.jetbrains.annotations.NotNull;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Query;
import org.jooq.impl.SQLDataType;
import org.jooq.exception.DataAccessException;

import java.time.LocalDate;

public class BirthdaysTable extends InitializableTable<Birthday> {
    public static final BirthdaysTable BIRTHDAYS = new BirthdaysTable();

    public final Field<Long> USER_ID       = newField("user_id",    SQLDataType.BIGINT.notNull());
    public final Field<String> NAME        = newField("name",       SQLDataType.VARCHAR(255).notNull());
    public final Field<LocalDate> BIRTHDAY = newField("birthday",   SQLDataType.LOCALDATE.notNull());
    public final Field<Integer> ZONE_HOURS = newField("zone_hours", SQLDataType.INTEGER.notNull());
    public final Field<Long> CREATED_AT    = newField("created_at", SQLDataType.BIGINT.notNull());
    public final Field<Long> UPDATED_AT    = newField("updated_at", SQLDataType.BIGINT.notNull());

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