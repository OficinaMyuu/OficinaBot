package ofc.bot.listeners.discord.guilds.messages;

import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.exceptions.ErrorHandler;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.ErrorResponse;
import ofc.bot.util.content.annotations.listeners.DiscordEventHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

@DiscordEventHandler
public class SteamScamBlocker extends ListenerAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(SteamScamBlocker.class);

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (!event.isFromGuild()) return;

        Member member = event.getMember();
        Guild guild = event.getGuild();
        Member self = guild.getSelfMember();

        // If the member could not be resolved or the bot is unable to interact with the member,
        // then there is not much we can do (we cannot do anything lol).
        if (member == null || !self.canInteract(member)) return;

        Message message = event.getMessage();
        User user = member.getUser();
        MessageChannel channel = event.getChannel();
        String content = message.getContentRaw().toLowerCase();

        if (!content.contains("https://") && !content.contains("http://")) return;

        if ((content.contains("50$") || content.contains("$50")) && content.contains("steam")) {
            LOGGER.info("Attempting to ban member @{} for suspicious message content at #{}: {}", user.getName(), channel.getName(), content);
            member.ban(1, TimeUnit.DAYS)
                    .reason("50$ from steam")
                    .queue(s -> tempSecurityMessageWarning(channel, user));
        }
    }

    private void tempSecurityMessageWarning(MessageChannel channel, User user) {

        LOGGER.info("Successfully banned member @{}", user.getName());

        channel.sendMessageFormat("Membro %s foi banido por medidas segurança! Não cliquem em nenhum link desconhecido enviado pelo mesmo.", user.getAsMention())
                .delay(20, TimeUnit.SECONDS)
                .flatMap(Message::delete)
                .queue(null, new ErrorHandler()
                        .ignore(ErrorResponse.UNKNOWN_MESSAGE)
                );
    }
}