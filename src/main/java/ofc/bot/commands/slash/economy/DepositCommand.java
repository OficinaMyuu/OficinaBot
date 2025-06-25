package ofc.bot.commands.slash.economy;

import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import ofc.bot.domain.entity.UserEconomy;
import ofc.bot.domain.sqlite.repository.UserEconomyRepository;
import ofc.bot.handlers.games.betting.BetManager;
import ofc.bot.handlers.interactions.commands.contexts.impl.SlashCommandContext;
import ofc.bot.handlers.interactions.commands.responses.states.InteractionResult;
import ofc.bot.handlers.interactions.commands.responses.states.Status;
import ofc.bot.handlers.interactions.commands.slash.abstractions.SlashCommand;
import ofc.bot.util.Bot;
import ofc.bot.util.content.annotations.commands.DiscordCommand;
import ofc.bot.util.embeds.EmbedFactory;
import org.jetbrains.annotations.NotNull;
import org.jooq.exception.DataAccessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@DiscordCommand(name = "deposit")
public class DepositCommand extends SlashCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(DepositCommand.class);
    private static final BetManager betManager = BetManager.getManager();
    private final UserEconomyRepository ecoRepo;

    public DepositCommand(UserEconomyRepository ecoRepo) {
        this.ecoRepo = ecoRepo;
    }

    @Override
    public InteractionResult onCommand(@NotNull SlashCommandContext ctx) {
        User user = ctx.getUser();
        long userId = user.getIdLong();
        UserEconomy userEco = ecoRepo.findByUserId(userId, UserEconomy.fromUserId(userId));
        String amountInput = ctx.getOption("amount", "all", OptionMapping::getAsString);
        int wallet = userEco.getWallet();

        if (betManager.isBetting(userId))
            return Status.YOU_CANNOT_DO_THIS_WHILE_BETTING;

        int amount = Bot.parseAmount(amountInput, wallet);
        if (amount < 0)
            return Status.INVALID_VALUE_PROVIDED.args(amountInput);

        if (wallet <= 0) {
            MessageEmbed embed = embedEmptyWallet(user);
            return ctx.replyEmbeds(Status.YOU_GOT_NO_MONEY_TO_DEPOSIT, embed);
        }

        if (amount > wallet) {
            MessageEmbed embed = embedInsfficient(user, wallet);
            return ctx.replyEmbeds(Status.YOU_GOT_NO_MONEY_TO_DEPOSIT, embed);
        }

        try {
            userEco.modifyBalance(-amount, amount).tickUpdate();
            ecoRepo.upsert(userEco);

            MessageEmbed embed = embedSuccess(user, amount);
            return ctx.replyEmbeds(embed);
        } catch (DataAccessException e) {
            LOGGER.error("Could not deposit {} of user user {}", amount, userEco, e);
            return Status.COULD_NOT_EXECUTE_SUCH_OPERATION;
        }
    }

    @NotNull
    @Override
    public String getDescription() {
        return "Deposite dinheiro na sua conta bancária.";
    }

    @NotNull
    @Override
    public List<OptionData> getOptions() {
        return List.of(
                new OptionData(OptionType.STRING, "amount", "A quantia a ser depositada (forneça \"all\" sem aspas para depositar tudo).")
        );
    }

    private MessageEmbed embedEmptyWallet(User user) {
        return EmbedFactory.embedBankAction(user, EmbedFactory.DANGER_RED,
                "❌ Você não tem nada na carteira para ser depositado.");
    }

    private MessageEmbed embedInsfficient(User user, int wallet) {
        return EmbedFactory.embedBankAction(user, EmbedFactory.DANGER_RED,
                "❌ Você não tem essa quantia para depositar. Você tem $%s na carteira agora.", Bot.fmtNum(wallet));
    }

    private MessageEmbed embedSuccess(User user, int amount) {
        return EmbedFactory.embedBankAction(user, EmbedFactory.OK_GREEN, "✅ Depositou $%s no seu banco!", Bot.fmtNum(amount));
    }
}