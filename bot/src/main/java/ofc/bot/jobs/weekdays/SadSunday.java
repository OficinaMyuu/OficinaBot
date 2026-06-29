package ofc.bot.jobs.weekdays;

import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import ofc.bot.util.content.annotations.jobs.CronJob;
import ofc.bot.util.content.Channels;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;
import java.util.concurrent.TimeUnit;

@CronJob(expression = "0 0 18 ? * SUN *") // Every Sunday at 6:00 PM
public class SadSunday implements Job {
    private static final Logger LOGGER = LoggerFactory.getLogger(SadSunday.class);
    private static final String SAD_SUNDAY_URL = System.getenv("SAD_SUNDAY_URL");
    private static final Random RANDOM = new Random();
    private static final int MAX_SEND_AFTER = (60 * 2) + 30; // Up to 2.5 hours (in minutes)

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        TextChannel channel = Channels.GENERAL.textChannel();
        if (channel == null) {
            LOGGER.warn("Could not send Sad Sunday image because the channel for id {} was not found", Channels.GENERAL.fetchId());
            return;
        }

        if (SAD_SUNDAY_URL == null || SAD_SUNDAY_URL.isBlank()) {
            LOGGER.warn("Could not send Sad Sunday image because SAD_SUNDAY_URL is not configured");
            return;
        }

        int sendAfterMinutes = RANDOM.nextInt(MAX_SEND_AFTER);

        LOGGER.info("Sad Sunday image will be sent in {} minutes", sendAfterMinutes);

        channel.sendMessage(SAD_SUNDAY_URL).queueAfter(sendAfterMinutes, TimeUnit.MINUTES);
    }
}
