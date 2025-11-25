package ofc.bot.listeners.discord.logs.messages;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.MessageUpdateEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import ofc.bot.domain.entity.MessageVersion;
import ofc.bot.domain.sqlite.repository.MessageVersionRepository;
import ofc.bot.util.Bot;
import ofc.bot.util.content.annotations.listeners.DiscordEventHandler;

import java.time.OffsetDateTime;

@DiscordEventHandler
public class MessageUpdatedLogger extends ListenerAdapter {
    private final MessageVersionRepository msgVrsRepo;

    public MessageUpdatedLogger(MessageVersionRepository msgVrsRepo) {
        this.msgVrsRepo = msgVrsRepo;
    }

    @Override
    public void onMessageUpdate(MessageUpdateEvent e) {
        Message msg = e.getMessage();
        OffsetDateTime timeEdited = msg.getTimeEdited();
        long timestamp = timeEdited == null ? Bot.nowMillis() : timeEdited.toInstant().toEpochMilli();
        MessageVersion version = new MessageVersion(msg)
                .setDeleted(false)
                .setTimeCreated(timestamp);

        msgVrsRepo.save(version);
    }
}