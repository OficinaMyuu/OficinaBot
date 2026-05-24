package ofc.bot.handlers.interactions.commands.slash.abstractions;

import net.dv8tion.jda.api.Permission;
import ofc.bot.handlers.interactions.commands.contexts.impl.SlashCommandContext;
import ofc.bot.util.Bot;
import ofc.bot.util.content.annotations.commands.DiscordCommand;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public abstract class SlashSubcommand implements ICommand<SlashCommandContext> {
    private final String name;

    public SlashSubcommand() {
        this.name = Bot.getSafeAnnotationValue(this, DiscordCommand.class, DiscordCommand::name);
    }

    @Override
    @NotNull
    public final String getName() {
        return this.name;
    }

    public final String getSimpleName() {
        String[] tokens = this.name.split(" ");
        return tokens[tokens.length - 1];
    }

    @Override
    @NotNull
    public final List<Permission> getPermissions() {
        return List.of(); // Subcommands cannot have custom permissions
    }
}