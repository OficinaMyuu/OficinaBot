package ofc.bot.listeners.discord.interactions.buttons.tickets;

import ofc.bot.domain.entity.MessageVersion;
import ofc.bot.domain.entity.SupportTicket;
import ofc.bot.domain.sqlite.repository.MessageVersionRepository;
import ofc.bot.handlers.interactions.AutoResponseType;
import ofc.bot.handlers.interactions.InteractionListener;
import ofc.bot.handlers.interactions.buttons.contexts.ButtonClickContext;
import ofc.bot.handlers.interactions.commands.responses.states.InteractionResult;
import ofc.bot.handlers.interactions.commands.responses.states.Status;
import ofc.bot.util.Scopes;
import ofc.bot.util.content.annotations.listeners.InteractionHandler;

import java.util.List;

@InteractionHandler(scope = Scopes.Tickets.DOWNLOAD_MESSAGES, autoResponseType = AutoResponseType.DEFER_EDIT)
public class DownloadMessagesHandler implements InteractionListener<ButtonClickContext> {
    private final MessageVersionRepository msgVrsRepo;

    public DownloadMessagesHandler(MessageVersionRepository msgVrsRepo) {
        this.msgVrsRepo = msgVrsRepo;
    }

    @Override
    public InteractionResult onExecute(ButtonClickContext ctx) {
        SupportTicket ticket = ctx.get("ticket");
        long chanId = ticket.getChannelId();
        List<MessageVersion> messages = msgVrsRepo.findLastValid(chanId);


        return Status.OK;
    }
}