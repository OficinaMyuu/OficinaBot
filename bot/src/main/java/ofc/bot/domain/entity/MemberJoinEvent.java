package ofc.bot.domain.entity;

import ofc.bot.domain.tables.MemberJoinEventsTable;

public class MemberJoinEvent extends OficinaRecord<MemberJoinEvent> {
    private static final MemberJoinEventsTable MEMBER_JOIN_EVENTS = MemberJoinEventsTable.MEMBER_JOIN_EVENTS;

    public MemberJoinEvent() {
        super(MEMBER_JOIN_EVENTS);
    }

    public MemberJoinEvent(
            long guildId,
            long userId,
            long createdAt
    ) {
        this();
        set(MEMBER_JOIN_EVENTS.GUILD_ID, guildId);
        set(MEMBER_JOIN_EVENTS.USER_ID, userId);
        set(MEMBER_JOIN_EVENTS.CREATED_AT, createdAt);
    }

    public int getId() {
        return get(MEMBER_JOIN_EVENTS.ID);
    }

    public long getGuildId() {
        return get(MEMBER_JOIN_EVENTS.GUILD_ID);
    }

    public long getUserId() {
        return get(MEMBER_JOIN_EVENTS.USER_ID);
    }

    public long getTimeCreated() {
        return get(MEMBER_JOIN_EVENTS.CREATED_AT);
    }
}
