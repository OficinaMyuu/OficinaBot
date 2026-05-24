package ofc.bot.listeners.discord.guilds.messages;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import ofc.bot.util.content.annotations.listeners.DiscordEventHandler;

@DiscordEventHandler
public class ErikPingReactionHelper extends ListenerAdapter {
    private static final long ERIK_ID = 145607100100247552L;

    @Override
    public void onMessageReceived(MessageReceivedEvent e) {
        Message msg = e.getMessage();
        User author = e.getAuthor();
        long authorId = author.getIdLong();

        if (authorId != ERIK_ID) return;

        if (!msg.getMentions().mentionsEveryone()) return;

        msg.addReaction(Emoji.fromUnicode("👀")).queue();
    }
}