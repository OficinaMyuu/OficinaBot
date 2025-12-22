package ofc.bot.listeners.discord.interactions.buttons;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import ofc.bot.Main;
import ofc.bot.commands.impl.slash.economy.WorkCommand;
import ofc.bot.domain.entity.UserEconomy;
import ofc.bot.domain.sqlite.repository.Repositories;
import ofc.bot.domain.sqlite.repository.UserEconomyRepository;
import ofc.bot.util.Bot;
import ofc.bot.util.content.annotations.listeners.DiscordEventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * This class is a stateless handler that allows users to set a reminder to use the
 * {@code /work} command again, once they hit its cooldown.
 */
@DiscordEventHandler
public class WorkReminderHandler extends ListenerAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(WorkReminderHandler.class);
    private static final String IDENTIFIER = "WORK_REMINDER";
    // Mapping <UserID, ReminderData>
    private static final Map<Long, ReminderData> reminders = new HashMap<>();

    @Override
    public void onButtonInteraction(ButtonInteractionEvent e) {
        if (!IDENTIFIER.equals(e.getComponentId())) return;

        User user = e.getUser();
        long now = Bot.unixNow();
        long userId = user.getIdLong();
        long channelId = e.getChannelIdLong();
        ReminderData data = reminders.get(userId);

        if (data != null && data.futureSeconds > now) {
            e.reply("Você já definiu um lembrete para este comando.").setEphemeral(true).queue();
            return;
        }

        long nextWork = getNextWork(userId);

        // This situation can only occur if the user waits for the cooldown to end
        // without closing the ephemeral warning, and repeatedly clicks the reminder
        // button. This happens after the work command becomes available again.
        if (nextWork <= now) {
            e.reply("Você já pode usar este comando novamente.").setEphemeral(true).queue();
            return;
        }

        ReminderData newData = reminders.getOrDefault(userId, new ReminderData(nextWork, channelId));
        Duration period = Duration.ofSeconds(newData.getPeriod());

        reminders.put(userId, newData);
        sendReminder(userId, newData);

        LOGGER.info("User @{} [{}] will be reminded about their /work cooldown in {}", user.getName(), userId, period);
        e.replyFormat("Você será lembrado <t:%d:R> para usar este comando novamente.", nextWork)
                .setEphemeral(true)
                .queue();
    }

    private void sendReminder(long userId, ReminderData reminder) {
        JDA api = Main.getApi();
        String msg = "🔔 Lembrete! Você já pode usar o comando `/work` novamente no chat <#" + reminder.channelId + ">.";

        api.openPrivateChannelById(userId)
                .flatMap(chan -> chan.sendMessage(msg))
                .queueAfter(reminder.getPeriod(), TimeUnit.SECONDS,
                        (s) -> reminders.remove(userId),
                        (t) -> reminders.remove(userId)
                );
    }

    /*
     * Returns the next moment where the user will be able to work again,
     * or 0 if the user has never run the work command before.
     */
    private long getNextWork(long userId) {
        UserEconomyRepository ecoRepo = Repositories.getUserEconomyRepository();
        UserEconomy econ = ecoRepo.findByUserId(userId);
        long lastWork = econ.getLastWorkAt();

        return lastWork + WorkCommand.COOLDOWN;
    }

    private record ReminderData(
            long futureSeconds,
            long channelId
    ) {
        public long getPeriod() {
            return this.futureSeconds - Bot.unixNow();
        }
    }
}