package ofc.bot.listeners.discord.interactions.modals.tickets;

import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import ofc.bot.domain.entity.SupportTicket;
import ofc.bot.domain.sqlite.repository.SupportTicketRepository;
import ofc.bot.handlers.interactions.InteractionListener;
import ofc.bot.handlers.interactions.commands.responses.states.InteractionResult;
import ofc.bot.handlers.interactions.commands.responses.states.Status;
import ofc.bot.handlers.interactions.modals.contexts.ModalSubmitContext;
import ofc.bot.util.Scopes;
import ofc.bot.util.content.annotations.listeners.InteractionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@InteractionHandler(scope = Scopes.Tickets.DELETE_TICKET)
public class TicketClosureHandler implements InteractionListener<ModalSubmitContext> {
    private static final Logger LOGGER = LoggerFactory.getLogger(TicketClosureHandler.class);
    private final SupportTicketRepository ticketRepo;

    public TicketClosureHandler(SupportTicketRepository ticketRepo) {
        this.ticketRepo = ticketRepo;
    }

    @Override
    public InteractionResult onExecute(ModalSubmitContext ctx) {
        long userId = ctx.getUserId();
        MessageChannel channel = ctx.getChannel();
        String reason = ctx.getField("reason");
        SupportTicket ticket = ticketRepo.findByChannelId(channel.getIdLong());

        if (ticket == null)
            return Status.TICKET_NOT_FOUND;

        try {
            ticket.setCloseReason(reason)
                    .setClosedAuthorId(userId)
                    .tickUpdate();

            ticketRepo.upsert(ticket);

            channel.delete().queue();
            return Status.TICKET_CLOSED_SUCCESSFULLY.args(ticket.getId());
        } catch (Exception e) {
            LOGGER.error("Failed to mark ticket as deleted");
            return Status.COULD_NOT_EXECUTE_SUCH_OPERATION;
        }
    }
}