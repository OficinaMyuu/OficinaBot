package ofc.bot.listeners.discord.interactions.dm;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Message.Attachment;
import net.dv8tion.jda.api.entities.SelfUser;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.sticker.StickerItem;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import ofc.bot.Main;
import ofc.bot.util.content.annotations.listeners.DiscordEventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@DiscordEventHandler
public class DirectMessageReceived extends ListenerAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(DirectMessageReceived.class);

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.isFromGuild()) return;
    
        User author = event.getAuthor();
        SelfUser self = Main.getApi().getSelfUser();
        Message message = event.getMessage();
        String content = message.getContentDisplay();
        List<Attachment> attachments = message.getAttachments();
        List<StickerItem> stickers = message.getStickers();
        StringBuilder builder = new StringBuilder();

        if (author.getIdLong() == self.getIdLong()) return;
        
        builder.append(String.format("[DM] %s: %s", author.getEffectiveName(), content));

        if (!attachments.isEmpty()) {
            builder.append("\n[ATTACHMENTS]");

            for (Attachment a : attachments)
                builder.append("\nULR: ")
                        .append(a.getUrl());
        }

        if (!stickers.isEmpty()) {
            builder.append("\n[STICKERS]");

            for (StickerItem s : stickers)
                builder.append("\nURL: ")
                        .append(s.getIconUrl())
                        .append(String.format(" (:%s:)", s.getName()));
        }

        LOGGER.info(builder.toString().strip());
    }
}