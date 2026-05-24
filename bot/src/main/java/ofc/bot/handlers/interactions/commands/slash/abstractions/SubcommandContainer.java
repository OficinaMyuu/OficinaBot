package ofc.bot.handlers.interactions.commands.slash.abstractions;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface SubcommandContainer {

    @NotNull
    SubcommandContainer addSubcommand(@NotNull SlashSubcommand cmd);

    @NotNull
    List<SlashSubcommand> getSubcommands();
}