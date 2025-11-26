package ofc.bot.listeners.discord.interactions.buttons.pagination.infractions;

import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageEmbed;
import ofc.bot.commands.slash.moderation.InfractionsCommand;
import ofc.bot.domain.entity.MemberPunishment;
import ofc.bot.handlers.interactions.AutoResponseType;
import ofc.bot.handlers.interactions.EntityContextFactory;
import ofc.bot.handlers.interactions.InteractionListener;
import ofc.bot.handlers.interactions.buttons.contexts.ButtonClickContext;
import ofc.bot.handlers.interactions.commands.responses.states.InteractionResult;
import ofc.bot.handlers.interactions.commands.responses.states.Status;
import ofc.bot.handlers.paginations.PageItem;
import ofc.bot.handlers.paginations.Paginator;
import ofc.bot.util.Bot;
import ofc.bot.util.Scopes;
import ofc.bot.util.content.annotations.listeners.InteractionHandler;
import ofc.bot.util.embeds.EmbedFactory;

import java.util.List;

@InteractionHandler(scope = Scopes.Punishments.VIEW_INFRACTIONS, autoResponseType = AutoResponseType.DEFER_EDIT)
public class InfractionsPageUpdate implements InteractionListener<ButtonClickContext> {

    @Override
    public InteractionResult onExecute(ButtonClickContext ctx) {
        boolean showInactive = ctx.get("show_inactive");
        int pageIndex = ctx.get("page_index");
        long targetId = ctx.get("target_id");
        long guildId = ctx.getGuildId();
        Guild guild = ctx.getGuild();
        PageItem<MemberPunishment> infrs = Paginator.viewInfractions(
                targetId, guildId, InfractionsCommand.PAGE_SIZE, pageIndex, showInactive);

        if (infrs.isEmpty())
            return Status.USER_HAS_NO_INFRACTIONS;

        MemberPunishment infr = infrs.get(0);
        boolean active = infr.isActive();
        List<Button> btns = EntityContextFactory.createInfractionsButtons(
                infr.getId(), active, targetId, pageIndex, showInactive, infrs.hasMore());

        Bot.fetchUser(targetId).queue(target -> {
            long infrModeratorId = infr.getModeratorId();
            MessageEmbed embed = EmbedFactory.embedInfractions(target, guild, infrs, infrModeratorId);

            ctx.create()
                    .setEmbeds(embed)
                    .setActionRows(btns)
                    .edit();
        }, (err) -> ctx.reply(Status.USER_NOT_FOUND));
        return Status.OK;
    }
}
