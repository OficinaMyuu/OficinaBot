package ofc.bot.domain.tables;

import ofc.bot.domain.abstractions.InitializableTable;
import ofc.bot.domain.entity.MentionLog;
import org.jetbrains.annotations.NotNull;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Query;
import org.jooq.impl.SQLDataType;

import static ofc.bot.domain.tables.UsersTable.USERS;

public class MentionsLogTable extends InitializableTable<MentionLog> {
    public static final MentionsLogTable MENTIONS_LOG = new MentionsLogTable();

    public final Field<Integer> ID =        newField("id",                SQLDataType.INTEGER.identity(true));
    public final Field<Long> MSG_ID =       newField("message_id",        SQLDataType.BIGINT.notNull());
    public final Field<Long> AUTHOR_ID =    newField("author_id",         SQLDataType.BIGINT.notNull());
    public final Field<Long> MENTIONED_ID = newField("mentioned_user_id", SQLDataType.BIGINT.notNull());
    public final Field<Long> CREATED_AT =   newField("created_at",        SQLDataType.BIGINT.notNull());

    public MentionsLogTable() {
        super("mentions_log");
    }

    @Override
    public Query getSchema(@NotNull DSLContext ctx) {
        return ctx.createTableIfNotExists(this)
                .primaryKey(ID)
                .columns(fields())
                .constraints(
                        foreignKey(AUTHOR_ID).references(USERS, USERS.ID),
                        foreignKey(MENTIONED_ID).references(USERS, USERS.ID)
                );
    }

    @NotNull
    @Override
    public Class<MentionLog> getRecordType() {
        return MentionLog.class;
    }
}