package ofc.bot.domain.sqlite;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import ofc.bot.domain.abstractions.InitializableTable;
import ofc.bot.domain.tables.*;
import ofc.bot.internal.data.BotFiles;
import ofc.bot.listeners.console.QueryCounter;
import org.jooq.Configuration;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.SQLDialect;
import org.jooq.exception.DataAccessException;
import org.jooq.impl.DSL;
import org.jooq.impl.DefaultConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.List;

public final class DB {
    private static final Logger LOGGER = LoggerFactory.getLogger(DB.class);
    private static HikariDataSource dataSource;
    
    public static DSLContext getContext() {
        Configuration config = new DefaultConfiguration()
                .set(dataSource)
                .set(SQLDialect.SQLITE)
                .set(new QueryCounter());
        return DSL.using(config);
    }

    public static void init() throws DataAccessException {
        initConfigs();
        DSLContext ctx = getContext();
        List<InitializableTable<?>> tables = getTables();

        for (InitializableTable<?> table : tables) {
            table.getSchema(ctx).execute();
            LOGGER.info("Successfully created table \"{}\"", table.getName());
        }
        runMigrations(ctx);
        LOGGER.info("Successfully created all tables");
    }

    private static List<InitializableTable<?>> getTables() {
        return List.of(
                AppUsersBanTable.APP_USERS_BAN,
                AutomodActionsTable.AUTOMOD_ACTIONS,
                BetGamesTable.BET_GAMES,
                BirthdaysTable.BIRTHDAYS,
                BlockedWordsTable.BLOCKED_WORDS,
                ColorRoleItemsTable.COLOR_ROLE_ITEMS,
                ColorRolesStateTable.COLOR_ROLES_STATES,
                CommandsHistoryTable.COMMANDS_HISTORY,
                CustomUserinfoTable.CUSTOM_USERINFO,
                EntitiesPoliciesTable.ENTITIES_POLICIES,
                FormerMembersRolesTable.FORMER_MEMBERS_ROLES,
                GameMafiaLogsTable.GAME_MAFIA_LOGS,
                GamesParticipantsTable.GAMES_PARTICIPANTS,
                GroupBotsTable.GROUP_BOTS,
                GroupsPerksTable.GROUPS_PERKS,
                LevelsRolesTable.LEVELS_ROLES,
                MarriageRequestsTable.MARRIAGE_REQUESTS,
                MarriagesTable.MARRIAGES,
                MembersEmojisTable.MEMBERS_EMOJIS,
                MembersPunishmentsTable.MEMBERS_PUNISHMENTS,
                MentionsLogTable.MENTIONS_LOG,
                MessagesTranscriptionsTable.MESSAGES_TRANSCRIPTIONS,
                MessagesVersionsTable.MESSAGE_VERSIONS,
                NicknameUpdateRequestsTable.NICKNAME_UPDATE_REQUESTS,
                OficinaGroupsTable.OFICINA_GROUPS,
                RegistersTable.REGISTERS,
                RemindersTable.REMINDERS,
                SupportTicketsTable.SUPPORT_TICKETS,
                TempBansTable.TEMP_BANS,
                UserNamesUpdatesTable.USERNAMES_UPDATES,
                UsersEconomyTable.USERS_ECONOMY,
                UsersEmojisPermissionsTable.USERS_EMOJIS_PERMS,
                UsersPreferencesTable.USERS_PREFERENCES,
                UsersTable.USERS,
                UsersXPTable.USERS_XP,
                VoiceHeartbeatsTable.VOICE_HEARTBEATS,
                WelcomedUsersTable.WELCOMED_USERS
        );
    }

    private static void runMigrations(DSLContext ctx) {
        ensureColorRoleExpiresAt(ctx);
    }

    private static void ensureColorRoleExpiresAt(DSLContext ctx) {
        String table = ColorRolesStateTable.COLOR_ROLES_STATES.getName();
        boolean hasColumn = ctx.fetch("PRAGMA table_info(" + table + ")")
                .stream()
                .map(Record::intoMap)
                .anyMatch(row -> "expires_at".equals(row.get("name")));

        if (hasColumn) {
            return;
        }

        ctx.query("ALTER TABLE " + table + " ADD COLUMN expires_at BIGINT")
                .execute();

        long defaultDurationSeconds = ofc.bot.domain.entity.ColorRoleState.DEFAULT_DURATION_SECONDS;
        ctx.update(ColorRolesStateTable.COLOR_ROLES_STATES)
                .set(
                        ColorRolesStateTable.COLOR_ROLES_STATES.EXPIRES_AT,
                        ColorRolesStateTable.COLOR_ROLES_STATES.UPDATED_AT.plus(defaultDurationSeconds)
                )
                .execute();

        LOGGER.info("Migrated color role states with expires_at");
    }

    private static void initConfigs() {
        File dbFile = BotFiles.DATABASE;
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:sqlite:" + dbFile + "?busy_timeout=5000");

        // SQLite does not benefit from multiple connections; instead, they can
        // cause lock contention and SQLITE_BUSY errors.
        config.setMaximumPoolSize(1);
        config.setMinimumIdle(1);

        dataSource = new HikariDataSource(config);
        LOGGER.info("Created datasource for database at {}", dbFile.getAbsolutePath());
    }
}
