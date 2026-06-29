package ofc.bot.jobs.weekdays;

import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import ofc.bot.util.content.annotations.jobs.CronJob;
import ofc.bot.util.content.Channels;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@CronJob(expression = "0 0 0 ? * MON *") // Every Monday at 12:00 AM
public class SadMonday implements Job {
    private static final Logger LOGGER = LoggerFactory.getLogger(SadMonday.class);
    private static final String SAD_MONDAY_URL = System.getenv("SAD_MONDAY_URL");

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        TextChannel channel = Channels.GENERAL.textChannel();
        if (channel == null) {
            LOGGER.warn("Could not send Sad Monday image because no channels for the id {} were found", Channels.GENERAL.fetchId());
            return;
        }

        if (SAD_MONDAY_URL == null || SAD_MONDAY_URL.isBlank()) {
            LOGGER.warn("Could not send Sad Monday image because SAD_MONDAY_URL is not configured");
            return;
        }

        channel.sendMessage(SAD_MONDAY_URL).queue();
    }
}
