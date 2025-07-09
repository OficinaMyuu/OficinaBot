package ofc.bot.domain.sqlite;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import ofc.bot.domain.abstractions.InitializableTable;
import ofc.bot.domain.tables.*;
import ofc.bot.internal.data.BotFiles;
import ofc.bot.listeners.console.QueryCounter;
import org.jooq.Configuration;
import org.jooq.DSLContext;
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
        LOGGER.info("Successfully created all tables");
    }

    private static List<InitializableTable<?>> getTables() {
        return List.of(
                AppUsersBanTable.APP_USERS_BAN,
                AutomodActionsTable.AUTOMOD_ACTIONS,
                BankTransactionsTable.BANK_TRANSACTIONS,
                BetGamesTable.BET_GAMES,
                BirthdaysTable.BIRTHDAYS,
                BlockedWordsTable.BLOCKED_WORDS,
                ColorRolesStateTable.COLOR_ROLES_STATES,
                CommandsHistoryTable.COMMANDS_HISTORY,
                CustomUserinfoTable.CUSTOM_USERINFO,
                DiscordMessagesTable.DISCORD_MESSAGES,
                DiscordMessageUpdatesTable.DISCORD_MESSAGE_UPDATES,
                EntitiesPoliciesTable.ENTITIES_POLICIES,
                FormerMembersRolesTable.FORMER_MEMBERS_ROLES,
                GamesParticipantsTable.GAMES_PARTICIPANTS,
                GroupBotsTable.GROUP_BOTS,
                GroupsPerksTable.GROUPS_PERKS,
                LevelsRolesTable.LEVELS_ROLES,
                MarriageRequestsTable.MARRIAGE_REQUESTS,
                MarriagesTable.MARRIAGES,
                MembersEmojisTable.MEMBERS_EMOJIS,
                MembersPunishmentsTable.MEMBERS_PUNISHMENTS,
                MessagesTranscriptionsTable.MESSAGES_TRANSCRIPTIONS,
                OficinaGroupsTable.OFICINA_GROUPS,
                RemindersTable.REMINDERS,
                SupportTicketsTable.SUPPORT_TICKETS,
                TempBansTable.TEMP_BANS,
                TwitchSubscriptionsTable.TWITCH_SUBSCRIPTIONS,
                UserNamesUpdatesTable.USERNAMES_UPDATES,
                UsersEconomyTable.USERS_ECONOMY,
                UsersPreferencesTable.USERS_PREFERENCES,
                UsersTable.USERS,
                UsersSubscriptionsTable.USERS_SUBSCRIPTIONS,
                UsersXPTable.USERS_XP
        );
    }

    private static void initConfigs() {
        File dbFile = BotFiles.DATABASE;
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:sqlite:" + dbFile);

        dataSource = new HikariDataSource(config);
        LOGGER.info("Created datasource for database at {}", dbFile.getAbsolutePath());
    }
}