package ofc.bot.commands.impl.slash;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import ofc.bot.Main;
import ofc.bot.commands.impl.slash.levels.LevelsRolesCommand;
import ofc.bot.domain.entity.LevelRole;
import ofc.bot.domain.sqlite.repository.LevelRoleRepository;
import ofc.bot.handlers.economy.PaymentManagerProvider;
import ofc.bot.handlers.economy.unb.UnbelievaBoatClient;
import ofc.bot.handlers.interactions.commands.Cooldown;
import ofc.bot.handlers.interactions.commands.contexts.impl.SlashCommandContext;
import ofc.bot.handlers.interactions.commands.responses.states.InteractionResult;
import ofc.bot.handlers.interactions.commands.slash.abstractions.SlashCommand;
import ofc.bot.util.Bot;
import ofc.bot.util.content.annotations.commands.DiscordCommand;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.TimeUnit;

@DiscordCommand(name = "status")
public class BotStatusCommand extends SlashCommand {
    private static final long MEGABYTES = 1024 * 1024;
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
        String javaVersion = System.getProperty("java.version");
        Runtime runtime = Runtime.getRuntime();
        long apiPing = api.getRestPing().complete();
        long gatewayPing = api.getGatewayPing();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        long initTime = Main.getInitTime();
        long unbPing = findUnbelievaLatency(self);
        long imageryPing = findImageryLatency(guild, roles);
        int activeThreads = Thread.activeCount();

        MessageEmbed embed = embed(guild, javaVersion, usedMemory / MEGABYTES, apiPing,
                gatewayPing, unbPing, imageryPing, initTime, activeThreads);

        return ctx.replyEmbeds(embed);
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

    private long findUnbelievaLatency(Member self) {
        long init = System.currentTimeMillis();
        boolean ok = unbelievaBoatClient.get(self.getIdLong(), self.getGuild().getIdLong()) != null;
        if (!ok) return 0;

        long end = System.currentTimeMillis();
        return end - init;
    }

    private long findImageryLatency(Guild guild, List<LevelRole> roles) {
        long init = System.currentTimeMillis();
        boolean ok = LevelsRolesCommand.getRolesImage(guild, roles).length != 0;
        if (!ok) return 0;

        long end = System.currentTimeMillis();
        return end - init;
    }

    private MessageEmbed embed(Guild guild, String javaVersion, long usedMemoryMB, long apiPing,
                               long gatewayPing, long unbPing, long imageryPing, long initTime, int threadCount) {
        EmbedBuilder builder = new EmbedBuilder();

        String formattedPing = formatPing(apiPing, gatewayPing, unbPing, imageryPing);
        String threads = String.format("%02d", threadCount);
        String uptime = String.format("<t:%d>\n<t:%1$d:R>", initTime);
        int guildCount = Main.getApi().getGuilds().size();

        return builder
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

    private String formatPing(long apiPing, long gatewayPing, long unbPing, long imageryPing) {
        return String.format("""
                Gateway Ping: `%dms`.
                API Ping: `%dms`.
                Unbelieva Ping: %s.
                Imagery Ping: %s.
                """, apiPing, gatewayPing,

                // Non Discord-related
                unbPing == 0 ? "❌" : String.format("`%dms`", unbPing),
                imageryPing == 0 ? "❌" : String.format("`%dms`", imageryPing));
    }
}