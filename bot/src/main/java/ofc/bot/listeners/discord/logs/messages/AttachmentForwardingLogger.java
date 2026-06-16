package ofc.bot.listeners.discord.logs.messages;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import ofc.bot.util.Bot;
import ofc.bot.util.content.annotations.listeners.DiscordEventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@DiscordEventHandler
public class AttachmentForwardingLogger extends ListenerAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(AttachmentForwardingLogger.class);

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        Message message = event.getMessage();
        Long archiveChannelId = getArchiveChannelId();

        if (!AttachmentForwardingPolicy.shouldForward(
                event.isFromGuild(),
                event.getAuthor().isBot(),
                event.isWebhookMessage(),
                !message.getAttachments().isEmpty(),
                archiveChannelId
        )) {
            return;
        }

        TextChannel archiveChannel = event.getGuild().getTextChannelById(archiveChannelId);
        if (archiveChannel == null) return;

        message.forwardTo(archiveChannel).queue(
                null,
                err -> LOGGER.warn("Could not forward attachment message {} to channel {}",
                        message.getIdLong(), archiveChannelId, err)
        );
    }

    private Long getArchiveChannelId() {
        String rawChannelId = Bot.get(AttachmentForwardingPolicy.CHANNEL_CONFIG_KEY);
        if (rawChannelId == null || rawChannelId.isBlank()) return null;

        try {
            return Long.parseLong(rawChannelId.strip());
        } catch (NumberFormatException e) {
            LOGGER.warn("Invalid attachment archive channel id in key {}: {}",
                    AttachmentForwardingPolicy.CHANNEL_CONFIG_KEY, rawChannelId);
            return null;
        }
    }
}
