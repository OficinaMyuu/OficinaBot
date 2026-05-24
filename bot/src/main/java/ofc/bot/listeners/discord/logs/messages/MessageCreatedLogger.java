package ofc.bot.listeners.discord.logs.messages;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import ofc.bot.domain.entity.MessageVersion;
import ofc.bot.domain.sqlite.repository.MessageVersionRepository;
import ofc.bot.util.content.annotations.listeners.DiscordEventHandler;

@DiscordEventHandler
public class MessageCreatedLogger extends ListenerAdapter {
    private final MessageVersionRepository msgVrsRepo;

    public MessageCreatedLogger(MessageVersionRepository msgVrsRepo) {
        this.msgVrsRepo = msgVrsRepo;
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent e) {
        Message msg = e.getMessage();
        long creation = msg.getTimeCreated().toInstant().toEpochMilli();
        MessageVersion version = new MessageVersion(msg)
                .setDeleted(false)
                .setOriginal(true)
                .setTimeCreated(creation);

        msgVrsRepo.save(version);
    }
}