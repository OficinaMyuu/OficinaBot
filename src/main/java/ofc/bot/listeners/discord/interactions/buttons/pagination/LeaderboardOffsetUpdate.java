package ofc.bot.listeners.discord.interactions.buttons.pagination;

import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageEmbed;
import ofc.bot.domain.viewmodels.LeaderboardUser;
import ofc.bot.handlers.interactions.AutoResponseType;
import ofc.bot.handlers.interactions.EntityContextFactory;
import ofc.bot.handlers.interactions.InteractionListener;
import ofc.bot.handlers.interactions.buttons.contexts.ButtonClickContext;
import ofc.bot.handlers.interactions.commands.responses.states.InteractionResult;
import ofc.bot.handlers.paginations.PageItem;
import ofc.bot.handlers.paginations.Paginator;
import ofc.bot.util.Scopes;
import ofc.bot.util.content.annotations.listeners.InteractionHandler;
import ofc.bot.util.embeds.EmbedFactory;

import java.util.List;

@InteractionHandler(scope = Scopes.Economy.VIEW_LEADERBOARD, autoResponseType = AutoResponseType.DEFER_EDIT)
public class LeaderboardOffsetUpdate implements InteractionListener<ButtonClickContext> {

    @Override
    public InteractionResult onExecute(ButtonClickContext ctx) {
        int pageIndex = ctx.get("page_index");
        Paginator<LeaderboardUser> paginator = ctx.get("paginator");
        PageItem<LeaderboardUser> lb = paginator.next(pageIndex);
        Guild guild = ctx.getGuild();
        boolean hasMorePages = lb.hasMore();
        List<Button> newButtons = EntityContextFactory.createLeaderboardButtons(paginator, pageIndex, hasMorePages);
        MessageEmbed newEmbed = EmbedFactory.embedLeaderboard(guild, lb);

        return ctx.create()
                .setEmbeds(newEmbed)
                .setActionRows(newButtons)
                .edit();
    }
}