package ofc.bot.jobs;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.channel.concrete.NewsChannel;
import ofc.bot.Main;
import ofc.bot.util.Bot;
import ofc.bot.util.content.annotations.jobs.CronJob;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.DayOfWeek;
import java.time.LocalDate;

@CronJob(expression = "0 0 14 ? * SAT *")
public class StaffFormReminder implements Job {
    private static final Logger LOGGER = LoggerFactory.getLogger(StaffFormReminder.class);

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        if (!shouldSendReminderOn(LocalDate.now()))
            return;

        JDA api = Main.getApi();
        String url = Bot.get("staff.form_url");
        Long chanId = Bot.get("staff.channels.announcement.id", Long::parseLong);

        if (url == null || chanId == null) {
            LOGGER.warn("Staff form URL or announcement channel ID were not found");
            return;
        }

        NewsChannel chan = api.getNewsChannelById(chanId);
        if (chan == null) {
            LOGGER.warn("Staff form channel with ID {} was not found", chanId);
            return;
        }

        chan.sendMessageFormat("**LEMBRETE DO FORMULÁRIO QUINZENAL**\n\n> %s\n\n@everyone", url)
                .queue();
    }

    private static boolean shouldSendReminderOn(LocalDate date) {
        return isSaturday(date) && isOddEpochWeek(date);
    }

    private static boolean isSaturday(LocalDate date) {
        return date.getDayOfWeek() == DayOfWeek.SATURDAY;
    }

    private static boolean isOddEpochWeek(LocalDate date) {
        return Bot.isOdd(Math.floorDiv(date.toEpochDay(), 7));
    }
}
