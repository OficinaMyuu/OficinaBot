package ofc.bot.handlers.interactions.commands.slash.abstractions;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.build.*;
import ofc.bot.handlers.interactions.commands.Cooldown;
import ofc.bot.handlers.interactions.commands.responses.states.InteractionResult;
import ofc.bot.handlers.interactions.commands.slash.SubcommandGroup;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface ICommand<C> {

    InteractionResult onCommand(@NotNull C ctx);

    @NotNull
    String getName();

    @NotNull
    String getDescription();

    @NotNull
    List<Permission> getPermissions();

    @NotNull
    default List<OptionData> getOptions() {
        return List.of();
    }

    @NotNull
    default Cooldown getCooldown() {
        return Cooldown.ZERO;
    }

    static SlashCommandData buildSlash(SlashCommand cmd) {
        try {
            List<SubcommandData> subcommands = cmd.getSubcommands().stream().map(ICommand::buildSubcommand).toList();
            List<SubcommandGroupData> subgroups = cmd.getGroups().stream().map(ICommand::buildGroup).toList();

            return Commands.slash(cmd.getName(), cmd.getDescription())
                    .addOptions(cmd.getOptions())
                    .addSubcommands(subcommands)
                    .addSubcommandGroups(subgroups)
                    .setDefaultPermissions(DefaultMemberPermissions.enabledFor(cmd.getPermissions()));
        } catch (Exception e) {
            throw new RuntimeException("Failed to build slash command \"" + cmd.getName() + "\"", e);
        }
    }

    private static SubcommandData buildSubcommand(SlashSubcommand sub) {
        return new SubcommandData(sub.getSimpleName(), sub.getDescription())
                .addOptions(sub.getOptions());
    }

    private static SubcommandGroupData buildGroup(SubcommandGroup group) {
        List<SubcommandData> subs = group.getSubcommands().stream().map(ICommand::buildSubcommand).toList();

        return new SubcommandGroupData(group.getName(), group.getDescription())
                .addSubcommands(subs);
    }
}