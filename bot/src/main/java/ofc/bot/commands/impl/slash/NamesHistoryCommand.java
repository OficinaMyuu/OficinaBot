package ofc.bot.commands.impl.slash;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import ofc.bot.domain.entity.enums.NameScope;
import ofc.bot.domain.sqlite.repository.UserNameUpdateRepository;
import ofc.bot.domain.viewmodels.NamesHistoryView;
import ofc.bot.handlers.interactions.EntityContextFactory;
import ofc.bot.handlers.interactions.commands.contexts.impl.SlashCommandContext;
import ofc.bot.handlers.interactions.commands.responses.states.InteractionResult;
import ofc.bot.handlers.interactions.commands.responses.states.Status;
import ofc.bot.handlers.interactions.commands.slash.abstractions.SlashCommand;
import ofc.bot.util.content.annotations.commands.DiscordCommand;
import ofc.bot.util.embeds.EmbedFactory;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.stream.Stream;

/**
 * Pagination handled at
 * {@link ofc.bot.listeners.discord.interactions.buttons.pagination.NamesPageUpdate NamesPageUpdate}.
 */
@DiscordCommand(name = "names", permissions = Permission.MANAGE_SERVER)
public class NamesHistoryCommand extends SlashCommand {
    private final UserNameUpdateRepository namesRepo;

    public NamesHistoryCommand(UserNameUpdateRepository namesRepo) {
        this.namesRepo = namesRepo;
    }

    @Override
    public InteractionResult onCommand(@NotNull SlashCommandContext ctx) {
        User sender = ctx.getUser();
        NameScope scope = ctx.getSafeEnumOption("type", NameScope.class);
        User target = ctx.getOption("user", sender, OptionMapping::getAsUser);
        Guild guild = ctx.getGuild();
        long targetId = target.getIdLong();
        NamesHistoryView names = namesRepo.viewByUserId(scope, targetId, 0, 10);

        if (names.isEmpty())
            return Status.NO_NAME_HISTORY_FOR_USER.args(target.getName());

        boolean hasMorePages = names.page() < names.maxPages();
        List<Button> buttons = EntityContextFactory.createNamesHistoryButtons(scope, targetId, 0, hasMorePages);
        MessageEmbed embed = EmbedFactory.embedUsernameUpdates(names, guild, target);

        return ctx.create()
                .setEmbeds(embed)
                .setActionRows(buttons)
                .send();
    }

    @NotNull
    @Override
    public String getDescription() {
        return "Veja o histórico de apelidos de um usuário.";
    }

    @NotNull
    @Override
    public List<OptionData> getOptions() {
        return List.of(
                new OptionData(OptionType.USER, "user", "O usuário a procurar pelo histórico de apelidos.", true),
                new OptionData(OptionType.STRING, "type", "O tipo de nome a ser recuperado.", true)
                        .addChoices(getTypeChoices())
        );
    }

    private List<Command.Choice> getTypeChoices() {
        return Stream.of(NameScope.values())
                .map(m -> new Command.Choice(m.getDisplayName(), m.name()))
                .toList();
    }
}