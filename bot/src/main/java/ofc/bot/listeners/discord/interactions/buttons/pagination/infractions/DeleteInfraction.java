package ofc.bot.listeners.discord.interactions.buttons.pagination.infractions;

import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageEmbed;
import ofc.bot.commands.impl.slash.moderation.InfractionsCommand;
import ofc.bot.domain.entity.MemberPunishment;
import ofc.bot.domain.sqlite.repository.MemberPunishmentRepository;
import ofc.bot.handlers.interactions.AutoResponseType;
import ofc.bot.handlers.interactions.InteractionListener;
import ofc.bot.handlers.interactions.buttons.contexts.ButtonClickContext;
import ofc.bot.handlers.interactions.EntityContextFactory;
import ofc.bot.handlers.interactions.commands.responses.states.InteractionResult;
import ofc.bot.handlers.interactions.commands.responses.states.Status;
import ofc.bot.handlers.paginations.PageItem;
import ofc.bot.handlers.paginations.Paginator;
import ofc.bot.util.Bot;
import ofc.bot.util.Scopes;
import ofc.bot.util.content.annotations.listeners.InteractionHandler;
import ofc.bot.util.embeds.EmbedFactory;
import org.jooq.exception.DataAccessException;

import java.util.List;

@InteractionHandler(scope = Scopes.Punishments.DELETE_INFRACTION, autoResponseType = AutoResponseType.DEFER_EDIT)
public class DeleteInfraction implements InteractionListener<ButtonClickContext> {
    private final MemberPunishmentRepository pnshRepo;

    public DeleteInfraction(MemberPunishmentRepository pnshRepo) {
        this.pnshRepo = pnshRepo;
    }

    @Override
    public InteractionResult onExecute(ButtonClickContext ctx) {
        long issuerId = ctx.getUserId();
        long guildId = ctx.getGuildId();
        long targetId = ctx.get("target_id");
        int pageIndex = ctx.get("page_index"); // Current page
        int infrId = ctx.get("infraction_id");
        boolean showInactive = ctx.get("show_inactive");
        Guild guild = ctx.getGuild();
        MemberPunishment infr = pnshRepo.findById(infrId);

        if (infr == null)
            return Status.INFRACTION_NOT_FOUND;

        infr.setActive(false)
                .setDeletionAuthorId(issuerId)
                .tickUpdate();

        // Saving to the database the inactivation ^^
        try {
            pnshRepo.upsert(infr);
        } catch (DataAccessException e) {
            return Status.COULD_NOT_EXECUTE_SUCH_OPERATION;
        }

        Bot.fetchUser(targetId).queue(target -> {
            int newPageIndex = Math.max(pageIndex - 1, 0);
            PageItem<MemberPunishment> infrs = Paginator.viewInfractions(
                    targetId, guildId, InfractionsCommand.PAGE_SIZE, newPageIndex, showInactive);

            if (infrs.isEmpty()) {
                ctx.edit(Status.USER_HAS_NO_INFRACTIONS);
                return;
            }

            MemberPunishment updatedIfr = infrs.get(0);
            boolean active = updatedIfr.isActive();
            MessageEmbed embed = EmbedFactory.embedInfractions(target, guild, infrs, issuerId);
            List<Button> btns = EntityContextFactory.createInfractionsButtons(
                    updatedIfr.getId(), active, targetId, newPageIndex, showInactive, infrs.hasMore());

            ctx.create()
                    .setEmbeds(embed)
                    .setActionRows(btns)
                    .edit();
        }, (err) -> ctx.reply(Status.USER_NOT_FOUND));
        return Status.OK;
    }
}