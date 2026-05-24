package ofc.bot.jobs;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import ofc.bot.Main;
import ofc.bot.domain.entity.VoiceHeartbeat;
import ofc.bot.domain.sqlite.repository.Repositories;
import ofc.bot.domain.sqlite.repository.VoiceHeartbeatRepository;
import ofc.bot.util.Bot;
import ofc.bot.util.content.annotations.jobs.CronJob;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@CronJob(expression = "0 * * ? * * *") // Every minute
public class VoiceHeartbeatCounter implements Job {
    private static final Logger LOGGER = LoggerFactory.getLogger(VoiceHeartbeatCounter.class);
    private final VoiceHeartbeatRepository vcHeartbeatRepo = Repositories.getVoiceHeartbeatRepository();

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        JDA api = Main.getApi();

        LOGGER.info("Gathering populated voice channels to store voice heartbeats");
        List<VoiceChannel> channels = getPopulatedVoiceChannels(api);
        LOGGER.info("Found {} populated voice channels", channels.size());

        List<VoiceHeartbeat> heartbeats = channels.stream().flatMap(vc -> vc.getMembers().stream().map(m -> {
            GuildVoiceState state = m.getVoiceState();
            boolean isMuted = state != null && state.isMuted();
            boolean isDeafened = state != null && state.isDeafened();
            boolean isVideo = state != null && state.isSendingVideo();
            boolean isStreaming = state != null && state.isStream();
            long userId = m.getIdLong();
            long chanId = vc.getIdLong();
            long now = Bot.nowMillis();

            return new VoiceHeartbeat(userId, chanId, isMuted, isDeafened, isVideo, isStreaming, now);
        })).toList();

        LOGGER.info("Bulk saving {} voice heartbeats", heartbeats.size());
        vcHeartbeatRepo.bulkSave(heartbeats);
    }

    private List<VoiceChannel> getPopulatedVoiceChannels(JDA api) {
        return api.getGuilds()
                .stream()
                .flatMap(g -> g.getVoiceChannels().stream())
                .filter(vc -> !vc.getMembers().isEmpty())
                .toList();
    }
}