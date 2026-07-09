package ofc.bot.commands.impl.slash;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import ofc.bot.Main;
import ofc.bot.commands.impl.slash.levels.LevelsRolesCommand;
import ofc.bot.domain.entity.LevelRole;
import ofc.bot.domain.database.repository.LevelRoleRepository;
import ofc.bot.handlers.economy.PaymentManagerProvider;
import ofc.bot.handlers.economy.unb.UnbelievaBoatClient;
import ofc.bot.handlers.interactions.commands.Cooldown;
import ofc.bot.handlers.interactions.commands.contexts.impl.SlashCommandContext;
import ofc.bot.handlers.interactions.commands.responses.states.InteractionResult;
import ofc.bot.handlers.interactions.commands.responses.states.Status;
import ofc.bot.handlers.interactions.commands.slash.abstractions.SlashCommand;
import ofc.bot.util.Bot;
import ofc.bot.util.content.annotations.commands.DiscordCommand;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@DiscordCommand(name = "status")
public class BotStatusCommand extends SlashCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(BotStatusCommand.class);
    private static final long MEGABYTES = 1024 * 1024;
    private static final String LOADING = Bot.Emojis.LOADING.getFormatted();

    private final UnbelievaBoatClient unbelievaBoatClient = PaymentManagerProvider.getUnbelievaBoatClient();
    private final LevelRoleRepository lvlRoleRepo;

    public BotStatusCommand(LevelRoleRepository lvlRoleRepo) {
        this.lvlRoleRepo = lvlRoleRepo;
    }

    @Override
    public InteractionResult onCommand(@NotNull SlashCommandContext ctx) {
        JDA api = Main.getApi();
        Guild guild = ctx.getGuild();
        Member self = guild.getSelfMember();
        List<LevelRole> roles = lvlRoleRepo.findAll();

        ctx.ack();

        long gatewayPing = api.getGatewayPing();
        long initTime = Main.getInitTime();
        Runtime runtime = Runtime.getRuntime();
        long usedMemoryMB = (runtime.totalMemory() - runtime.freeMemory()) / MEGABYTES;
        String javaVersion = System.getProperty("java.version");
        int activeThreads = Thread.activeCount();

        // Send the initial response immediately with loading spinners for the slow pings
        MessageEmbed initial = buildEmbed(guild, javaVersion, usedMemoryMB, LOADING,
                gatewayPing, LOADING, LOADING, initTime, activeThreads);

        ctx.getSource().getHook()
                .editOriginalEmbeds(initial)
                .queue(msg -> resolvePings(ctx, api, self, guild, roles, javaVersion,
                        usedMemoryMB, gatewayPing, initTime, activeThreads));

        return Status.OK;
    }

    @NotNull
    @Override
    public String getDescription() {
        return "Veja os status do bot :)";
    }

    @NotNull
    @Override
    public Cooldown getCooldown() {
        return Cooldown.of(false, true, 30, TimeUnit.SECONDS);
    }

    private void resolvePings(SlashCommandContext ctx, JDA api, Member self, Guild guild,
                              List<LevelRole> roles, String javaVersion, long usedMemoryMB,
                              long gatewayPing, long initTime, int activeThreads) {

        CompletableFuture<String> apiPingFuture = CompletableFuture.supplyAsync(() -> {
            long ping = api.getRestPing().complete();
            return formatMs(ping);
        });

        CompletableFuture<String> unbPingFuture = CompletableFuture.supplyAsync(() ->
                formatLatency(() -> unbelievaBoatClient.get(self.getIdLong(), self.getGuild().getIdLong()) != null)
        );

        CompletableFuture<String> imageryPingFuture = CompletableFuture.supplyAsync(() ->
                formatLatency(() -> LevelsRolesCommand.getRolesImage(guild, roles).length != 0)
        );

        CompletableFuture.allOf(apiPingFuture, unbPingFuture, imageryPingFuture)
                .whenComplete((ignored, error) -> {
                    if (error != null) {
                        LOGGER.error("Unexpected error while resolving status pings", error);
                    }

                    String apiPing = resolve(apiPingFuture);
                    String unbPing = resolve(unbPingFuture);
                    String imageryPing = resolve(imageryPingFuture);

                    MessageEmbed embed = buildEmbed(guild, javaVersion, usedMemoryMB, apiPing,
                            gatewayPing, unbPing, imageryPing, initTime, activeThreads);

                    ctx.getSource().getHook()
                            .editOriginalEmbeds(embed)
                            .queue(null, err -> LOGGER.error("Failed to edit /status response", err));
                });
    }

    private MessageEmbed buildEmbed(Guild guild, String javaVersion, long usedMemoryMB,
                                    String apiPing, long gatewayPing, String unbPing,
                                    String imageryPing, long initTime, int threadCount) {
        String formattedPing = formatPing(apiPing, gatewayPing, unbPing, imageryPing);
        String threads = String.format("%02d", threadCount);
        String uptime = String.format("<t:%d>\n<t:%1$d:R>", initTime);
        int guildCount = Main.getApi().getGuilds().size();

        return new EmbedBuilder()
                .setTitle("Oficina's Status")
                .addField("📡 Response Time", formattedPing, true)
                .addField("🕒 Uptime", uptime, true)
                .addField("💻 Used Memory", usedMemoryMB + " MB", true)
                .addField("👥 Members Cached", Bot.fmtNum(guild.getMembers().size()), true)
                .addField("🌐 Guilds", Bot.fmtNum(guildCount), true)
                .addField("☕ Java Version", javaVersion, true)
                .addField("🤝 Active Threads", threads, true)
                .setColor(Bot.Colors.DISCORD)
                .build();
    }

    private String formatPing(String apiPing, long gatewayPing, String unbPing, String imageryPing) {
        return String.format("""
                Gateway Ping: `%dms`.
                API Ping: %s.
                Unbelieva Ping: %s.
                Imagery Ping: %s.
                """, gatewayPing, apiPing, unbPing, imageryPing);
    }

    /**
     * Measures the latency of a probe action.
     * Returns a formatted millisecond string on success, or "❌" if the
     * probe returned {@code false} or threw an exception.
     */
    private static String formatLatency(PingProbe probe) {
        try {
            long start = System.currentTimeMillis();
            boolean ok = probe.check();
            if (!ok) return "❌";

            return formatMs(System.currentTimeMillis() - start);
        } catch (Exception e) {
            LOGGER.error("Ping probe failed", e);
            return "❌";
        }
    }

    private static String formatMs(long ms) {
        return String.format("`%dms`", ms);
    }

    /**
     * Safely resolves a completed future, returning "❌" if it failed.
     */
    private static String resolve(CompletableFuture<String> future) {
        try {
            return future.join();
        } catch (Exception e) {
            return "❌";
        }
    }

    @FunctionalInterface
    private interface PingProbe {
        boolean check();
    }
}