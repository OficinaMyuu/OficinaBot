package ofc.bot.commands.impl.slash.economy;

import net.dv8tion.jda.api.entities.Member;
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

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Random;

@DiscordCommand(name = "daily")
public class DailyCommand extends SlashCommand {
    private static final Random RANDOM = new Random();
    private static final Logger LOGGER = LoggerFactory.getLogger(DailyCommand.class);
    private static final double BOOSTER_EXTRA_PERCENTAGE = 1.2;
    private static final int MIN = 750;
    private static final int MAX = 2100;
    private final UserEconomyRepository ecoRepo;

    public DailyCommand(UserEconomyRepository ecoRepo) {
        this.ecoRepo = ecoRepo;
    }

    @Override
    public InteractionResult onCommand(@NotNull SlashCommandContext ctx) {
        Member sender = ctx.getIssuer();
        long userId = sender.getIdLong();
        boolean hasCollected = hasCollected(userId);

        if (hasCollected)
            return Status.DAILY_ALREADY_COLLECTED;

        try {
            boolean boosting = sender.isBoosting();
            int randomInt = RANDOM.nextInt(MIN, MAX + 1);
            int value = boosting
                    ? (int) (randomInt * BOOSTER_EXTRA_PERCENTAGE)
                    : randomInt;
            String prettyValue = Bot.fmtNum(value);
            String prettyDifference = Bot.fmtNum(value - randomInt);

            applyDaily(userId, value);
            dispatchDailyCollectEvent(userId, value);

            if (boosting)
                return Status.DAILY_SUCCESSFULLY_COLLECTED_BOOSTING.args(prettyValue, prettyDifference);
            else
                return Status.DAILY_SUCCESSFULLY_COLLECT.args(prettyValue);
        } catch (DataAccessException e) {
            LOGGER.error("Could not collect daily prize for user '{}'", userId, e);
            return Status.COULD_NOT_EXECUTE_SUCH_OPERATION;
        }
    }

    @NotNull
    @Override
    public String getDescription() {
        return "Colete a sua recompensa diária.";
    }

    private void applyDaily(long userId, int value) {
        UserEconomy eco = ecoRepo.findByUserId(userId, UserEconomy.fromUserId(userId));
        long now = Bot.unixNow();

        eco.modifyBalance(value, 0)
                .setLastDailyAt(now)
                .setLastUpdated(now);
        ecoRepo.upsert(eco);
    }

    private boolean hasCollected(long userId) {
        Instant now = Instant.now();
        Instant lastUsed = lastUsed(userId);

        if (lastUsed == null) return false;

        LocalDate nowDate = now.atZone(ZoneId.systemDefault()).toLocalDate();
        LocalDate lastUsedDate = lastUsed.atZone(ZoneId.systemDefault()).toLocalDate();
        return nowDate.equals(lastUsedDate);
    }

    private Instant lastUsed(long userId) {
        long lastDaily = ecoRepo.fetchLastDailyByUserId(userId);
        return lastDaily == 0
                ? null
                : Instant.ofEpochSecond(lastDaily);
    }

    private void dispatchDailyCollectEvent(long userId, int amount) {
        BankTransaction tr = new BankTransaction(userId, amount, CurrencyType.OFICINA, TransactionType.DAILY_COLLECTED);
        EventBus.dispatchEvent(new BankTransactionEvent(tr));
    }
}