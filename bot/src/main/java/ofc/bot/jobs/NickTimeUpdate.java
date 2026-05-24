package ofc.bot.jobs;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import ofc.bot.Main;
import ofc.bot.internal.data.BotProperties;
import ofc.bot.util.content.annotations.jobs.CronJob;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

@CronJob(expression = "0 * * ? * * *")
public class NickTimeUpdate implements Job {
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final Logger LOGGER = LoggerFactory.getLogger(NickTimeUpdate.class);

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        String userId = BotProperties.fetch("nick.user");
        JDA api = Main.getApi();
        Guild guild = api.getGuildById(582430782577049600L);

        if (userId == null) {
            LOGGER.warn("No users found");
            return;
        }

        if (guild == null) {
            LOGGER.warn("Guild not found");
            return;
        }

        String zoneId = BotProperties.fetch("zone.id");
        String username = BotProperties.fetch("nick.user.name.format");
        if (zoneId == null || username == null) {
            LOGGER.warn("Zone id or Username not found");
            return;
        }

        LocalTime time = ZonedDateTime.now(ZoneId.of(zoneId)).toLocalTime();
        String name = String.format(username, time.format(TIME_FORMATTER));
        guild.retrieveMemberById(userId)
                .flatMap(m -> m.modifyNickname(name))
                .queue(null, err -> LOGGER.error("Could not complete operation", err));
    }
}