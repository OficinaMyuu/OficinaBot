package ofc.bot.domain.sqlite.repository;

import ofc.bot.domain.abstractions.InitializableTable;
import ofc.bot.domain.entity.UserPreference;
import ofc.bot.domain.tables.UsersPreferencesTable;
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
        UserPreference pref = findByUserId(userId, UserPreference.fromUserPreference(userId, locale));
        pref.setLocale(locale).tickUpdate();
        upsert(pref);
    }

    public void setRankupPings(long userId, boolean enabled) {
        UserPreference pref = findByUserId(userId, UserPreference.fromUserPreference(userId, null));
        pref.setRankupPingsEnabled(enabled).tickUpdate();
        upsert(pref);
    }
}
