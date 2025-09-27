package ofc.bot.commands.slash.economy;

import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import ofc.bot.domain.entity.BankTransaction;
import ofc.bot.domain.entity.enums.TransactionType;
import ofc.bot.domain.sqlite.repository.UserEconomyRepository;
import ofc.bot.events.eventbus.EventBus;
import ofc.bot.events.impl.BankTransactionEvent;
import ofc.bot.handlers.economy.CurrencyType;
import ofc.bot.handlers.games.betting.BetManager;
import ofc.bot.handlers.interactions.commands.Cooldown;
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

import java.util.List;
import java.util.concurrent.TimeUnit;

@DiscordCommand(name = "pay")
public class PayCommand extends SlashCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(PayCommand.class);
    private static final BetManager betManager = BetManager.getManager();
    private static final float TAX_PER_EXECUTION = 1f; // 0%
    private static final int TAX_THRESHOLD = 10;
    private final UserEconomyRepository ecoRepo;

    public PayCommand(UserEconomyRepository ecoRepo) {
        this.ecoRepo = ecoRepo;
    }

    @Override
    public InteractionResult onCommand(@NotNull SlashCommandContext ctx) {
        User target = ctx.getSafeOption("user", OptionMapping::getAsUser);
        String amountInput = ctx.getSafeOption("amount", OptionMapping::getAsString);
        long issuerId = ctx.getUserId();

        if (betManager.isBetting(issuerId))
            return Status.YOU_CANNOT_DO_THIS_WHILE_BETTING;

        long targetId = target.getIdLong();
        int amountWallet = ecoRepo.fetchWalletByUserId(issuerId);
        int total = Bot.parseAmount(amountInput, amountWallet);
        int amountToSend = total >= TAX_THRESHOLD
                ? (int) (total * TAX_PER_EXECUTION)
                : total;

        if (total > amountWallet)
            return Status.INSUFFICIENT_BALANCE_VALUE.args(Bot.fmtNum(total - amountWallet));

        if (total <= 0)
            return Status.INVALID_VALUE_PROVIDED.args(amountInput);

        if (targetId == issuerId)
            return Status.CANNOT_TRANSFER_TO_YOURSELF;

        if (target.isBot())
            return Status.CANNOT_TRANSFER_TO_BOTS;

        try {
            ecoRepo.transferWallet(issuerId, targetId, amountToSend, total);

            dispatchSendMoneyEvent(issuerId, targetId, total);

            return Status.TRANSACTION_SUCCESSFUL.args(
                    Bot.fmtNum(amountToSend),
                    target.getAsMention()
            );
        } catch (DataAccessException e) {
            LOGGER.error("Could not complete transaction of '%{}' from '{}' to '{}'", amountInput, issuerId, targetId, e);
            return Status.COULD_NOT_EXECUTE_SUCH_OPERATION;
        }
    }

    @NotNull
    @Override
    public String getDescription() {
        return "Envie dinheiro para outro usuário.";
    }

    @NotNull
    @Override
    public Cooldown getCooldown() {
        return Cooldown.of(30, TimeUnit.SECONDS);
    }

    @NotNull
    @Override
    public List<OptionData> getOptions() {
        return List.of(
                new OptionData(OptionType.USER, "user", "O usuário para enviar o dinheiro.", true),
                new OptionData(OptionType.STRING, "amount", "A quantia a ser enviada (forneça \"all\" sem aspas para transferir tudo).", true)
        );
    }

    private void dispatchSendMoneyEvent(long senderId, long targetId, long amount) {
        BankTransaction tr = new BankTransaction(senderId, targetId, amount, CurrencyType.OFICINA, TransactionType.MONEY_TRANSFERRED);
        EventBus.dispatchEvent(new BankTransactionEvent(tr));
    }
}