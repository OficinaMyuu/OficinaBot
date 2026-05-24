package ofc.bot.domain.sqlite.repository;

import ofc.bot.domain.abstractions.InitializableTable;
import ofc.bot.domain.entity.CommandHistory;
import ofc.bot.domain.tables.CommandsHistoryTable;
import org.jetbrains.annotations.NotNull;
import org.jooq.DSLContext;

/**
 * Repository for {@link CommandHistory} entity.
 */
public class CommandHistoryRepository extends Repository<CommandHistory> {
    private static final CommandsHistoryTable COMMANDS_HISTORY = CommandsHistoryTable.COMMANDS_HISTORY;

    public CommandHistoryRepository(DSLContext ctx) {
        super(ctx);
    }

    /**
     * Gets the timestamp of the last time the user has run a given command.
     * <p>
     * You can call {@link java.time.Instant#ofEpochSecond(long) Instant.ofEpochSecond(long)} on it,
     * if you need an Instant object out this value.
     *
     * @param userId The ID of the user to be checked.
     * @param cmdName The name of the command to be checked.
     * @return The timestamp of the last commnad execution by this user,
     *         or {@code 0} if nothing was found.
     */
    public long getLastCall(long userId, String cmdName) {
        return ctx.select(COMMANDS_HISTORY.CREATED_AT)
                .from(COMMANDS_HISTORY)
                .where(COMMANDS_HISTORY.USER_ID.eq(userId))
                .and(COMMANDS_HISTORY.COMMAND_NAME.eq(cmdName))
                .and(COMMANDS_HISTORY.TICKS_COOLDOWN.eq(true))
                .orderBy(COMMANDS_HISTORY.CREATED_AT.desc())
                .limit(1)
                .fetchOptionalInto(long.class)
                .orElse(0L);
    }

    public long getLastGlobalCall(String cmdName) {
        return ctx.select(COMMANDS_HISTORY.CREATED_AT)
                .from(COMMANDS_HISTORY)
                .where(COMMANDS_HISTORY.COMMAND_NAME.eq(cmdName))
                .and(COMMANDS_HISTORY.TICKS_COOLDOWN.eq(true))
                .orderBy(COMMANDS_HISTORY.CREATED_AT.desc())
                .limit(1)
                .fetchOptionalInto(long.class)
                .orElse(0L);
    }

    @NotNull
    @Override
    public InitializableTable<CommandHistory> getTable() {
        return COMMANDS_HISTORY;
    }
}