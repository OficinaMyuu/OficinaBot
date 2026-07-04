package ofc.bot.domain.tables;

import ofc.bot.domain.abstractions.InitializableTable;
import ofc.bot.domain.entity.AppUserBan;
import org.jetbrains.annotations.NotNull;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Query;
import org.jooq.impl.SQLDataType;

public class AppUsersBanTable extends InitializableTable<AppUserBan> {
    public static final AppUsersBanTable APP_USERS_BAN = new AppUsersBanTable();

    public final Field<Integer> ID      = newField("id",         SQLDataType.INTEGER.identity(true));
    public final Field<Long> USER_ID    = newField("user_id",    SQLDataType.BIGINT.notNull());
    public final Field<String> REASON   = newField("reason",     SQLDataType.VARCHAR(255).notNull());
    public final Field<Long> EXPIRES_AT = newField("expires_at", SQLDataType.BIGINT.notNull());
    public final Field<Long> BANNED_AT  = newField("banned_at",  SQLDataType.BIGINT.notNull());

    public AppUsersBanTable() {
        super("application_users_ban");
    }

    @NotNull
    @Override
    public Class<AppUserBan> getRecordType() {
        return AppUserBan.class;
    }

    @Override
    public Query getSchema(@NotNull DSLContext ctx) {
        return ctx.createTableIfNotExists(this)
                .primaryKey(ID)
                .check(EXPIRES_AT.gt(BANNED_AT))
                .columns(fields());
    }
}