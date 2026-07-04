package ofc.bot.domain.tables;

import ofc.bot.domain.abstractions.InitializableTable;
import ofc.bot.domain.entity.WelcomedUser;
import org.jetbrains.annotations.NotNull;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Query;
import org.jooq.impl.SQLDataType;

public class WelcomedUsersTable extends InitializableTable<WelcomedUser> {
    public static final WelcomedUsersTable WELCOMED_USERS = new WelcomedUsersTable();

    public final Field<Integer> ID        = newField("id",           SQLDataType.INTEGER.identity(true));
    public final Field<Long> GUILD_ID     = newField("guild_id",     SQLDataType.BIGINT.notNull());
    public final Field<Long> MODERATOR_ID = newField("moderator_id", SQLDataType.BIGINT.notNull());
    public final Field<Long> TARGET_ID    = newField("target_id",    SQLDataType.BIGINT.notNull());
    public final Field<String> COMMENT    = newField("comment",      SQLDataType.VARCHAR(255));
    public final Field<Long> CREATED_AT   = newField("created_at",   SQLDataType.BIGINT.notNull());

    public WelcomedUsersTable() {
        super("welcomed_users");
    }

    @Override
    public Query getSchema(@NotNull DSLContext ctx) {
        return ctx.createTableIfNotExists(this)
                .primaryKey(ID)
                .columns(fields());
    }

    @NotNull
    @Override
    public Class<WelcomedUser> getRecordType() {
        return WelcomedUser.class;
    }
}