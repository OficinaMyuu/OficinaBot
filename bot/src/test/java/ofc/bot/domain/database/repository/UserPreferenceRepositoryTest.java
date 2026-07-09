package ofc.bot.domain.database.repository;

import ofc.bot.testing.MySQLTestDatabase;

import net.dv8tion.jda.api.interactions.DiscordLocale;
import ofc.bot.domain.entity.AppUser;
import ofc.bot.domain.entity.UserPreference;
import ofc.bot.domain.tables.UsersPreferencesTable;
import ofc.bot.domain.tables.UsersTable;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;

import static org.junit.jupiter.api.Assertions.*;

class UserPreferenceRepositoryTest {
    private static final UsersPreferencesTable USERS_PREFERENCES = UsersPreferencesTable.USERS_PREFERENCES;

    @Test
    void shouldDefaultMissingRankupPingsToEnabled() throws Exception {
        try (Connection connection = MySQLTestDatabase.open()) {
            UserPreferenceRepository repository = createRepository(connection);

            assertTrue(repository.isRankupPingsEnabled(100L));
        }
    }

    @Test
    void shouldInsertRankupPreferenceWithoutLocale() throws Exception {
        try (Connection connection = MySQLTestDatabase.open()) {
            UserPreferenceRepository repository = createRepository(connection);
            insertUser(repository.getContext(), 101L);

            repository.setRankupPings(101L, false);

            UserPreference pref = repository.findByUserId(101L);
            assertNotNull(pref);
            assertNull(pref.get(USERS_PREFERENCES.LOCALE));
            assertFalse(pref.isRankupPingsEnabled());
            assertEquals(DiscordLocale.UNKNOWN, pref.getLocale());
        }
    }

    @Test
    void shouldPreserveRankupPreferenceWhenLocaleChanges() throws Exception {
        try (Connection connection = MySQLTestDatabase.open()) {
            UserPreferenceRepository repository = createRepository(connection);
            insertUser(repository.getContext(), 102L);

            repository.setRankupPings(102L, false);
            repository.setLocale(102L, DiscordLocale.PORTUGUESE_BRAZILIAN.getLocale());

            UserPreference pref = repository.findByUserId(102L);
            assertEquals(DiscordLocale.PORTUGUESE_BRAZILIAN, pref.getLocale());
            assertFalse(pref.isRankupPingsEnabled());
        }
    }

    @Test
    void shouldPreserveLocaleWhenRankupPreferenceChanges() throws Exception {
        try (Connection connection = MySQLTestDatabase.open()) {
            UserPreferenceRepository repository = createRepository(connection);
            insertUser(repository.getContext(), 103L);

            repository.setLocale(103L, DiscordLocale.ENGLISH_US.getLocale());
            repository.setRankupPings(103L, false);

            UserPreference pref = repository.findByUserId(103L);
            assertEquals(DiscordLocale.ENGLISH_US, pref.getLocale());
            assertFalse(pref.isRankupPingsEnabled());
        }
    }

    private UserPreferenceRepository createRepository(Connection connection) {
        DSLContext ctx = MySQLTestDatabase.context(connection);
        UsersTable.USERS.getSchema(ctx).execute();
        USERS_PREFERENCES.getSchema(ctx).execute();
        return new UserPreferenceRepository(ctx);
    }

    private void insertUser(DSLContext ctx, long userId) {
        new UserRepository(ctx).upsert(new AppUser(userId, "user" + userId, null, null, 1L, 1L));
    }
}
