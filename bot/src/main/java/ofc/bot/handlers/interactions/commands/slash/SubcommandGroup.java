package ofc.bot.handlers.interactions.commands.slash;

import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.internal.utils.Checks;
import ofc.bot.handlers.interactions.commands.slash.abstractions.SlashSubcommand;
import ofc.bot.handlers.interactions.commands.slash.abstractions.SubcommandContainer;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class SubcommandGroup implements SubcommandContainer {
    private final String name;
    private final String description;
    private final List<SlashSubcommand> subcommands;

    public SubcommandGroup(@NotNull String name, @NotNull String description) {
        Checks.notNull(name, "Name");
        Checks.notNull(description, "Description");

        this.name = name;
        this.description = description;
        this.subcommands = new ArrayList<>(CommandData.MAX_OPTIONS);
    }

    @NotNull
    public String getName() {
        return this.name;
    }

    @NotNull
    public String getDescription() {
        return this.description;
    }

    @Override
    @NotNull
    public SubcommandGroup addSubcommand(@NotNull SlashSubcommand sub) {
        this.subcommands.add(sub);
        return this;
    }

    @Override
    @NotNull
    public List<SlashSubcommand> getSubcommands() {
        return this.subcommands;
    }
}