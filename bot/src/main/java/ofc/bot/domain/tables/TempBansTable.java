package ofc.bot.domain.tables;

import ofc.bot.domain.abstractions.InitializableTable;
import ofc.bot.domain.entity.TempBan;
import org.jetbrains.annotations.NotNull;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Query;

public class TempBansTable extends InitializableTable<TempBan> {
    public static final TempBansTable TEMP_BANS = new TempBansTable();

    public final Field<Integer> ID        = newField("id",           INT.identity(true));
    public final Field<Long> USER_ID      = newField("user_id",      BIGINT.notNull());
    public final Field<Long> GUILD_ID     = newField("guild_id",     BIGINT.notNull());
    public final Field<Long> EXPIRES_AT   = newField("expires_at",   BIGINT.notNull());
    public final Field<Long> CREATED_AT   = newField("created_at",   BIGINT.notNull());

    public TempBansTable() {
        super("temp_bans");
    }

    @NotNull
    @Override
    public Class<TempBan> getRecordType() {
        return TempBan.class;
    }

    @Override
    public Query getSchema(@NotNull DSLContext ctx) {
        return ctx.createTableIfNotExists(this)
                .columns(fields())
                .unique(GUILD_ID, USER_ID)
                .check(EXPIRES_AT.gt(0L));
    }
}