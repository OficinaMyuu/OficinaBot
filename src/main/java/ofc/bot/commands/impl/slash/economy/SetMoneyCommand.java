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

@DiscordCommand(name = "set-money", permissions = Permission.MANAGE_SERVER)
public class SetMoneyCommand extends SlashCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(SetMoneyCommand.class);
    private final UserEconomyRepository ecoRepo;

    public SetMoneyCommand(UserEconomyRepository ecoRepo) {
        this.ecoRepo = ecoRepo;
    }

    @Override
    public InteractionResult onCommand(@NotNull SlashCommandContext ctx) {
        User issuer = ctx.getUser();
        User target = ctx.getOption("target", issuer, OptionMapping::getAsUser);
        int wallet = ctx.getSafeOption("cash", OptionMapping::getAsInt);
        int bank = ctx.getSafeOption("bank", OptionMapping::getAsInt);
        long issuerId = ctx.getUserId();
        long targetId = target.getIdLong();

        try {
            UserEconomy eco = ecoRepo.findByUserId(targetId, UserEconomy.fromUserId(targetId))
                    .setWallet(wallet)
                    .setBank(bank)
                    .tickUpdate();

            long total = eco.getTotal();
            ecoRepo.upsert(eco);
            dispatchBalanceSetEvent(issuerId, targetId, total);

            return Status.BALANCE_SET_SUCCESSFULLY.args(target.getAsMention(), Bot.fmtNum(total));
        } catch (DataAccessException e) {
            LOGGER.error("Could not access balance of '{}'", targetId, e);
            return Status.COULD_NOT_EXECUTE_SUCH_OPERATION;
        }
    }

    @NotNull
    @Override
    public String getDescription() {
        return "DEFINE um novo saldo para um usuário.";
    }

    @NotNull
    @Override
    public List<OptionData> getOptions() {
        return List.of(
                new OptionData(OptionType.INTEGER, "cash", "A quantia a ser definida no cash.", true)
                        .setRequiredRange(Integer.MIN_VALUE, Integer.MAX_VALUE),

                new OptionData(OptionType.INTEGER, "bank", "A quantia a ser definida no bank.", true)
                        .setRequiredRange(Integer.MIN_VALUE, Integer.MAX_VALUE),

                new OptionData(OptionType.USER, "target", "O usuário a definir o saldo.")
        );
    }

    private void dispatchBalanceSetEvent(long userId, long targetId, long amount) {
        BankTransaction tr = new BankTransaction(userId, targetId, amount, CurrencyType.OFICINA, TransactionType.BALANCE_SET);
        EventBus.dispatchEvent(new BankTransactionEvent(tr));
    }
}