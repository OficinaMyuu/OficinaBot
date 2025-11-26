package ofc.bot.commands.slash.economy;

import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import ofc.bot.domain.entity.BankTransaction;
import ofc.bot.domain.entity.UserEconomy;
import ofc.bot.domain.entity.enums.TransactionType;
import ofc.bot.domain.sqlite.repository.UserEconomyRepository;
import ofc.bot.events.eventbus.EventBus;
import ofc.bot.events.impl.BankTransactionEvent;
import ofc.bot.handlers.economy.CurrencyType;
import ofc.bot.handlers.interactions.commands.contexts.impl.SlashCommandContext;
import ofc.bot.handlers.interactions.commands.responses.states.InteractionResult;
import ofc.bot.handlers.interactions.commands.responses.states.Status;
import ofc.bot.handlers.interactions.commands.slash.abstractions.SlashCommand;
import ofc.bot.util.Bot;
import ofc.bot.util.content.annotations.commands.DiscordCommand;
import org.jetbrains.annotations.NotNull;
import org.jooq.exception.DataAccessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;

@DiscordCommand(name = "work")
public class WorkCommand extends SlashCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(WorkCommand.class);
    private static final Random random = new Random();
    public static final long COOLDOWN = 2700; // Seconds
    private static final double BOOSTER_EXTRA_PERCENTAGE = 1.2;
    private static final int MIN = 50;
    private static final int MAX = 200;
    private final UserEconomyRepository ecoRepo;

    public WorkCommand(UserEconomyRepository ecoRepo) {
        this.ecoRepo = ecoRepo;
    }

    @Override
    public InteractionResult onCommand(@NotNull SlashCommandContext ctx) {
        Member sender = ctx.getIssuer();
        long userId = sender.getIdLong();
        long nextWorkAt = getNextWork(userId);
        long now = Bot.unixNow();

        if (nextWorkAt > now) {
            return ctx.create(true)
                    .setContentFormat("Você poderá trabalhar de novo <t:%s:R>.", nextWorkAt)
                    .setActionRows(getReminderButton())
                    .send(Status.WAIT_BEFORE_WORK_AGAIN);
        }

        try {
            boolean boosting = sender.isBoosting();
            int randomInt = random.nextInt(MIN, MAX + 1);
            int value = boosting
                    ? (int) (randomInt * BOOSTER_EXTRA_PERCENTAGE)
                    : randomInt;
            String prettyValue = Bot.fmtNum(value);
            String prettyDifference = Bot.fmtNum(value - randomInt);

            applyWork(userId, value);
            dispatchWorkExecutedEvent(userId, value);

            if (boosting)
                return Status.WORK_SUCCESSFUL_BOOSTING.args(prettyValue, prettyDifference);
            else
                return Status.WORK_SUCCESSFUL.args(prettyValue);
        } catch (DataAccessException e) {
            LOGGER.error("Could not perform work operation for user '{}'", userId, e);
            return Status.COULD_NOT_EXECUTE_SUCH_OPERATION;
        }
    }

    @NotNull
    @Override
    public String getDescription() {
        return "Trabalhe para ganhar dinheiro.";
    }

    private void applyWork(long userId, int value) {
        UserEconomy eco = ecoRepo.findByUserId(userId, UserEconomy.fromUserId(userId));
        long now = Bot.unixNow();

        eco.modifyBalance(value, 0)
                .setLastWorkAt(now)
                .setLastUpdated(now);
        ecoRepo.upsert(eco);
    }

    private Button getReminderButton() {
        return Button.secondary("WORK_REMINDER", "Lembrar-me")
                .withEmoji(Emoji.fromUnicode("🔔"));
    }

    private long getNextWork(long userId) {
        long lastWork = ecoRepo.fetchLastWorkByUserId(userId);
        return lastWork + COOLDOWN;
    }

    private void dispatchWorkExecutedEvent(long userId, int amount) {
        BankTransaction tr = new BankTransaction(userId, amount, CurrencyType.OFICINA, TransactionType.WORK_EXECUTED);
        EventBus.dispatchEvent(new BankTransactionEvent(tr));
    }
}