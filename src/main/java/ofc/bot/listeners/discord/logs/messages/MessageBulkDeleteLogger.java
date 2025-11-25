package ofc.bot.listeners.discord.logs.messages;

import net.dv8tion.jda.api.audit.ActionType;
import net.dv8tion.jda.api.audit.AuditLogEntry;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.message.MessageBulkDeleteEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import ofc.bot.domain.entity.MessageVersion;
import ofc.bot.domain.sqlite.repository.MessageVersionRepository;
import ofc.bot.util.Bot;
import ofc.bot.util.content.annotations.listeners.DiscordEventHandler;

import java.util.List;

@DiscordEventHandler
public class MessageBulkDeleteLogger extends ListenerAdapter {
    private final MessageVersionRepository msgVrsRepo;

    public MessageBulkDeleteLogger(MessageVersionRepository msgVrsRepo) {
        this.msgVrsRepo = msgVrsRepo;
    }

    @Override
    public void onMessageBulkDelete(MessageBulkDeleteEvent e) {
        long chanId = e.getChannel().getIdLong();
        long now = Bot.nowMillis();
        Guild guild = e.getGuild();
        List<Long> deleted = e.getMessageIds()
                .stream()
                .map(Long::parseLong)
                .toList();

        guild.retrieveAuditLogs().type(ActionType.MESSAGE_BULK_DELETE).limit(1).queue((entries) -> {
            AuditLogEntry entry = entries.isEmpty() ? null : entries.getFirst();

            if (entry == null) return;

            long deleter = entry.getUserIdLong();
            List<MessageVersion> versions = deleted.stream()
                    .map((id) -> new MessageVersion()
                            .setMessageId(id)
                            .setAuthorId(0)
                            .setChannelId(chanId)
                            .setDeleted(true)
                            .setDeletedById(deleter)
                            .setTimeCreated(now))
                    .toList();

            msgVrsRepo.bulkSave(versions);
        });
    }
}