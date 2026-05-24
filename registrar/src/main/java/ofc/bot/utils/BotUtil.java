package ofc.bot.utils;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.exceptions.ErrorHandler;
import net.dv8tion.jda.api.requests.ErrorResponse;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

public class BotUtil {

    public static void tempMessage(String content, MessageChannel chan, long millis) {
        chan.sendMessage(content)
                .delay(millis, TimeUnit.MILLISECONDS)
                .flatMap(Message::delete)
                .queue(null, new ErrorHandler().ignore(ErrorResponse.UNKNOWN_MESSAGE));
    }

    public static void delete(Message msg) {
        msg.delete().queue(null, new ErrorHandler().ignore(ErrorResponse.UNKNOWN_MESSAGE));
    }

    public static long unixNow() {
        return Instant.now().toEpochMilli();
    }
}