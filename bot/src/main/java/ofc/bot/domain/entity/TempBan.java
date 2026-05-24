package ofc.bot.domain.entity;

import ofc.bot.domain.tables.TempBansTable;
import ofc.bot.util.Bot;

public class TempBan extends OficinaRecord<TempBan> {
    private static final TempBansTable TEMP_BANS = TempBansTable.TEMP_BANS;

    public TempBan() {
        super(TEMP_BANS);
    }

    public TempBan(long userId, long guildId, long expiresAt, long createdAt) {
        this();
        set(TEMP_BANS.USER_ID, userId);
        set(TEMP_BANS.GUILD_ID, guildId);
        set(TEMP_BANS.EXPIRES_AT, expiresAt);
        set(TEMP_BANS.CREATED_AT, createdAt);
    }

    public TempBan(long userId, long guildId, long expiresAt) {
        this(userId, guildId, expiresAt, Bot.unixNow());
    }

    public int getId() {
        return get(TEMP_BANS.ID);
    }

    public long getUserId() {
        return get(TEMP_BANS.USER_ID);
    }

    public long getGuildId() {
        return get(TEMP_BANS.GUILD_ID);
    }

    public long getExpiration() {
        return get(TEMP_BANS.EXPIRES_AT);
    }

    public long getTimeCreated() {
        return get(TEMP_BANS.CREATED_AT);
    }
}