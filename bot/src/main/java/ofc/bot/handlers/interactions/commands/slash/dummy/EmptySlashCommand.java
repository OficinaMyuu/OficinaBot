package ofc.bot.handlers.interactions.commands.slash.dummy;

import net.dv8tion.jda.api.Permission;
import ofc.bot.handlers.interactions.commands.contexts.impl.SlashCommandContext;
import ofc.bot.handlers.interactions.commands.responses.states.InteractionResult;
import ofc.bot.handlers.interactions.commands.slash.abstractions.SlashCommand;
import org.jetbrains.annotations.NotNull;

/**
 * This class is an empty SlashCommand, you should use it when you need
 * a command with subcommands and subcommand groups.
 * <p>
 * Since base commands cannot be executed when subcomands and groups are present,
 * you will never create a class extending the {@link SlashCommand} just to define
 * your base command since it just worsens class organization.
 * <p>
 * In this case, you instantiate a new {@link EmptySlashCommand} and add
 * all your subcommands and groups to it.
 */
public final class EmptySlashCommand extends SlashCommand {
    private final String description;

    public EmptySlashCommand(String name, String desc, Permission... perms) {
        super(name, perms);
        this.description = desc;
    }

    public EmptySlashCommand(String name, String desc) {
        this(name, desc, new Permission[0]);
    }

    @Override
    public InteractionResult onCommand(@NotNull SlashCommandContext ctx) {
        throw new UnsupportedOperationException("Cannot execute this command");
    }

    @NotNull
    @Override
    public String getDescription() {
        return this.description;
    }
}