package ofc.bot.listeners.discord.guilds.messages;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import ofc.bot.domain.entity.MentionLog;
import ofc.bot.domain.sqlite.repository.MentionLogRepository;
import ofc.bot.util.content.annotations.listeners.DiscordEventHandler;
import org.apache.commons.collections4.Bag;

@DiscordEventHandler
public class MentionLoggerHandler extends ListenerAdapter {
    private final MentionLogRepository mentionLogRepo;

    public MentionLoggerHandler(MentionLogRepository mentionLogRepo) {
        this.mentionLogRepo = mentionLogRepo;
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent e) {
        if (!e.isFromGuild()) return;

        Message msg = e.getMessage();
        User author = e.getAuthor();
        Bag<User> mentions = msg.getMentions().getUsersBag();
        long msgId = msg.getIdLong();
        long authorId = author.getIdLong();

        mentions.forEach((mnt) -> {
            MentionLog entry = new MentionLog(msgId, authorId, mnt.getIdLong());
            mentionLogRepo.save(entry);
        });
    }
}