package ofc.bot.commands.impl.slash.bets;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import ofc.bot.domain.entity.UserEconomy;
import ofc.bot.domain.sqlite.repository.BetGameRepository;
import ofc.bot.domain.sqlite.repository.GameParticipantRepository;
import ofc.bot.domain.sqlite.repository.UserEconomyRepository;
import ofc.bot.handlers.games.betting.BetManager;
import ofc.bot.handlers.games.betting.blackjack.BlackjackGame;
import ofc.bot.handlers.games.betting.blackjack.BlackjackPlayerView;
import ofc.bot.handlers.interactions.commands.contexts.impl.SlashCommandContext;
import ofc.bot.handlers.interactions.commands.responses.states.InteractionResult;
import ofc.bot.handlers.interactions.commands.responses.states.Status;
import ofc.bot.handlers.interactions.commands.slash.abstractions.SlashCommand;
import ofc.bot.util.Bot;
import ofc.bot.util.content.annotations.commands.DiscordCommand;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@DiscordCommand(name = "blackjack", permissions = Permission.ADMINISTRATOR)
public class BetBlackjackCommand extends SlashCommand {
    private static final BetManager BET_MANAGER = BetManager.getManager();

    private final UserEconomyRepository ecoRepo;
    private final BetGameRepository betRepo;
    private final GameParticipantRepository participantRepo;

    public BetBlackjackCommand(
            UserEconomyRepository ecoRepo,
            BetGameRepository betRepo,
            GameParticipantRepository participantRepo
    ) {
        this.ecoRepo = ecoRepo;
        this.betRepo = betRepo;
        this.participantRepo = participantRepo;
    }

    @Override
    public InteractionResult onCommand(@NotNull SlashCommandContext ctx) {
        long userId = ctx.getUserId();
        String rawAmount = ctx.getSafeOption("amount", OptionMapping::getAsString);
        UserEconomy eco = ecoRepo.findByUserId(userId);
        int bank = eco == null ? 0 : eco.getBank();
        int amount = parseBetAmount(rawAmount, bank);

        if (amount < 0) {
            return Status.INVALID_VALUE_PROVIDED.args(rawAmount);
        }
        if (BET_MANAGER.isBetting(userId)) {
            return Status.YOU_CANNOT_DO_THIS_WHILE_BETTING;
        }
        if (eco == null || eco.getBank() < amount) {
            return Status.INSUFFICIENT_BALANCE;
        }
        if (!canReceiveInitialWin(eco, amount)) {
            return Status.USER_CANNOT_RECEIVE_GIVEN_AMOUNT;
        }

        BlackjackGame game = new BlackjackGame(
                BlackjackPlayerView.from(ctx.getUser()),
                amount,
                ecoRepo,
                betRepo,
                participantRepo
        );

        try {
            BET_MANAGER.addSession(userId, game);
        } catch (IllegalArgumentException e) {
            return Status.YOU_CANNOT_DO_THIS_WHILE_BETTING;
        }

        try {
            reserveInitialStake(eco, amount);
        } catch (RuntimeException e) {
            BET_MANAGER.removeBet(userId);
            throw e;
        }

        BlackjackGame.MessageEmbedData message = game.initialMessage();
        if (game.isSettled()) {
            return ctx.create()
                    .setEmbeds(message.embed())
                    .setComponents(message.components())
                    .send();
        }

        return ctx.create()
                .setEmbeds(message.embed())
                .setComponents(message.components())
                .onSend(
                        hook -> hook.retrieveOriginal().queue(game::start, err -> game.cancelAndRefund()),
                        err -> game.cancelAndRefund()
                )
                .send();
    }

    @Override
    @NotNull
    public String getDescription() {
        return "Jogue blackjack contra a banca.";
    }

    @Override
    @NotNull
    public List<OptionData> getOptions() {
        return List.of(
                new OptionData(OptionType.STRING, "amount", "A quantia a ser apostada (ex.: all, 2k, 5000).", true)
        );
    }

    static int parseBetAmount(String input, int bank) {
        int amount = Bot.parseAmount(input, bank);
        if (amount < BlackjackGame.MIN_AMOUNT || amount > BlackjackGame.MAX_AMOUNT) {
            return -1;
        }
        return amount;
    }

    static boolean canReceiveInitialWin(UserEconomy eco, int amount) {
        long bankAfterStake = (long) eco.getBank() - amount;
        long maxCredit = amount * 2L;
        return bankAfterStake + maxCredit <= Integer.MAX_VALUE;
    }

    void reserveInitialStake(UserEconomy eco, int amount) {
        eco.modifyBalance(0, -amount).tickUpdate();
        ecoRepo.upsert(eco);
    }
}
