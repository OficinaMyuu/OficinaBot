package ofc.bot.domain.entity;

import net.dv8tion.jda.internal.utils.Checks;
import ofc.bot.domain.tables.CommandsHistoryTable;
import ofc.bot.handlers.interactions.commands.responses.states.Status;
import ofc.bot.util.Bot;
import org.jetbrains.annotations.NotNull;

public class CommandHistory extends OficinaRecord<CommandHistory> {
    private static final CommandsHistoryTable COMMANDS_HISTORY = CommandsHistoryTable.COMMANDS_HISTORY;

    public CommandHistory() {
        super(COMMANDS_HISTORY);
    }

    public CommandHistory(@NotNull String cmdName, @NotNull Status status, boolean ticksCooldown,
                          long guildId, long userId, long createdAt) {
        this();
        Checks.notNull(cmdName, "Command Name");
        Checks.notNull(status, "Status");
        set(COMMANDS_HISTORY.COMMAND_NAME, cmdName);
        set(COMMANDS_HISTORY.EXIT_STATUS, status.name());
        set(COMMANDS_HISTORY.TICKS_COOLDOWN, ticksCooldown);
        set(COMMANDS_HISTORY.GUILD_ID, guildId);
        set(COMMANDS_HISTORY.USER_ID, userId);
        set(COMMANDS_HISTORY.CREATED_AT, createdAt);
    }

    public CommandHistory(@NotNull String cmdName, @NotNull Status status, boolean ticksCooldown, long guildId, long userId) {
        this(cmdName, status, ticksCooldown, guildId, userId, Bot.unixNow());
    }

    public int getId() {
        return get(COMMANDS_HISTORY.ID);
    }

    public String getCommandName() {
        return get(COMMANDS_HISTORY.COMMAND_NAME);
    }

    public Status getExitStatus() {
        String status = get(COMMANDS_HISTORY.EXIT_STATUS);
        return Status.valueOf(status);
    }

    public boolean ticksCooldown() {
        return get(COMMANDS_HISTORY.TICKS_COOLDOWN);
    }

    public long getGuildId() {
        return get(COMMANDS_HISTORY.GUILD_ID);
    }

    public long getUserId() {
        return get(COMMANDS_HISTORY.USER_ID);
    }

    public long getTimeCreated() {
        return get(COMMANDS_HISTORY.CREATED_AT);
    }
}