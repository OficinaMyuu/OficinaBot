package ofc.bot.jobs;

import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import ofc.bot.util.content.Channels;
import ofc.bot.util.content.annotations.jobs.CronJob;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;

@CronJob(expression = "0 0 0 1 JAN ? *")
public class HappyNewYearAnnouncement implements Job {
    private static final Logger LOGGER = LoggerFactory.getLogger(HappyNewYearAnnouncement.class);

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        TextChannel channel = Channels.GENERAL.textChannel();

        if (channel == null) {
            LOGGER.warn("No channel found for ID {}", Channels.GENERAL.fetchId());
            return;
        }

        int year = LocalDate.now().getYear();

        String message = """
                @everyone FELIZ %d!!! FELIZ ANO NOVOOO 🥳🥳🥳
                
                > https://youtu.be/kL44EeYxSm8
                """;

        channel.sendMessageFormat(message, year).queue();
    }
}