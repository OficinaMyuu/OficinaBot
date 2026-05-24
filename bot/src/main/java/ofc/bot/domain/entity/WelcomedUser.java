package ofc.bot.domain.entity;

import ofc.bot.domain.tables.WelcomedUsersTable;

public class WelcomedUser extends OficinaRecord<WelcomedUser> {
    private static final WelcomedUsersTable WELCOMED_USERS = WelcomedUsersTable.WELCOMED_USERS;

    public WelcomedUser() {
        super(WELCOMED_USERS);
    }

    public WelcomedUser(long guildId, long moderatorId, long targetId, String comment, long timeCreated) {
        this();
        set(WELCOMED_USERS.GUILD_ID, guildId);
        set(WELCOMED_USERS.MODERATOR_ID, moderatorId);
        set(WELCOMED_USERS.TARGET_ID, targetId);
        set(WELCOMED_USERS.COMMENT, comment);
        set(WELCOMED_USERS.CREATED_AT, timeCreated);
    }

    public int getId() {
        return get(WELCOMED_USERS.ID);
    }

    public long getGuildId() {
        return get(WELCOMED_USERS.GUILD_ID);
    }

    public long getModeratorId() {
        return get(WELCOMED_USERS.MODERATOR_ID);
    }

    public long getTargetId() {
        return get(WELCOMED_USERS.TARGET_ID);
    }

    public String getComment() {
        return get(WELCOMED_USERS.COMMENT);
    }

    public long getTimeCreated() {
        return get(WELCOMED_USERS.CREATED_AT);
    }
}