package ofc.bot.jobs;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.exceptions.ErrorHandler;
import net.dv8tion.jda.api.requests.ErrorResponse;
import ofc.bot.Main;
import ofc.bot.domain.entity.Reminder;
import ofc.bot.domain.sqlite.repository.ReminderRepository;
import ofc.bot.domain.sqlite.repository.Repositories;
import ofc.bot.util.Bot;
import ofc.bot.util.content.annotations.jobs.CronJob;
import ofc.bot.util.embeds.EmbedFactory;
import org.jooq.exception.DataAccessException;
import org.quartz.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Date;
import java.util.List;

@CronJob(expression = "0 * * ? * * *") // Minutely
public class RemindersHandler implements Job {
    private static final Logger LOGGER = LoggerFactory.getLogger(RemindersHandler.class);
    private final ReminderRepository remRepo;
    private final JDA api;

    public RemindersHandler() {
        this.remRepo = Repositories.getReminderRepository();
        this.api = Main.getApi();
    }

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        List<Reminder> reminders = remRepo.findAllActive();
        long now = Bot.unixNow();

        for (Reminder rem : reminders) {
            switch (rem.getType()) {
                case AT -> handleAt(rem, now);
                case PERIOD -> handlePeriod(rem, now);
                case CRON -> handleCron(rem, now);
            }
        }
    }

    private void handleAt(Reminder rem, long now) {
        Long epoch = rem.getReminderValue();
        if (epoch == null) {
            throw new IllegalStateException("Received AT reminder with no epoch value");
        }

        if (epoch <= now) {
            trigger(rem);
            markAsExpired(rem);
        }
    }

    private void handlePeriod(Reminder rem, long now) {
        // Just a safeguard for reminders that may be never marked as expired
        if (rem.getTriggersLeft() <= 0) {
            markAsExpired(rem);
            return;
        }

        /*
         * We need to determine the correct "anchor time" to calculate when the reminder should trigger.
         * As `getLastTimeTriggered()` is initially stored as `0` in the database
         * when the reminder is created (as it has never been triggered before).
         * If we only relied on `getLastTimeTriggered()`, the reminder would trigger in the next minute
         * (since `0` would be treated as 01/01/1970 at 00:00 lol).
         */
        long lastTrigger = rem.getLastTimeTriggered();
        long anchorTime = lastTrigger == 0 ? rem.getTimeCreated() : lastTrigger;

        if (anchorTime + rem.getReminderValue() <= now) {
            trigger(rem);
            updatePeriodicReminder(rem);
        }
    }

    private void handleCron(Reminder rem, long now) {
        try {
            long lastTriggerEpoch = rem.getLastTimeTriggered();
            long anchorTime = lastTriggerEpoch == 0 ? rem.getTimeCreated() : lastTriggerEpoch;
            CronExpression exp = new CronExpression(rem.getExpression());
            Date lastTrigger = Date.from(Instant.ofEpochSecond(anchorTime));
            Date nextTrigger = exp.getNextValidTimeAfter(lastTrigger);

            if (nextTrigger != null && nextTrigger.toInstant().getEpochSecond() <= now) {
                trigger(rem);
                updateCronReminder(rem);
            }
        } catch (Exception e) {
            LOGGER.error("Failed to parse cron expression at reminder ID {}: {}", rem.getId(), rem.getExpression(), e);
            markAsExpired(rem);
        }
    }

    private void trigger(Reminder rem) {
        long chanId = rem.getChannelId();
        long userId = rem.getUserId();
        ChannelType chanType = rem.getChannelType();

        if (chanType == ChannelType.PRIVATE) {
            api.openPrivateChannelById(userId).queue(chan -> runTriggerLogic(chan, rem.getMessage(), userId, rem.getTimeCreated()));
            return;
        }

        MessageChannel channel = (MessageChannel) api.getChannelById(chanType.getInterface(), chanId);
        if (channel != null) {
            runTriggerLogic(channel, rem.getMessage(), userId, rem.getTimeCreated());
        }
    }

    private void runTriggerLogic(MessageChannel chan, String message, long userId, long timeCreated) {
        MessageEmbed embed = EmbedFactory.embedReminderTrigger(message, timeCreated);
        boolean isDm = chan.getType() == ChannelType.PRIVATE;

        chan.sendMessageEmbeds(embed)
                .setContent(isDm ? null : "<@" + userId + ">")
                .queue(null, new ErrorHandler().ignore(ErrorResponse.UNKNOWN_CHANNEL, ErrorResponse.CANNOT_SEND_TO_USER));
    }

    private void markAsExpired(Reminder rem) {
        try {
            rem.setExpired(true).tickUpdate();
            remRepo.upsert(rem);
        } catch (DataAccessException e) {
            LOGGER.error("Failed to mark reminder {} as expired", rem.getId(), e);
        }
    }

    private void updatePeriodicReminder(Reminder rem) {
        int triggersLeft = rem.getTriggersLeft();

        // Update the amount of repeats left
        if (triggersLeft > 0) {
            rem.setTriggersLeft(triggersLeft - 1);
        }

        // Checks if we should set this reminder as expired
        if (rem.getTriggersLeft() <= 0) {
            rem.setExpired(true);
        }

        rem.setLastTimeTriggered(Bot.unixNow()).tickUpdate();
        remRepo.upsert(rem);
    }

    private void updateCronReminder(Reminder rem) {
        rem.setLastTimeTriggered(Bot.unixNow()).tickUpdate();
        remRepo.upsert(rem);
    }
}