package ofc.bot.listeners.discord.guilds.messages;

import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import ofc.bot.util.content.annotations.listeners.DiscordEventHandler;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.*;

@DiscordEventHandler
public class CoinflipInferenceHandler extends ListenerAdapter {
    private static final int TIMEOUT_MS = 5_000;
    private static final int COOLDOWN_MS = 30_000;
    private static final String COIN_EMOJI = "\uD83E\uDE99";

    private final Map<Long, PendingFlip> pendingFlips = new ConcurrentHashMap<>();
    private final Map<Long, Long> chanCooldowns = new ConcurrentHashMap<>();
    private final Random random = new Random();

    public CoinflipInferenceHandler() {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

        scheduler.scheduleAtFixedRate(() -> {
            long now = System.currentTimeMillis();

            this.pendingFlips.entrySet().removeIf((e) -> now - e.getValue().timestamp() > TIMEOUT_MS);

            this.chanCooldowns.entrySet().removeIf((e) -> now > e.getValue());
        }, 10, 10, TimeUnit.SECONDS);
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent e) {
        if (e.getAuthor().isBot()) return;

        MessageChannel chan = e.getChannel();
        long userId = e.getAuthor().getIdLong();
        long chanId = chan.getIdLong();
        long now = System.currentTimeMillis();

        if (now < chanCooldowns.getOrDefault(chanId, 0L)) return;

        String content = e.getMessage().getContentRaw()
                .trim()
                .toLowerCase()
                .replaceAll("[^a-z]", "");

        boolean isCara = content.equals("cara");
        boolean isCoroa = content.equals("coroa");

        if (!isCara && !isCoroa) return;

        PendingFlip pending = pendingFlips.get(chanId);
        if (pending == null) {
            pendingFlips.put(chanId, new PendingFlip(userId, isCara, now));
            return;
        }

        if (now - pending.timestamp() > TIMEOUT_MS) {
            pendingFlips.put(chanId, new PendingFlip(userId, isCara, now));
            return;
        }

        if (pending.userId() == userId || pending.isCara() == isCara) {
            pendingFlips.put(chanId, new PendingFlip(userId, isCara, now));
            return;
        }

        pendingFlips.remove(chanId);
        chanCooldowns.put(chanId, now + COOLDOWN_MS);

        String result = random.nextBoolean()
                ? COIN_EMOJI + " **Cara!**"
                : COIN_EMOJI + " **Coroa!**";

        chan.sendMessageFormat("<@%d> e <@%d> tiraram no cara ou coroa!\nResultado: %s",
                pending.userId(), userId, result).queue();
    }

    private record PendingFlip(long userId, boolean isCara, long timestamp) {}
}
