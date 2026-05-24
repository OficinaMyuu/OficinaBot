package ofc.bot.listeners.discord.guilds.messages;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import ofc.bot.util.content.annotations.listeners.DiscordEventHandler;

@DiscordEventHandler
public class OutageCommandsDisclaimer extends ListenerAdapter {

    @Override
    public void onMessageReceived(MessageReceivedEvent e) {
        if (e.getAuthor().isBot() || e.isWebhookMessage() || !e.isFromGuild()) return;

        Message msg = e.getMessage();
        Guild guild = msg.getGuild();
        String content = msg.getContentRaw().toLowerCase();
        String selfMention = guild.getSelfMember().getAsMention();

        if (!content.startsWith("!rank")) return;

        msg.replyFormat("""
                O bot responsável por níveis não é o MEE6.
                Favor fazer o utilizo dos comandos `/rank` do bot %s!
                """, selfMention).queue();
    }
}