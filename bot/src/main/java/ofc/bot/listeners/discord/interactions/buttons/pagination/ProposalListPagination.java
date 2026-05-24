package ofc.bot.listeners.discord.interactions.buttons.pagination;

import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageEmbed;
import ofc.bot.domain.sqlite.repository.MarriageRequestRepository;
import ofc.bot.domain.viewmodels.ProposalsView;
import ofc.bot.handlers.interactions.EntityContextFactory;
import ofc.bot.handlers.interactions.InteractionListener;
import ofc.bot.handlers.interactions.buttons.contexts.ButtonClickContext;
import ofc.bot.handlers.interactions.commands.responses.states.InteractionResult;
import ofc.bot.handlers.interactions.commands.responses.states.Status;
import ofc.bot.util.Bot;
import ofc.bot.util.Scopes;
import ofc.bot.util.content.annotations.listeners.InteractionHandler;
import ofc.bot.util.embeds.EmbedFactory;

import java.util.List;

@InteractionHandler(scope = Scopes.Misc.PAGINATE_MARRIAGE_REQUESTS)
public class ProposalListPagination implements InteractionListener<ButtonClickContext> {
    private final MarriageRequestRepository mreqRepo;

    public ProposalListPagination(MarriageRequestRepository mreqRepo) {
        this.mreqRepo = mreqRepo;
    }

    @Override
    public InteractionResult onExecute(ButtonClickContext ctx) {
        int page = ctx.get("page");
        long userId = ctx.get("target_id");
        Guild guild = ctx.getGuild();
        String type = ctx.get("type");
        ProposalsView proposals = mreqRepo.viewProposals(type, userId, page - 1);
        boolean hasMorePages = proposals.page() < proposals.maxPages();
        List<Button> buttons = EntityContextFactory.createProposalsListButtons(page, userId, hasMorePages, type);

        Bot.fetchUser(userId).queue((user) -> {
            MessageEmbed embed = EmbedFactory.embedProposals(guild, user, proposals);

            ctx.create()
                    .setEmbeds(embed)
                    .setActionRows(buttons)
                    .edit();
        }, (e) -> ctx.reply(Status.USER_NOT_FOUND));
        return Status.OK;
    }
}
