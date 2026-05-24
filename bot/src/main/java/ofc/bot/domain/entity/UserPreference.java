package ofc.bot.domain.entity;

import net.dv8tion.jda.api.interactions.DiscordLocale;
import ofc.bot.domain.tables.UsersPreferencesTable;
import ofc.bot.util.Bot;
import org.jetbrains.annotations.NotNull;

public class UserPreference extends OficinaRecord<UserPreference> {
    private static final UsersPreferencesTable USERS_PREFERENCES = UsersPreferencesTable.USERS_PREFERENCES;

    public UserPreference() {
        super(USERS_PREFERENCES);
    }

    public UserPreference(long userId, String locale, boolean rankupPingsEnabled, long createdAt, long updatedAt) {
        this();
        set(USERS_PREFERENCES.USER_ID, userId);
        set(USERS_PREFERENCES.LOCALE, locale);
        set(USERS_PREFERENCES.RANKUP_PINGS_ENABLED, rankupPingsEnabled);
        set(USERS_PREFERENCES.CREATED_AT, createdAt);
        set(USERS_PREFERENCES.UPDATED_AT, updatedAt);
    }

    public static UserPreference fromUserPreference(long userId, String locale) {
        long now = Bot.unixNow();
        return new UserPreference(userId, locale, true, now, now);
    }

    public long getUserId() {
        return get(USERS_PREFERENCES.USER_ID);
    }

    public DiscordLocale getLocale() {
        String locale = get(USERS_PREFERENCES.LOCALE);
        return locale == null ? DiscordLocale.UNKNOWN : DiscordLocale.from(locale);
    }

    public boolean isRankupPingsEnabled() {
        Boolean enabled = get(USERS_PREFERENCES.RANKUP_PINGS_ENABLED);
        return enabled == null || enabled;
    }

    public long getTimeCreated() {
        return get(USERS_PREFERENCES.CREATED_AT);
    }

    public long getLastUpdated() {
        return get(USERS_PREFERENCES.UPDATED_AT);
    }

    public UserPreference setUserId(long userId) {
        set(USERS_PREFERENCES.USER_ID, userId);
        return this;
    }

    public UserPreference setLocale(String locale) {
        set(USERS_PREFERENCES.LOCALE, locale);
        return this;
    }

    public UserPreference setRankupPingsEnabled(boolean enabled) {
        set(USERS_PREFERENCES.RANKUP_PINGS_ENABLED, enabled);
        return this;
    }

    @NotNull
    public UserPreference setLastUpdated(long updatedAt) {
        set(USERS_PREFERENCES.UPDATED_AT, updatedAt);
        return this;
    }
}
