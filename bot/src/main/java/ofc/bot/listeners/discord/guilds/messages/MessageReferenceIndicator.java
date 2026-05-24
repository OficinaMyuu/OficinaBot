package ofc.bot.listeners.discord.guilds.messages;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageReference;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import ofc.bot.util.content.annotations.listeners.DiscordEventHandler;

@DiscordEventHandler
public class MessageReferenceIndicator extends ListenerAdapter {

    @Override
    public void onMessageReceived(MessageReceivedEvent e) {
        Message msg = e.getMessage();
        String content = msg.getContentRaw();
        MessageReference ref = msg.getMessageReference();

        if (!content.equals(".r") || ref == null) return;

        ref.resolve().queue(refMsg -> {
            MessageReference targetRef = refMsg.getMessageReference();

            if (targetRef == null) return;

            String url = getUrl(targetRef);
            msg.reply(url).queue();
        });
    }

    private String getUrl(MessageReference ref) {
        return String.format(Message.JUMP_URL, ref.getGuildId(), ref.getChannelId(), ref.getMessageId());
    }
}