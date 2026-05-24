package ofc.bot.listeners.discord.guilds.messages;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import ofc.bot.domain.entity.UserXP;
import ofc.bot.handlers.LevelManager;
import ofc.bot.util.content.annotations.listeners.DiscordEventHandler;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@DiscordEventHandler
public class UsersXPHandler extends ListenerAdapter {
    // Mapping <UserID, Timestamp>
    private static final Map<Long, Long> TIMESTAMPS = new HashMap<>();
    private static final int COOLDOWN_MS = 60 * 1000; // 60s
    private static final Random RANDOM = new Random();
    private final LevelManager levelManager;

    public UsersXPHandler() {
        levelManager = LevelManager.getManager();
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent e) {
        if (!e.isFromGuild()) return;

        User user = e.getAuthor();
        Member member = e.getMember();
        GuildChannel channel = e.getGuildChannel();
        long userId = user.getIdLong();

        if (user.isBot() || member == null || !isAllowed(userId)) return;

        int xpMod = RANDOM.nextInt(UserXP.MIN_CYCLE, UserXP.MAX_CYCLE + 1);
        levelManager.addXp(member, channel, xpMod);
    }

    private boolean isAllowed(long userId) {
        long lastMessage = TIMESTAMPS.getOrDefault(userId, 0L);
        long now = System.currentTimeMillis();

        if (now - lastMessage < COOLDOWN_MS) {
            return false;
        }

        TIMESTAMPS.put(userId, now);
        return true;
    }
}