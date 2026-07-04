package ofc.bot.listeners.discord.guilds.messages;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import ofc.bot.domain.entity.AppUser;
import ofc.bot.domain.database.repository.UserPreferenceRepository;
import ofc.bot.domain.database.repository.UserRepository;
import ofc.bot.util.content.annotations.listeners.DiscordEventHandler;
import org.jooq.exception.DataAccessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@DiscordEventHandler
public class ToggleRankupPingsCommandHandler extends ListenerAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(ToggleRankupPingsCommandHandler.class);
    private static final String COMMAND = ".toggle-rankup-pings";
    private final UserPreferenceRepository preferenceRepo;
    private final UserRepository userRepo;

    public ToggleRankupPingsCommandHandler(UserPreferenceRepository preferenceRepo, UserRepository userRepo) {
        this.preferenceRepo = preferenceRepo;
        this.userRepo = userRepo;
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (!event.isFromGuild()) return;

        Message message = event.getMessage();
        User author = event.getAuthor();
        String content = message.getContentRaw().strip();

        if (author.isBot() || !content.equals(COMMAND)) return;

        long userId = author.getIdLong();
        boolean enabled = !preferenceRepo.isRankupPingsEnabled(userId);

        try {
            userRepo.upsert(AppUser.fromUser(author));
            preferenceRepo.setRankupPings(userId, enabled);
            message.reply(responseFor(enabled)).queue();
        } catch (DataAccessException err) {
            LOGGER.error("Failed to update rankup ping preference for user {}", userId, err);
            message.reply("Nao consegui atualizar sua preferencia de pings de rank up agora.").queue();
        }
    }

    static String responseFor(boolean enabled) {
        return enabled
                ? "Pings de rank up ativados."
                : "Pings de rank up desativados.";
    }
}
