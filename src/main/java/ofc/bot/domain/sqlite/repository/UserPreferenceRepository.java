package ofc.bot.domain.sqlite.repository;

import net.dv8tion.jda.api.interactions.DiscordLocale;
import ofc.bot.domain.abstractions.InitializableTable;
import ofc.bot.domain.entity.UserPreference;
import ofc.bot.domain.tables.UsersPreferencesTable;
import ofc.bot.util.Bot;
import org.jetbrains.annotations.NotNull;
import org.jooq.DSLContext;

/**
 * Repository for {@link UserPreference} entity.
 */
public class UserPreferenceRepository extends Repository<UserPreference> {
    private static final UsersPreferencesTable USERS_PREFERENCES = UsersPreferencesTable.USERS_PREFERENCES;

    public UserPreferenceRepository(DSLContext ctx) {
        super(ctx);
    }

    @NotNull
    @Override
    public InitializableTable<UserPreference> getTable() {
        return USERS_PREFERENCES;
    }

    public UserPreference findByUserId(long userId, UserPreference fallback) {
        UserPreference pref = findByUserId(userId);
        return pref == null ? fallback : pref;
    }

    public UserPreference findByUserId(long userId) {
        return ctx.selectFrom(USERS_PREFERENCES)
                .where(USERS_PREFERENCES.USER_ID.eq(userId))
                .fetchOne();
    }

    public boolean isRankupPingsEnabled(long userId) {
        UserPreference pref = findByUserId(userId);
        return pref == null || pref.isRankupPingsEnabled();
    }

    public void setLocale(long userId, String locale) {
        long now = Bot.unixNow();

        ctx.insertInto(USERS_PREFERENCES)
                .set(USERS_PREFERENCES.USER_ID, userId)
                .set(USERS_PREFERENCES.LOCALE, locale)
                .set(USERS_PREFERENCES.RANKUP_PINGS_ENABLED, true)
                .set(USERS_PREFERENCES.CREATED_AT, now)
                .set(USERS_PREFERENCES.UPDATED_AT, now)
                .onDuplicateKeyUpdate()
                .set(USERS_PREFERENCES.LOCALE, locale)
                .set(USERS_PREFERENCES.UPDATED_AT, now)
                .execute();
    }

    public void setRankupPings(long userId, boolean enabled) {
        long now = Bot.unixNow();

        ctx.insertInto(USERS_PREFERENCES)
                .set(USERS_PREFERENCES.USER_ID, userId)
                .set(USERS_PREFERENCES.LOCALE, DiscordLocale.UNKNOWN.getLocale())
                .set(USERS_PREFERENCES.RANKUP_PINGS_ENABLED, enabled)
                .set(USERS_PREFERENCES.CREATED_AT, now)
                .set(USERS_PREFERENCES.UPDATED_AT, now)
                .onDuplicateKeyUpdate()
                .set(USERS_PREFERENCES.RANKUP_PINGS_ENABLED, enabled)
                .set(USERS_PREFERENCES.UPDATED_AT, now)
                .execute();
    }
}
