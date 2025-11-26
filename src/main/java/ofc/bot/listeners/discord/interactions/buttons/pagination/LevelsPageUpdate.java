package ofc.bot.listeners.discord.interactions.buttons.pagination;

import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageEmbed;
import ofc.bot.commands.slash.levels.LevelsCommand;
import ofc.bot.domain.sqlite.repository.UserXPRepository;
import ofc.bot.domain.viewmodels.LevelView;
import ofc.bot.handlers.interactions.AutoResponseType;
import ofc.bot.handlers.interactions.EntityContextFactory;
import ofc.bot.handlers.interactions.InteractionListener;
import ofc.bot.handlers.interactions.buttons.contexts.ButtonClickContext;
import ofc.bot.handlers.interactions.commands.responses.states.InteractionResult;
import ofc.bot.handlers.interactions.commands.responses.states.Status;
import ofc.bot.handlers.paginations.PageItem;
import ofc.bot.handlers.paginations.Paginator;
import ofc.bot.util.Scopes;
import ofc.bot.util.content.annotations.listeners.InteractionHandler;
import ofc.bot.util.embeds.EmbedFactory;

import java.util.List;

@InteractionHandler(scope = Scopes.Misc.PAGINATE_LEVELS, autoResponseType = AutoResponseType.DEFER_EDIT)
public class LevelsPageUpdate implements InteractionListener<ButtonClickContext> {
    private final UserXPRepository xpRepo;

    public LevelsPageUpdate(UserXPRepository xpRepo) {
        this.xpRepo = xpRepo;
    }

    @Override
    public InteractionResult onExecute(ButtonClickContext ctx) {
        int pageIndex = ctx.get("page_index");
        long userId = ctx.get("user_id");
        Guild guild = ctx.getGuild();
        PageItem<LevelView> pageItem = Paginator.viewLevels(LevelsCommand.PAGE_SIZE, pageIndex);

        if (pageItem.isEmpty())
            return Status.LEADERBOARD_IS_EMPTY;

        boolean hasMore = pageItem.hasMore();
        LevelView levelView = xpRepo.viewLevelByUserId(userId);
        List<Button> buttons = EntityContextFactory.createLevelsButtons(userId, pageIndex, hasMore);
        MessageEmbed embed = EmbedFactory.embedLevels(guild, levelView, pageItem);

        return ctx.create()
                .setEmbeds(embed)
                .setActionRows(buttons)
                .edit();
    }
}