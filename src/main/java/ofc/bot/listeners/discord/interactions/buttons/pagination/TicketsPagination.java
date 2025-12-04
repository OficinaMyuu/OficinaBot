package ofc.bot.listeners.discord.interactions.buttons.pagination;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.entities.*;
import ofc.bot.Main;
import ofc.bot.domain.entity.SupportTicket;
import ofc.bot.domain.sqlite.repository.MessageVersionRepository;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Set;

@InteractionHandler(scope = Scopes.Tickets.PAGINATE_TICKETS, autoResponseType = AutoResponseType.DEFER_EDIT)
public class TicketsPagination implements InteractionListener<ButtonClickContext> {
    private static final Logger LOGGER = LoggerFactory.getLogger(TicketsPagination.class);
    private final MessageVersionRepository msgVrsRepo;

    public TicketsPagination(MessageVersionRepository msgVrsRepo) {
        this.msgVrsRepo = msgVrsRepo;
    }

    @Override
    public InteractionResult onExecute(ButtonClickContext ctx) {
        JDA api = Main.getApi();
        SelfUser selfUser = api.getSelfUser();
        User byUser = ctx.find("by_user");
        int pageIndex = ctx.get("page_index");
        PageItem<SupportTicket> tickets = Paginator.viewTickets(byUser, pageIndex);
        long selfId = selfUser.getIdLong();

        if (tickets.isEmpty())
            return Status.NO_TICKETS_FOUND;

        SupportTicket ticket = tickets.get(0);
        long chanId = ticket.getChannelId();
        boolean hasMore = tickets.hasMore();
        Guild guild = ctx.getGuild();
        Set<Long> users = msgVrsRepo.findUsersByChannelId(chanId);
        users.remove(selfId);

        api.retrieveUserById(ticket.getInitiatorId()).queue(issuer -> {
            MessageEmbed embed = EmbedFactory.embedTicketPage(issuer, guild, ticket, users);
            List<Button> buttons = EntityContextFactory.createTicketsButtons(byUser, pageIndex, hasMore);

            ctx.create()
                    .setEmbeds(embed)
                    .setActionRows(buttons)
                    .edit();
        }, (err) -> { // Also ???
            LOGGER.error("We failed to fetch the user who issued ticket {}", ticket.getId(), err);
        });
        return Status.OK;
    }
}