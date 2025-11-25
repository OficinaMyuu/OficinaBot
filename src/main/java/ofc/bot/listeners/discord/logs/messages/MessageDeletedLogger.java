package ofc.bot.listeners.discord.logs.messages;

import net.dv8tion.jda.api.audit.ActionType;
import net.dv8tion.jda.api.audit.AuditLogEntry;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.message.MessageDeleteEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import ofc.bot.domain.entity.MessageVersion;
import ofc.bot.domain.sqlite.repository.MessageVersionRepository;
import ofc.bot.util.Bot;
import ofc.bot.util.content.annotations.listeners.DiscordEventHandler;

@DiscordEventHandler
public class MessageDeletedLogger extends ListenerAdapter {
    private final MessageVersionRepository msgVrsRepo;

    public MessageDeletedLogger(MessageVersionRepository msgVrsRepo) {
        this.msgVrsRepo = msgVrsRepo;
    }

    @Override
    public void onMessageDelete(MessageDeleteEvent e) {
        if (!e.isFromGuild()) return;

        long now = Bot.nowMillis();
        long messageId = e.getMessageIdLong();
        long chanId = e.getChannel().getIdLong();
        Guild guild = e.getGuild();
        guild.retrieveAuditLogs().limit(1).type(ActionType.MESSAGE_DELETE).queue((entries) -> {
            AuditLogEntry entry = entries.isEmpty() ? null : entries.getFirst();

            if (entry == null) return;

            long targetId = entry.getTargetIdLong();
            long issuerId = entry.getUserIdLong();
            boolean isConsistent = msgVrsRepo.findsByMessageAndAuthorId(messageId, targetId);
            Long deleter = isConsistent ? issuerId : null;

            MessageVersion version = new MessageVersion()
                    .setMessageId(messageId)
                    .setAuthorId(0)
                    .setChannelId(chanId)
                    .setDeleted(true)
                    .setDeletedById(deleter)
                    .setTimeCreated(now);

            msgVrsRepo.save(version);
        });
    }
}