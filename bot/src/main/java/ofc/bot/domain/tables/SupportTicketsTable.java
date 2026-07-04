package ofc.bot.domain.tables;

import ofc.bot.domain.abstractions.InitializableTable;
import ofc.bot.domain.entity.SupportTicket;
import org.jetbrains.annotations.NotNull;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Query;
import org.jooq.impl.SQLDataType;

import static ofc.bot.domain.tables.UsersTable.USERS;

public class SupportTicketsTable extends InitializableTable<SupportTicket> {
    public static final SupportTicketsTable SUPPORT_TICKETS = new SupportTicketsTable();

    public final Field<Integer> ID          = newField("id",           SQLDataType.INTEGER.identity(true));
    public final Field<String> TITLE        = newField("title",        SQLDataType.VARCHAR(255).notNull());
    public final Field<String> DESCRIPTION  = newField("description",  SQLDataType.VARCHAR(255).notNull());
    public final Field<Long> GUILD_ID       = newField("guild_id",     SQLDataType.BIGINT.notNull());
    public final Field<Long> CHANNEL_ID     = newField("channel_id",   SQLDataType.BIGINT.notNull());
    public final Field<Long> INITIATOR_ID   = newField("initiator_id", SQLDataType.BIGINT.notNull());
    public final Field<String> CLOSE_REASON = newField("close_reason", SQLDataType.VARCHAR(255));
    public final Field<Long> CLOSED_BY_ID   = newField("closed_by_id", SQLDataType.BIGINT);
    public final Field<Integer> MERGED_INTO = newField("merged_into",  SQLDataType.INTEGER);
    public final Field<Long> CREATED_AT     = newField("created_at",   SQLDataType.BIGINT.notNull());
    public final Field<Long> UPDATED_AT     = newField("updated_at",   SQLDataType.BIGINT.notNull());

    public SupportTicketsTable() {
        super("support_tickets");
    }

    @Override
    public Query getSchema(@NotNull DSLContext ctx) {
        return ctx.createTableIfNotExists(this)
                .primaryKey(ID)
                .columns(fields())
                .unique(CHANNEL_ID)
                .constraints(
                        foreignKey(INITIATOR_ID).references(USERS, USERS.ID),
                        foreignKey(CLOSED_BY_ID).references(USERS, USERS.ID),
                        foreignKey(MERGED_INTO).references(this, ID)
                );
    }

    @NotNull
    @Override
    public Class<SupportTicket> getRecordType() {
        return SupportTicket.class;
    }
}