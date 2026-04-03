package ofc.bot.listeners.discord.guilds.messages;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.exceptions.ErrorHandler;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.ErrorResponse;
import ofc.bot.domain.entity.WelcomedUser;
import ofc.bot.domain.sqlite.repository.WelcomedUserRepository;
import ofc.bot.util.Bot;
import ofc.bot.util.content.annotations.listeners.DiscordEventHandler;
import org.jooq.exception.DataAccessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

@DiscordEventHandler
public class WelcomeCommandHandler extends ListenerAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(WelcomeCommandHandler.class);
    private static final Emoji SUCCESS_EMOJI = Emoji.fromUnicode("✅");
    private static final ErrorHandler MSG_ERROR_HANDLER = new ErrorHandler().ignore(ErrorResponse.UNKNOWN_MESSAGE);
    private final WelcomedUserRepository welcomedRepo;

    public WelcomeCommandHandler(WelcomedUserRepository welcomedRepo) {
        this.welcomedRepo = welcomedRepo;
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent e) {
        JDA jda = e.getJDA();
        Message msg = e.getMessage();
        String content = msg.getContentRaw();
        String[] args = content.split(" ");
        User author = msg.getAuthor();

        if (!e.isFromGuild()) return;

        if (!args[0].equals(".welcomed") && !args[0].equals(".wd")) return;

        if (args.length < 2) {
            sendError(msg, "> ❌ Uso incorreto: `.wd <user> [mensagem]`.");
            return;
        }

        long userId = author.getIdLong();
        long guildId = msg.getGuildIdLong();
        long targetId = extractNumbers(args[1]);
        String comment = getComment(args);

        if (targetId == 0) {
            sendError(msg, "ID inválido informado.");
            return;
        }

        jda.retrieveUserById(targetId).queue(user -> {
            try {
                long now = Bot.nowMillis();
                WelcomedUser welcomed = new WelcomedUser(guildId, userId, targetId, comment, now);
                welcomedRepo.save(welcomed);

                sendSuccess(msg);
            } catch (DataAccessException err) {
                LOGGER.error("Failed to save welcomed user {} to database", targetId, err);
            }
        }, (err) -> sendError(msg, "Usuário não encontrado."));
    }

    private void sendSuccess(Message msg) {
        msg.addReaction(SUCCESS_EMOJI).queue();
        msg.delete().queueAfter(5, TimeUnit.SECONDS, null, MSG_ERROR_HANDLER);
    }

    private void sendError(Message msg, String content) {
        msg.reply(content).queue((r) -> scheduleDelete(r, msg));
    }

    private void scheduleDelete(Message... msgs) {
        for (Message msg : msgs) {
            msg.delete().queueAfter(10, TimeUnit.SECONDS, null, MSG_ERROR_HANDLER);
        }
    }

    private long extractNumbers(String input) {
        String val =  input.replaceAll("\\D+", "");
        try {
            return Long.parseLong(val);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private String getComment(String[] args) {
        return String.join(" ", Arrays.copyOfRange(args, 2, args.length));
    }
}
