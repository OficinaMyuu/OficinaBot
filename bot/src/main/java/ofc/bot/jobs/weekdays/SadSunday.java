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
import java.util.Random;
import java.util.concurrent.TimeUnit;

@CronJob(expression = "0 0 18 ? * SUN *") // Every Sunday at 6:00 PM
public class SadSunday implements Job {
    private static final Logger LOGGER = LoggerFactory.getLogger(SadSunday.class);
    private static final File SAD_SUNDAY_IMAGE = new File(BotFiles.DIR_ASSETS, "sunday.jpg");
    private static final Random RANDOM = new Random();
    private static final int MAX_SEND_AFTER = (60 * 2) + 30; // Up to 2.5 hours (in minutes)

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        TextChannel channel = Channels.GENERAL.textChannel();
        if (channel == null) {
            LOGGER.warn("Could not send Sad Sunday image because the channel for id {} was not found", Channels.GENERAL.fetchId());
            return;
        }

        int sendAfterMinutes = RANDOM.nextInt(MAX_SEND_AFTER);

        LOGGER.info("Sad Sunday image will be sent in {} minutes", sendAfterMinutes);

        channel.sendFiles(FileUpload.fromData(SAD_SUNDAY_IMAGE)).
                queueAfter(sendAfterMinutes, TimeUnit.MINUTES);
    }
}