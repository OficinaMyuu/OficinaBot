package ofc.bot.domain.tables;

import ofc.bot.domain.abstractions.InitializableTable;
import ofc.bot.domain.entity.MemberJoinEvent;
import org.jetbrains.annotations.NotNull;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Query;
import org.jooq.impl.SQLDataType;

public class MemberJoinEventsTable extends InitializableTable<MemberJoinEvent> {
    public static final MemberJoinEventsTable MEMBER_JOIN_EVENTS = new MemberJoinEventsTable();

    public final Field<Integer> ID         = newField("id",         SQLDataType.INTEGER.identity(true));
    public final Field<Long> GUILD_ID      = newField("guild_id",   SQLDataType.BIGINT.notNull());
    public final Field<Long> USER_ID       = newField("user_id",    SQLDataType.BIGINT.notNull());
    public final Field<Long> CREATED_AT    = newField("created_at", SQLDataType.BIGINT.notNull());

    public MemberJoinEventsTable() {
        super("member_join_events");
    }

    @Override
    public Query getSchema(@NotNull DSLContext ctx) {
        return ctx.createTableIfNotExists(this)
                .primaryKey(ID)
                .columns(fields());
    }

    @NotNull
    @Override
    public Class<MemberJoinEvent> getRecordType() {
        return MemberJoinEvent.class;
    }
}
