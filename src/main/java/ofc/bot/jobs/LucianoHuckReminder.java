package ofc.bot.jobs;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import ofc.bot.Main;
import ofc.bot.util.content.Channels;
import ofc.bot.util.content.annotations.jobs.CronJob;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@CronJob(expression = "0 0 0 ? * * *") // Every day at midnight
public class LucianoHuckReminder implements Job {
    private static final LocalDate TARGET_DATE = LocalDate.of(2025, 12, 24);

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        long chanId = Channels.GENERAL.fetchIdLong();
        JDA api = Main.getApi();
        TextChannel chan = api.getTextChannelById(chanId);

        if (chan == null) return;

        int periodDays = getPeriodDays();
        if (periodDays < 0) return;

        if (periodDays == 0) {
            chan.sendMessage("<@425093903189016577> <@596939790532739075> É HOJE A FOTO DO PERU DO LUCIANO HUCK")
                    .queue();
            return;
        }

        if (periodDays == 1) {
            chan.sendMessage("<@425093903189016577> <@596939790532739075> falta exatamente " +
                            "1 dia para o Luciano Huck postar uma foto do peru dele")
                    .queue();
        } else {
            chan.sendMessageFormat("<@425093903189016577> <@596939790532739075> faltam exatamente " +
                            "%d dias para o Luciano Huck postar uma foto do peru dele", periodDays)
                    .queue();
        }
    }

    private int getPeriodDays() {
        LocalDate now = LocalDate.now();
        return (int) now.until(TARGET_DATE, ChronoUnit.DAYS);
    }
}