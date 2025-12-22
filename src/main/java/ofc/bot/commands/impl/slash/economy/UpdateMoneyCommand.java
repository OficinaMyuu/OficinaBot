package ofc.bot.commands.impl.slash.economy;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
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

import java.util.List;

@DiscordCommand(name = "update-money", permissions = Permission.MANAGE_SERVER)
public class UpdateMoneyCommand extends SlashCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(UpdateMoneyCommand.class);
    private final UserEconomyRepository ecoRepo;

    public UpdateMoneyCommand(UserEconomyRepository ecoRepo) {
        this.ecoRepo = ecoRepo;
    }

    @Override
    public InteractionResult onCommand(@NotNull SlashCommandContext ctx) {
        User issuer = ctx.getUser();
        User target = ctx.getOption("target", issuer, OptionMapping::getAsUser);
        int bank = ctx.getSafeOption("bank", OptionMapping::getAsInt);
        int wallet = ctx.getSafeOption("cash", OptionMapping::getAsInt);
        long issuerId = ctx.getUserId();
        long targetId = target.getIdLong();

        if (bank == 0 && wallet == 0)
            return Status.NOTHING_CHANGED_WITH_REASON.args("`$0` foi fornecido em ambos os argumentos");

        try {
            UserEconomy eco = ecoRepo.findByUserId(targetId, UserEconomy.fromUserId(targetId));
            long walletResult = eco.getWallet() + (long) wallet;
            long bankResult = eco.getBank() + (long) bank;

            if (walletResult < 0)
                return Status.WALLET_CANNOT_BE_NEGATIVE;

            if (bankResult > Integer.MAX_VALUE || walletResult > Integer.MAX_VALUE)
                return Status.USER_CANNOT_RECEIVE_GIVEN_AMOUNT;

            eco.modifyBalance(wallet, bank).tickUpdate();
            ecoRepo.upsert(eco);

            long total = eco.getTotal();
            dispatchBalanceUpdateEvent(issuerId, targetId, total);
            return Status.ECONOMY_SUCCESSFULLY_UPDATED_BALANCE.args(target.getAsMention(), Bot.fmtNum(total));
        } catch (DataAccessException e) {
            LOGGER.error("Could not access balance of '{}'", targetId, e);
            return Status.COULD_NOT_EXECUTE_SUCH_OPERATION;
        }
    }

    @NotNull
    @Override
    public String getDescription() {
        return "ATUALIZA o saldo do usuário fornecido com a quantia fornecida, valores negativos removerão dinheiro.";
    }

    @NotNull
    @Override
    public List<OptionData> getOptions() {
        return List.of(
                new OptionData(OptionType.INTEGER, "cash", "A quantia a ser atualizada no cash.", true)
                        .setRequiredRange(Integer.MIN_VALUE, Integer.MAX_VALUE),

                new OptionData(OptionType.INTEGER, "bank", "A quantia a ser atualizada no bank.", true)
                        .setRequiredRange(Integer.MIN_VALUE, Integer.MAX_VALUE),

                new OptionData(OptionType.USER, "target", "O alvo a atualizar o saldo.")
        );
    }

    private void dispatchBalanceUpdateEvent(long userId, long targetId, long amount) {
        BankTransaction tr = new BankTransaction(userId, targetId, amount, CurrencyType.OFICINA, TransactionType.BALANCE_UPDATED);
        EventBus.dispatchEvent(new BankTransactionEvent(tr));
    }
}