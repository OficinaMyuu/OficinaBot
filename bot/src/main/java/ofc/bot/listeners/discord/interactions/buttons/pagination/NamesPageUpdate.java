package ofc.bot.listeners.discord.interactions.buttons.pagination;

import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageEmbed;
import ofc.bot.domain.entity.enums.NameScope;
import ofc.bot.domain.sqlite.repository.UserNameUpdateRepository;
import ofc.bot.domain.viewmodels.NamesHistoryView;
import ofc.bot.handlers.interactions.AutoResponseType;
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

@InteractionHandler(scope = Scopes.Misc.PAGINATE_NAME_UPDATE, autoResponseType = AutoResponseType.DEFER_EDIT)
public class NamesPageUpdate implements InteractionListener<ButtonClickContext> {
    private final UserNameUpdateRepository namesRepo;

    public NamesPageUpdate(UserNameUpdateRepository repo) {
        this.namesRepo = repo;
    }

    @Override
    public InteractionResult onExecute(ButtonClickContext ctx) {
        Guild guild = ctx.getGuild();
        NameScope type = ctx.get("type");
        long targetId = ctx.get("target_id");
        int offset = ctx.get("offset");
        NamesHistoryView namesData = namesRepo.viewByUserId(type, targetId, offset, 10);
        boolean hasMorePages = namesData.page() < namesData.maxPages();
        List<Button> newButtons = EntityContextFactory.createNamesHistoryButtons(type, targetId, offset, hasMorePages);

        if (namesData.isEmpty())
            return ctx.reply(Status.NO_RESULT_FOUND);

        Bot.fetchUser(targetId).queue((target) -> {
            MessageEmbed newEmbed = EmbedFactory.embedUsernameUpdates(namesData, guild, target);

            ctx.create()
                    .setEmbeds(newEmbed)
                    .setActionRows(newButtons)
                    .edit();
        }, (error) -> ctx.reply(Status.USER_NOT_FOUND));
        return Status.OK;
    }
}