package ofc.bot.handlers.interactions.commands.slash;

import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.internal.utils.Checks;
import ofc.bot.handlers.interactions.commands.contexts.impl.SlashCommandContext;
import ofc.bot.handlers.interactions.commands.slash.abstractions.ICommand;
import ofc.bot.handlers.interactions.commands.slash.abstractions.SlashCommand;
import ofc.bot.handlers.interactions.commands.slash.abstractions.SlashSubcommand;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public final class SlashCommandsRegistryManager {
    private static final SlashCommandsRegistryManager INSTANCE = new SlashCommandsRegistryManager();
    private static final Map<String, ICommand<SlashCommandContext>> commands = new HashMap<>();
    private static final List<SlashCommand> temp = new ArrayList<>(Commands.MAX_SLASH_COMMANDS);

    private SlashCommandsRegistryManager() {}

    public static SlashCommandsRegistryManager getManager() {
        return INSTANCE;
    }

    @Nullable
    public ICommand<SlashCommandContext> getCommand(@NotNull String fullName) {
        Checks.notNull(fullName, "Full Command Name");
        return commands.get(fullName);
    }

    @NotNull
    public List<SlashCommand> getAll() {
        return temp;
    }

    public void clearTemp() {
        temp.clear();
    }

    public void register(@NotNull SlashCommand cmd) {
        Checks.notNull(cmd, "Slash Command");
        temp.add(cmd);
        commands.putAll(getExecutables(cmd));
    }

    private Map<String, ICommand<SlashCommandContext>> getExecutables(SlashCommand cmd) {
        Map<String, ICommand<SlashCommandContext>> executables = new HashMap<>();

        // Add only the base command if there are no subcommands
        // If subcommands or groups exist, the base command becomes unexecutable, so we don't add it
        if (!cmd.hasSubcommands()) {
            executables.put(cmd.getName(), cmd);
            return executables;
        }

        for (SlashSubcommand sub : cmd.getSubcommands()) {
            executables.put(sub.getName(), sub);
        }

        for (SubcommandGroup group : cmd.getGroups()) {
            for (SlashSubcommand sub : group.getSubcommands()) {
                executables.put(sub.getName(), sub);
            }
        }
        return executables;
    }
}