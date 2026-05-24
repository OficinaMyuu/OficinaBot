package ofc.bot.domain.tables;

import ofc.bot.domain.abstractions.InitializableTable;
import ofc.bot.domain.entity.GroupBot;
import org.jetbrains.annotations.NotNull;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Query;
import org.jooq.exception.DataAccessException;
import org.jooq.impl.SQLDataType;

public class GroupBotsTable extends InitializableTable<GroupBot> {
    public static final GroupBotsTable GROUP_BOTS = new GroupBotsTable();

    public final Field<Integer> ID          = newField("id",           SQLDataType.INTEGER.identity(true));
    public final Field<Long> BOT_ID         = newField("bot_id",       SQLDataType.BIGINT.notNull());
    public final Field<String> BOT_NAME     = newField("bot_name",     SQLDataType.CHAR.notNull());
    public final Field<String> BOT_CATEGORY = newField("bot_category", SQLDataType.CHAR.notNull());
    public final Field<Long> CREATED_AT     = newField("created_at",   SQLDataType.BIGINT.notNull());

    public GroupBotsTable() {
        super("group_bots");
    }

    @NotNull
    @Override
    public Class<GroupBot> getRecordType() {
        return GroupBot.class;
    }

    @Override
    public Query getSchema(@NotNull DSLContext ctx) throws DataAccessException {
        return ctx.createTableIfNotExists(this)
                .primaryKey(ID)
                .columns(fields());
    }
}
