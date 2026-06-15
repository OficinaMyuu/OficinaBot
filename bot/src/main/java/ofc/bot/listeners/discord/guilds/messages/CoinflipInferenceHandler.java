package ofc.bot.listeners.discord.guilds.messages;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import ofc.bot.util.Bot;
import ofc.bot.util.content.Staff;
import ofc.bot.util.content.annotations.listeners.DiscordEventHandler;

import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.*;

@DiscordEventHandler
public class CoinflipInferenceHandler extends ListenerAdapter {
    private static final int TIMEOUT_MS = 5_000;
    private static final int COOLDOWN_MS = 30_000;
    private static final String COIN_EMOJI = "\uD83E\uDE99";
    private static final String BANNED_CHANNEL_IDS_KEY = "messages.coinflip.banned-channel-ids";

    private final Map<Long, PendingFlip> pendingFlips = new ConcurrentHashMap<>();
    private final Map<Long, Long> chanCooldowns = new ConcurrentHashMap<>();
    private final Random random;

    public CoinflipInferenceHandler() {
        this(new Random());

        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(this::cleanupExpiredEntries, 10, 10, TimeUnit.SECONDS);
    }

    CoinflipInferenceHandler(Random random) {
        this.random = Objects.requireNonNull(random);
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent e) {
        if (e.getAuthor().isBot() || !e.isFromGuild()) return;

        Member member = e.getMember();
        if (member == null) return;

        MessageChannel chan = e.getChannel();
        long chanId = chan.getIdLong();
        if (isBannedChannel(chanId)) return;

        Boolean choseCara = parseGuess(e.getMessage().getContentRaw());
        if (choseCara == null) return;

        long userId = e.getAuthor().getIdLong();
        long now = System.currentTimeMillis();
        boolean isStaff = Staff.isStaff(member);

        CoinflipResult result = handleGuess(chanId, userId, isStaff, choseCara, now);
        if (result == null) {
            return;
        }

        String resultText = result.caraWon()
                ? COIN_EMOJI + " **Cara!**"
                : COIN_EMOJI + " **Coroa!**";

        chan.sendMessageFormat("<@%d> e <@%d> tiraram no cara ou coroa!\nResultado: %s",
                result.firstUserId(), result.secondUserId(), resultText).queue();
    }

    CoinflipResult handleGuess(long channelId, long userId, boolean isStaff, boolean choseCara, long now) {
        PendingFlip pending = pendingFlips.get(channelId);
        PendingFlip currentFlip = new PendingFlip(userId, choseCara, isStaff, now);

        if (pending == null || hasExpired(pending, now)) {
            pendingFlips.put(channelId, currentFlip);
            return null;
        }

        if (pending.userId() == userId || pending.isCara() == choseCara) {
            pendingFlips.put(channelId, currentFlip);
            return null;
        }

        if (isOnCooldown(channelId, now) && !shouldIgnoreCooldown(pending, isStaff)) {
            pendingFlips.put(channelId, currentFlip);
            return null;
        }

        pendingFlips.remove(channelId);
        chanCooldowns.put(channelId, now + COOLDOWN_MS);

        return new CoinflipResult(pending.userId(), userId, random.nextBoolean());
    }

    private void cleanupExpiredEntries() {
        long now = System.currentTimeMillis();

        pendingFlips.entrySet().removeIf(entry -> hasExpired(entry.getValue(), now));
        chanCooldowns.entrySet().removeIf(entry -> now > entry.getValue());
    }

    private Boolean parseGuess(String content) {
        String normalized = content
                .trim()
                .toLowerCase()
                .replaceAll("[^a-z]", "");

        if (normalized.equals("cara")) return true;
        if (normalized.equals("coroa")) return false;

        return null;
    }

    private boolean hasExpired(PendingFlip pendingFlip, long now) {
        return now - pendingFlip.timestamp() > TIMEOUT_MS;
    }

    private boolean isOnCooldown(long channelId, long now) {
        return now < chanCooldowns.getOrDefault(channelId, 0L);
    }

    private boolean isBannedChannel(long channelId) {
        return isConfiguredChannelId(Bot.getArray(BANNED_CHANNEL_IDS_KEY), channelId);
    }

    static boolean isConfiguredChannelId(String[] rawChannelIds, long channelId) {
        String expected = Long.toString(channelId);
        for (String rawChannelId : rawChannelIds) {
            if (expected.equals(rawChannelId.trim())) return true;
        }
        return false;
    }

    private boolean shouldIgnoreCooldown(PendingFlip pendingFlip, boolean currentUserIsStaff) {
        return pendingFlip.isStaff() || currentUserIsStaff;
    }

    private record PendingFlip(long userId, boolean isCara, boolean isStaff, long timestamp) {}

    record CoinflipResult(long firstUserId, long secondUserId, boolean caraWon) {}
}
