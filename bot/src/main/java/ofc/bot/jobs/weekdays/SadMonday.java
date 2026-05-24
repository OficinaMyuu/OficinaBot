package ofc.bot.jobs.weekdays;

import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.utils.FileUpload;
import ofc.bot.internal.data.BotFiles;
import ofc.bot.util.content.annotations.jobs.CronJob;
import ofc.bot.util.content.Channels;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

@CronJob(expression = "0 0 0 ? * MON *") // Every Monday at 12:00 AM
public class SadMonday implements Job {
    private static final Logger LOGGER = LoggerFactory.getLogger(SadMonday.class);
    private static final File SAD_MONDAY_IMAGE = new File(BotFiles.DIR_ASSETS, "monday.jpg");

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        TextChannel channel = Channels.GENERAL.textChannel();
        if (channel == null) {
            LOGGER.warn("Could not send Sad Monday image because no channels for the id {} were found", Channels.GENERAL.fetchId());
            return;
        }

        channel.sendFiles(
                FileUpload.fromData(SAD_MONDAY_IMAGE)
        ).queue();
    }
}