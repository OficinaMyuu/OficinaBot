package ofc.bot.domain.tables;

import ofc.bot.domain.abstractions.InitializableTable;
import ofc.bot.domain.entity.SupportTicket;
import org.jetbrains.annotations.NotNull;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Query;

import static ofc.bot.domain.tables.UsersTable.USERS;

public class SupportTicketsTable extends InitializableTable<SupportTicket> {
    public static final SupportTicketsTable SUPPORT_TICKETS = new SupportTicketsTable();

    public final Field<Integer> ID          = newField("id",           INT.identity(true));
    public final Field<String> TITLE        = newField("title",        CHAR.notNull());
    public final Field<String> DESCRIPTION  = newField("description",  CHAR.notNull());
    public final Field<Long> GUILD_ID       = newField("guild_id",     BIGINT.notNull());
    public final Field<Long> CHANNEL_ID     = newField("channel_id",   BIGINT.notNull());
    public final Field<Long> INITIATOR_ID   = newField("initiator_id", BIGINT.notNull());
    public final Field<String> CLOSE_REASON = newField("close_reason", CHAR);
    public final Field<Long> CLOSED_BY_ID   = newField("closed_by_id", BIGINT);
    public final Field<Long> CREATED_AT     = newField("created_at",   BIGINT.notNull());
    public final Field<Long> UPDATED_AT     = newField("updated_at",   BIGINT.notNull());

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
                        foreignKey(CLOSED_BY_ID).references(USERS, USERS.ID)
                );
    }

    @NotNull
    @Override
    public Class<SupportTicket> getRecordType() {
        return SupportTicket.class;
    }
}