package ofc.bot.domain.sqlite.repository;

import ofc.bot.domain.abstractions.InitializableTable;
import ofc.bot.domain.entity.MemberJoinEvent;
import ofc.bot.domain.tables.MemberJoinEventsTable;
import org.jetbrains.annotations.NotNull;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;

public class MemberJoinEventRepository extends Repository<MemberJoinEvent> {
    private static final MemberJoinEventsTable MEMBER_JOIN_EVENTS = MemberJoinEventsTable.MEMBER_JOIN_EVENTS;

    public MemberJoinEventRepository(@NotNull DSLContext ctx) {
        super(ctx);
    }

    @NotNull
    @Override
    public InitializableTable<MemberJoinEvent> getTable() {
        return MEMBER_JOIN_EVENTS;
    }

    public Long findEarliestCreatedAtByGuildAndUserId(long guildId, long userId) {
        return ctx.select(DSL.min(MEMBER_JOIN_EVENTS.CREATED_AT))
                .from(MEMBER_JOIN_EVENTS)
                .where(MEMBER_JOIN_EVENTS.GUILD_ID.eq(guildId))
                .and(MEMBER_JOIN_EVENTS.USER_ID.eq(userId))
                .fetchOne(0, Long.class);
    }
}
