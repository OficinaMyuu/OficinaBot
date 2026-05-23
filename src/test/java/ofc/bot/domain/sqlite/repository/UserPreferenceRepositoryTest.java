package ofc.bot.domain.sqlite.repository;

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
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite::memory:")) {
            UserPreferenceRepository repository = createRepository(connection);

            assertTrue(repository.isRankupPingsEnabled(100L));
        }
    }

    @Test
    void shouldInsertRankupPreferenceWithUnknownLocaleFallback() throws Exception {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite::memory:")) {
            UserPreferenceRepository repository = createRepository(connection);
            insertUser(repository.getContext(), 101L);

            repository.setRankupPings(101L, false);

            UserPreference pref = repository.findByUserId(101L);
            assertNotNull(pref);
            assertEquals(DiscordLocale.UNKNOWN.getLocale(), pref.get(USERS_PREFERENCES.LOCALE));
            assertFalse(pref.isRankupPingsEnabled());
            assertEquals(DiscordLocale.UNKNOWN, pref.getLocale());
        }
    }

    @Test
    void shouldPreserveRankupPreferenceWhenLocaleChanges() throws Exception {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite::memory:")) {
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
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite::memory:")) {
            UserPreferenceRepository repository = createRepository(connection);
            insertUser(repository.getContext(), 103L);

            repository.setLocale(103L, DiscordLocale.ENGLISH_US.getLocale());
            repository.setRankupPings(103L, false);

            UserPreference pref = repository.findByUserId(103L);
            assertEquals(DiscordLocale.ENGLISH_US, pref.getLocale());
            assertFalse(pref.isRankupPingsEnabled());
        }
    }

    @Test
    void shouldSupportLegacyNotNullLocaleSchema() throws Exception {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite::memory:")) {
            DSLContext ctx = DSL.using(connection, SQLDialect.SQLITE);
            UsersTable.USERS.getSchema(ctx).execute();
            ctx.createTable(USERS_PREFERENCES)
                    .primaryKey(USERS_PREFERENCES.USER_ID)
                    .column(USERS_PREFERENCES.USER_ID)
                    .column(USERS_PREFERENCES.LOCALE.getName(), USERS_PREFERENCES.LOCALE.getDataType().nullable(false))
                    .column(USERS_PREFERENCES.RANKUP_PINGS_ENABLED)
                    .column(USERS_PREFERENCES.CREATED_AT)
                    .column(USERS_PREFERENCES.UPDATED_AT)
                    .constraint(DSL.foreignKey(USERS_PREFERENCES.USER_ID).references(UsersTable.USERS, UsersTable.USERS.ID))
                    .execute();
            UserPreferenceRepository repository = new UserPreferenceRepository(ctx);
            insertUser(ctx, 104L);

            repository.setRankupPings(104L, false);

            UserPreference pref = repository.findByUserId(104L);
            assertEquals(DiscordLocale.UNKNOWN, pref.getLocale());
            assertFalse(pref.isRankupPingsEnabled());
        }
    }

    private UserPreferenceRepository createRepository(Connection connection) {
        DSLContext ctx = DSL.using(connection, SQLDialect.SQLITE);
        UsersTable.USERS.getSchema(ctx).execute();
        USERS_PREFERENCES.getSchema(ctx).execute();
        return new UserPreferenceRepository(ctx);
    }

    private void insertUser(DSLContext ctx, long userId) {
        new UserRepository(ctx).upsert(new AppUser(userId, "user" + userId, null, 1L, 1L));
    }
}
