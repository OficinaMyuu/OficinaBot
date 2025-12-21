package ofc.bot.commands.impl.slash.economy;

import net.dv8tion.jda.api.entities.MessageEmbed;
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
import ofc.bot.handlers.games.betting.BetManager;
import ofc.bot.handlers.interactions.commands.Cooldown;
import ofc.bot.handlers.interactions.commands.contexts.impl.SlashCommandContext;
import ofc.bot.handlers.interactions.commands.responses.states.InteractionResult;
import ofc.bot.handlers.interactions.commands.responses.states.Status;
import ofc.bot.handlers.interactions.commands.slash.abstractions.SlashCommand;
import ofc.bot.util.Bot;
import ofc.bot.util.OficinaEmbed;
import ofc.bot.util.content.annotations.commands.DiscordCommand;
import ofc.bot.util.embeds.EmbedFactory;
import org.jetbrains.annotations.NotNull;
import org.jooq.exception.DataAccessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;

@DiscordCommand(name = "rob")
public class RobCommand extends SlashCommand {
    private static final BiFunction<Double, Double, Double> FAIL_PROB_ALGORITHM = (networth, wallet) -> networth / (wallet + networth);
    private static final Logger LOGGER = LoggerFactory.getLogger(RobCommand.class);
    private static final BetManager betManager = BetManager.getManager();
    private static final float MIN_FINE = 0.15f;
    private static final float MAX_FINE = 0.3f;
    private static final Random RANDOM = new Random();
    private final UserEconomyRepository ecoRepo;

    public RobCommand(UserEconomyRepository ecoRepo) {
        this.ecoRepo = ecoRepo;
    }

    @Override
    public InteractionResult onCommand(@NotNull SlashCommandContext ctx) {
        User target = ctx.getSafeOption("user", OptionMapping::getAsUser);
        User issuer = ctx.getUser();
        long targetId = target.getIdLong();
        long issuerId = issuer.getIdLong();

        if (betManager.isBetting(issuerId))
            return Status.YOU_CANNOT_DO_THIS_WHILE_BETTING;

        // Can you respectfully at least wait for the user to stop betting before trying to rob them?
        if (betManager.isBetting(targetId))
            return Status.MEMBER_IS_BETTING_PLEASE_WAIT;

        if (targetId == issuerId)
            return Status.YOU_CANNOT_ROB_YOURSELF;

        UserEconomy targetEco = ecoRepo.findByUserId(targetId, UserEconomy.fromUserId(targetId));
        if (targetEco.getWallet() <= 0)
            return Status.TARGET_WALLET_IS_EMPTY;

        UserEconomy issuerEco = ecoRepo.findByUserId(issuerId, UserEconomy.fromUserId(issuerId));
        double failProb = FAIL_PROB_ALGORITHM.apply((double) issuerEco.getTotal(), (double) targetEco.getWallet());
        double rand = RANDOM.nextDouble();
        float successProb = (float) (1 - failProb);
        boolean failed = failProb >= rand;

        try {
            if (failed) {
                float fineRate = RANDOM.nextFloat(MIN_FINE, MAX_FINE);
                int fineAmount = Math.clamp(Math.round((double) issuerEco.getTotal() * fineRate),
                        Integer.MIN_VALUE, Integer.MAX_VALUE);

                issuerEco.modifyBalance(0, -fineAmount).tickUpdate();
                ecoRepo.upsert(issuerEco);
                dispatchFineEvent(target, issuerId, fineAmount);

                MessageEmbed embed = embedFail(issuer, target, fineAmount);
                return ctx.replyEmbeds(Status.FAILED_TO_ROB_USER, embed);
            } else {
                int moneyStolen = (int) Math.ceil(successProb * targetEco.getWallet());

                // Here, we can just handle the operation as if it was a normal user-actioned transaction,
                // this way, we simplify the code and also uses ACID operations.
                ecoRepo.transferWallet(targetId, issuerId, moneyStolen);
                dispatchRobEvent(issuer, target, moneyStolen);

                MessageEmbed embed = embedSuccess(issuer, target, moneyStolen);
                return ctx.replyEmbeds(embed);
            }
        } catch (DataAccessException e) {
            LOGGER.error("Failed to save user {} robbing user {}", issuerId, targetId, e);
            return Status.COULD_NOT_EXECUTE_SUCH_OPERATION;
        }
    }

    @NotNull
    @Override
    public String getDescription() {
        return "Roube o dinheiro da carteira de um usuário.";
    }

    @NotNull
    @Override
    public Cooldown getCooldown() {
        return Cooldown.of(false, false, 4, TimeUnit.HOURS);
    }

    @NotNull
    @Override
    public List<OptionData> getOptions() {
        return List.of(
                new OptionData(OptionType.USER, "user", "O usuário a ser roubado.", true)
        );
    }

    private MessageEmbed embedSuccess(User issuer, User target, int amount) {
        OficinaEmbed builder = new OficinaEmbed();

        return builder
                .setAuthor(issuer.getName(), null, issuer.getEffectiveAvatarUrl())
                .setDescf("\uD83D\uDCB0 Você roubou $%s de %s.", Bot.fmtNum(amount), target.getName())
                .setColor(EmbedFactory.OK_GREEN)
                .build();
    }

    private MessageEmbed embedFail(User issuer, User target, int amount) {
        OficinaEmbed builder = new OficinaEmbed();

        return builder
                .setAuthor(issuer.getName(), null, issuer.getEffectiveAvatarUrl())
                .setDescf("\uD83D\uDC6E Você foi pego tentando roubar %s e foi multado em $%s.",
                        target.getName(), Bot.fmtNum(amount))
                .setColor(EmbedFactory.DANGER_RED)
                .build();
    }

    private void dispatchRobEvent(User robber, User victim, int amount) {
        String comment = String.format("%s roubou %s de %s", robber.getName(), Bot.fmtNum(amount), victim.getName());
        long robberId = robber.getIdLong();
        long victimId = victim.getIdLong();
        BankTransaction tr = new BankTransaction(robberId, victimId, comment, CurrencyType.OFICINA, TransactionType.AMOUNT_ROBBED);
        EventBus.dispatchEvent(new BankTransactionEvent(tr));
    }

    private void dispatchFineEvent(User victim, long userId, int fineAmount) {
        String comment = String.format("Tentou roubar %s", victim.getName());
        BankTransaction tr = new BankTransaction(userId, fineAmount, comment, CurrencyType.OFICINA, TransactionType.AMOUNT_FINED);
        EventBus.dispatchEvent(new BankTransactionEvent(tr));
    }
}