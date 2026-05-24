package ofc.bot.domain.entity;

import ofc.bot.domain.tables.AppUsersBanTable;
import ofc.bot.util.Bot;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public class AppUserBan extends OficinaRecord<AppUserBan> {
    private static final AppUsersBanTable APP_USERS_BAN = AppUsersBanTable.APP_USERS_BAN;

    public AppUserBan() {
        super(APP_USERS_BAN);
    }

    public AppUserBan(long userId, @NotNull String reason, long expiresAt, long bannedAt) {
        this();
        checkReason(reason);
        set(APP_USERS_BAN.USER_ID, userId);
        set(APP_USERS_BAN.REASON, reason);
        set(APP_USERS_BAN.EXPIRES_AT, expiresAt);
        set(APP_USERS_BAN.BANNED_AT, bannedAt);
    }

    public AppUserBan(long userId, @NotNull String reason, long expiresAt) {
        this(userId, reason, expiresAt, Bot.unixNow());
    }

    public int getId() {
        return get(APP_USERS_BAN.ID);
    }

    public long getUserId() {
        return get(APP_USERS_BAN.USER_ID);
    }

    public String getReason() {
        return get(APP_USERS_BAN.REASON);
    }

    public long getExpiration() {
        return get(APP_USERS_BAN.EXPIRES_AT);
    }

    public long getTimeBanned() {
        return get(APP_USERS_BAN.BANNED_AT);
    }

    @Contract("null, -> fail")
    private static void checkReason(String reason) {
        if (reason == null || reason.isBlank())
            throw new IllegalArgumentException("Reason cannot be null or blank");
    }
}
