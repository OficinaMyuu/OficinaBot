package ofc.bot.handlers.interactions.commands.slash.abstractions;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.internal.utils.Checks;
import ofc.bot.handlers.interactions.commands.contexts.impl.SlashCommandContext;
import ofc.bot.handlers.interactions.commands.slash.SubcommandGroup;
import ofc.bot.util.Bot;
import ofc.bot.util.content.annotations.commands.DiscordCommand;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public abstract class SlashCommand implements SubcommandContainer, ICommand<SlashCommandContext> {
    private final String name;
    private final List<Permission> permissions;
    private final List<SlashSubcommand> subCmds;
    private final List<SubcommandGroup> groups;

    public SlashCommand(String name, Permission... permissions) {
        this.name = name;
        this.permissions = List.of(permissions);
        this.subCmds = new ArrayList<>();
        this.groups = new ArrayList<>();
        checkName();
    }

    public SlashCommand() {
        this.name = Bot.getSafeAnnotationValue(this, DiscordCommand.class, DiscordCommand::name);
        this.permissions = List.of(Bot.getAnnotationValue(this, DiscordCommand.class, DiscordCommand::permissions, new Permission[0]));
        this.groups = new ArrayList<>();
        this.subCmds = new ArrayList<>();
        checkName();
    }

    @NotNull
    @Override
    public final SlashCommand addSubcommand(@NotNull SlashSubcommand sub) {
        Checks.notNull(sub, "Subcommand");
        this.subCmds.add(sub);
        return this;
    }

    @NotNull
    @Override
    public final String getName() {
        return this.name;
    }

    @NotNull
    @Override
    public final List<Permission> getPermissions() {
        return this.permissions;
    }

    @NotNull
    @Override
    public final List<SlashSubcommand> getSubcommands() {
        return this.subCmds;
    }

    public final boolean hasSubcommands() {
        return !subCmds.isEmpty() || !groups.isEmpty();
    }

    public final SlashCommand addGroups(SubcommandGroup... groups) {
        this.groups.addAll(List.of(groups));
        return this;
    }

    public final List<SubcommandGroup> getGroups() {
        return this.groups;
    }

    private void checkName() {
        if (this.name.split(" ").length != 1) {
            throw new IllegalArgumentException("Command names cannot contain spaces, provided: " + this.name);
        }
    }
}