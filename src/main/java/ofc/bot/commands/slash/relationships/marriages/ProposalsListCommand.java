package ofc.bot.commands.slash.relationships.marriages;

import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import ofc.bot.domain.sqlite.repository.MarriageRequestRepository;
import ofc.bot.domain.viewmodels.ProposalsView;
import ofc.bot.handlers.interactions.EntityContextFactory;
import ofc.bot.handlers.interactions.commands.contexts.impl.SlashCommandContext;
import ofc.bot.handlers.interactions.commands.responses.states.InteractionResult;
import ofc.bot.handlers.interactions.commands.responses.states.Status;
import ofc.bot.handlers.interactions.commands.slash.abstractions.SlashSubcommand;
import ofc.bot.util.content.annotations.commands.DiscordCommand;
import ofc.bot.util.embeds.EmbedFactory;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@DiscordCommand(name = "marriage proposals")
public class ProposalsListCommand extends SlashSubcommand {
    private final MarriageRequestRepository mreqRepo;

    public ProposalsListCommand(MarriageRequestRepository mreqRepo) {
        this.mreqRepo = mreqRepo;
    }

    @Override
    public InteractionResult onCommand(@NotNull SlashCommandContext ctx) {
        User user = ctx.getUser();
        long senderId = user.getIdLong();
        Guild guild = ctx.getGuild();
        String type = ctx.getSafeOption("type", OptionMapping::getAsString);
        ProposalsView proposals = mreqRepo.viewProposals(type, senderId, 0);

        if (proposals.isEmpty())
            return Status.MARRIAGE_PROPOSAL_LIST_IS_EMPTY;

        boolean hasMorePages = 1 < proposals.maxPages();
        List<Button> buttons = EntityContextFactory.createProposalsListButtons(1, senderId, hasMorePages, type);
        MessageEmbed embed = EmbedFactory.embedProposals(guild, user, proposals);

        return ctx.create()
                .setEmbeds(embed)
                .setActionRows(buttons)
                .send();
    }

    @NotNull
    @Override
    public String getDescription() {
        return "Veja as propostas de casamento pendentes.";
    }

    @NotNull
    @Override
    public List<OptionData> getOptions() {
        return List.of(
                new OptionData(OptionType.STRING, "type", "O tipo de proposta a ser listado.", true)
                        .addChoice("Outgoing", "out")
                        .addChoice("Incoming", "in")
        );
    }
}