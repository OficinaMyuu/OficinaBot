package ofc.bot.commands.impl.slash.bets;

import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.AutoCompleteQuery;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import ofc.bot.domain.entity.UserEconomy;
import ofc.bot.domain.sqlite.repository.BetGameRepository;
import ofc.bot.domain.sqlite.repository.GameParticipantRepository;
import ofc.bot.domain.sqlite.repository.UserEconomyRepository;
import ofc.bot.handlers.games.betting.roulette.*;
import ofc.bot.handlers.interactions.commands.contexts.impl.SlashCommandContext;
import ofc.bot.handlers.interactions.commands.responses.states.InteractionResult;
import ofc.bot.handlers.interactions.commands.responses.states.Status;
import ofc.bot.handlers.interactions.commands.slash.abstractions.SlashSubcommand;
import ofc.bot.util.Bot;
import ofc.bot.util.content.annotations.commands.DiscordCommand;
import ofc.bot.util.content.annotations.listeners.DiscordEventHandler;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@DiscordCommand(name = "bets roulette")
public class BetRouletteCommand extends SlashSubcommand {
    private static final RouletteGameManager ROULETTE_MANAGER = RouletteGameManager.getManager();

    private final UserEconomyRepository ecoRepo;
    private final BetGameRepository betRepo;
    private final GameParticipantRepository participantRepo;

    public BetRouletteCommand(
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
        String rawBet = ctx.getSafeOption("bet", OptionMapping::getAsString);
        String rawAmount = ctx.getSafeOption("amount", OptionMapping::getAsString);
        RouletteBet bet = RouletteBet.parse(rawBet).orElse(null);
        UserEconomy eco = ecoRepo.findByUserId(ctx.getUserId());
        int bank = eco == null ? 0 : eco.getBank();
        int amount = parseBetAmount(rawAmount, bank);

        if (bet == null) {
            return Status.INVALID_VALUE_PROVIDED.args(rawBet);
        }

        if (amount < 0) {
            return Status.INVALID_VALUE_PROVIDED.args(rawAmount);
        }

        RouletteGame game = ROULETTE_MANAGER.getOrCreate(ctx.getChannelId(), ecoRepo, betRepo, participantRepo);
        RoulettePlacementResult result = game.placeBet(ctx.getUserId(), bet, amount);

        if (result instanceof RoulettePlacementResult.Rejected rejected) {
            ROULETTE_MANAGER.removeIfEmpty(ctx.getChannelId(), game);
            return handleRejected(ctx, rejected.reason(), amount);
        }

        RoulettePlacementResult.Accepted accepted = (RoulettePlacementResult.Accepted) result;
        if (accepted.firstBetInGame()) {
            return ctx.create()
                    .setEmbeds(RouletteMessageFactory.lobby(game.snapshot()))
                    .onSend(
                            hook -> hook.retrieveOriginal().queue(game::start, err -> game.cancelAndRefund()),
                            err -> game.cancelAndRefund()
                    )
                    .send();
        }

        game.refreshLobbyMessage();
        return ctx.create(true)
                .setContentFormat(
                        "Aposta aceita: `%s` em `%s`.",
                        Bot.fmtMoney(amount),
                        bet.canonicalName()
                )
                .send();
    }

    @Override
    @NotNull
    public String getDescription() {
        return "Aposte em uma rodada de roleta.";
    }

    @Override
    @NotNull
    public List<OptionData> getOptions() {
        return List.of(
                new OptionData(OptionType.STRING, "bet", "Espaço da roleta: odd, 3rd, 13-24, 16, etc.", true, true),
                new OptionData(OptionType.STRING, "amount", "A quantia a ser apostada (ex.: all, 2k, 5000).", true)
        );
    }

    static int parseBetAmount(String input, int bank) {
        int amount = Bot.parseAmount(input, bank);
        if (amount < RouletteGame.MIN_AMOUNT || amount > RouletteGame.MAX_AMOUNT) {
            return -1;
        }
        return amount;
    }

    private InteractionResult handleRejected(SlashCommandContext ctx, RoulettePlacementResult.Reason reason, int amount) {
        return switch (reason) {
            case GAME_CLOSED -> ctx.reply("Esta roleta já está encerrando. Tente novamente em alguns segundos.", true);
            case ACTIVE_OTHER_GAME -> Status.YOU_CANNOT_DO_THIS_WHILE_BETTING;
            case INSUFFICIENT_BALANCE -> Status.INSUFFICIENT_BALANCE;
            case INVALID_AMOUNT -> Status.INVALID_VALUE_PROVIDED.args(amount);
            case PAYOUT_LIMIT -> Status.USER_CANNOT_RECEIVE_GIVEN_AMOUNT;
        };
    }

    @DiscordEventHandler
    public static class RouletteBetAutocompletion extends ListenerAdapter {
        @Override
        public void onCommandAutoCompleteInteraction(CommandAutoCompleteInteractionEvent e) {
            if (!e.getFullCommandName().equals("bets roulette")) return;

            AutoCompleteQuery focused = e.getFocusedOption();
            if (!focused.getName().equals("bet")) return;

            List<Command.Choice> choices = RouletteBet.suggestions(focused.getValue())
                    .stream()
                    .map(value -> {
                        RouletteBet bet = RouletteBet.parse(value).orElseThrow();
                        String name = value + " (x" + bet.multiplier() + ")";
                        return new Command.Choice(name, value);
                    })
                    .toList();

            e.replyChoices(choices).queue();
        }
    }
}
