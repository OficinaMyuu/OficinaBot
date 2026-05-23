package ofc.bot.listeners;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.exceptions.ErrorHandler;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.ErrorResponse;
import ofc.bot.utils.Channels;
import ofc.bot.utils.CommandParser;
import ofc.bot.utils.Roles;

public class NumberlessMessagesRemover extends ListenerAdapter {
    private static final ErrorHandler DEFAULT_ERROR_HANDLER = new ErrorHandler().ignore(ErrorResponse.UNKNOWN_MESSAGE);

    @Override
    public void onMessageReceived(MessageReceivedEvent e) {
        Message msg = e.getMessage();
        MessageChannel channel = e.getChannel();
        String content = msg.getContentRaw();
        Member member = e.getMember();
        long channelId = channel.getIdLong();

        // Yes, we are gonna query the database for every received message,
        // but this is a very small bot, there is no problem with it
        if (channelId != Channels.REGISTER.fetchId()) return;

        if (member == null || isAllowed(member)) return;

        if (!hasNumbers(content)) {
            msg.delete().queue(null, DEFAULT_ERROR_HANDLER);
        }
    }

    private boolean isAllowed(Member member) {
        return Roles.REGISTRAR.isPresent(member) || member.hasPermission(Permission.MANAGE_CHANNEL);
    }

    private boolean hasNumbers(String text) {
        return !CommandParser.getNumbers(text).isBlank();
    }
}