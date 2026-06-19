package ofc.bot.handlers.games.betting.blackjack;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.exceptions.ErrorHandler;
import net.dv8tion.jda.api.requests.ErrorResponse;
import ofc.bot.domain.entity.BetGame;
import ofc.bot.domain.entity.GameParticipant;
import ofc.bot.domain.entity.UserEconomy;
import ofc.bot.domain.sqlite.repository.BetGameRepository;
import ofc.bot.domain.sqlite.repository.GameParticipantRepository;
import ofc.bot.domain.sqlite.repository.UserEconomyRepository;
import ofc.bot.handlers.games.GameStatus;
import ofc.bot.handlers.games.GameType;
import ofc.bot.handlers.games.betting.BetManager;
import ofc.bot.handlers.interactions.buttons.contexts.ButtonClickContext;
import ofc.bot.handlers.interactions.commands.responses.states.InteractionResult;
import ofc.bot.handlers.interactions.commands.responses.states.Status;
import ofc.bot.util.Bot;
import ofc.bot.util.time.ElasticScheduler;
import org.jooq.exception.DataAccessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.TimeUnit;

public final class BlackjackGame {
    public static final int MIN_AMOUNT = 100;
    public static final int MAX_AMOUNT = 1_000_000;
    public static final int TIMEOUT_MILLIS = 5 * 60 * 1000;

    private static final Logger LOGGER = LoggerFactory.getLogger(BlackjackGame.class);
    private static final ErrorHandler IGNORE_UNKNOWN_MESSAGE = new ErrorHandler().ignore(ErrorResponse.UNKNOWN_MESSAGE);
    private static final BetManager BET_MANAGER = BetManager.getManager();

    private final long id;
    private final BlackjackPlayerView player;
    private final UserEconomyRepository ecoRepo;
    private final BetGameRepository betRepo;
    private final GameParticipantRepository participantRepo;
    private final BlackjackRound round;
    private final ElasticScheduler scheduler;

    private GameStatus status;
    private Message message;
    private long startedAt;
    private long endedAt;
    private boolean settledEconomy;

    public BlackjackGame(
            BlackjackPlayerView player,
            int amount,
            UserEconomyRepository ecoRepo,
            BetGameRepository betRepo,
            GameParticipantRepository participantRepo
    ) {
        this(player, amount, ecoRepo, betRepo, participantRepo, new BlackjackShoe());
    }

    BlackjackGame(
            BlackjackPlayerView player,
            int amount,
            UserEconomyRepository ecoRepo,
            BetGameRepository betRepo,
            GameParticipantRepository participantRepo,
            BlackjackShoe shoe
    ) {
        this.id = System.nanoTime();
        this.player = player;
        this.ecoRepo = ecoRepo;
        this.betRepo = betRepo;
        this.participantRepo = participantRepo;
        this.round = new BlackjackRound(amount, shoe);
        this.scheduler = new ElasticScheduler(this::timeout, TIMEOUT_MILLIS);
        this.status = round.isSettled() ? GameStatus.COMPLETE : GameStatus.RUNNING;
        this.startedAt = Bot.unixNow();
    }

    public long id() {
        return id;
    }

    public long userId() {
        return player.userId();
    }

    public BlackjackRound round() {
        return round;
    }

    public synchronized GameStatus status() {
        return status;
    }

    public synchronized boolean isSettled() {
        return round.isSettled();
    }

    public synchronized void start(Message message) {
        if (this.message != null || status != GameStatus.RUNNING) return;

        this.message = message;
        scheduler.start();
    }

    public synchronized MessageEmbedData initialMessage() {
        if (round.isSettled()) {
            settleEconomyAndPersist();
            return new MessageEmbedData(
                    BlackjackMessageFactory.result(player, round),
                    BlackjackComponentFactory.finished()
            );
        }
        return new MessageEmbedData(
                BlackjackMessageFactory.active(player, round),
                BlackjackComponentFactory.active(this)
        );
    }

    public synchronized boolean canHit() {
        return round.canHit();
    }

    public synchronized boolean canStand() {
        return round.canStand();
    }

    public synchronized boolean canDoubleDown() {
        return round.canDoubleDown() && canReserveExtraStake(round.requiredDoubleStake());
    }

    public synchronized boolean canSplit() {
        return round.canSplit() && canReserveExtraStake(round.requiredSplitStake());
    }

    public synchronized InteractionResult handle(BlackjackAction action, ButtonClickContext ctx) {
        InteractionResult result = apply(action);
        if (!result.isOk()) {
            refreshFrom(ctx);
            return result;
        }

        if (round.isSettled()) {
            editResult(ctx);
        } else {
            scheduler.reset();
            editActive(ctx);
        }
        return Status.OK;
    }

    synchronized InteractionResult apply(BlackjackAction action) {
        if (status != GameStatus.RUNNING || round.isSettled()) {
            return Status.OK;
        }
        try {
            switch (action) {
                case HIT -> round.hit();
                case STAND -> round.stand();
                case DOUBLE_DOWN -> {
                    if (!round.canDoubleDown()) {
                        return Status.YOU_CANNOT_RUN_THIS_COMMAND;
                    }
                    InteractionResult reserved = reserveExtraStake(round.requiredDoubleStake());
                    if (!reserved.isOk()) {
                        return reserved;
                    }
                    round.doubleDown();
                }
                case SPLIT -> {
                    if (!round.canSplit()) {
                        return Status.YOU_CANNOT_RUN_THIS_COMMAND;
                    }
                    InteractionResult reserved = reserveExtraStake(round.requiredSplitStake());
                    if (!reserved.isOk()) {
                        return reserved;
                    }
                    round.split();
                }
            }
        } catch (IllegalStateException e) {
            return Status.YOU_CANNOT_RUN_THIS_COMMAND;
        }

        if (round.isSettled()) {
            settleEconomyAndPersist();
        }
        return Status.OK;
    }

    public synchronized void timeout() {
        if (status != GameStatus.RUNNING || round.isSettled()) return;

        round.standAll();
        settleEconomyAndPersist();

        if (message != null) {
            message.editMessageEmbeds(BlackjackMessageFactory.result(player, round))
                    .setComponents(BlackjackComponentFactory.finished())
                    .setReplace(true)
                    .queue(null, IGNORE_UNKNOWN_MESSAGE);
        }
    }

    public synchronized void cancelAndRefund() {
        if (settledEconomy) return;

        status = GameStatus.INTERRUPTED;
        scheduler.shutdown();
        refund(round.totalStake());
        BET_MANAGER.removeBet(userId());
    }

    private void editActive(ButtonClickContext ctx) {
        ctx.create()
                .setEmbeds(BlackjackMessageFactory.active(player, round))
                .setComponents(BlackjackComponentFactory.active(this))
                .edit();
    }

    private void editResult(ButtonClickContext ctx) {
        ctx.create()
                .setEmbeds(BlackjackMessageFactory.result(player, round))
                .setComponents(BlackjackComponentFactory.finished())
                .edit();
    }

    private void refreshFrom(ButtonClickContext ctx) {
        if (round.isSettled()) {
            editResult(ctx);
        } else {
            editActive(ctx);
        }
    }

    private boolean canReserveExtraStake(int amount) {
        UserEconomy eco = ecoRepo.findByUserId(userId());
        return eco != null
                && eco.getBank() >= amount
                && canReceiveMaxPotentialCredit(eco, amount);
    }

    private InteractionResult reserveExtraStake(int amount) {
        UserEconomy eco = ecoRepo.findByUserId(userId());
        if (eco == null || eco.getBank() < amount) {
            return Status.INSUFFICIENT_BALANCE;
        }
        if (!canReceiveMaxPotentialCredit(eco, amount)) {
            return Status.USER_CANNOT_RECEIVE_GIVEN_AMOUNT;
        }

        eco.modifyBalance(0, -amount).tickUpdate();
        ecoRepo.upsert(eco);
        return Status.OK;
    }

    private boolean canReceiveMaxPotentialCredit(UserEconomy eco, int additionalStake) {
        long nextBank = (long) eco.getBank() - additionalStake;
        long nextStake = (long) round.totalStake() + additionalStake;
        return nextBank + (nextStake * 2L) <= Integer.MAX_VALUE;
    }

    private void settleEconomyAndPersist() {
        if (settledEconomy) return;

        status = GameStatus.COMPLETE;
        endedAt = Bot.unixNow();
        scheduler.shutdown();

        try {
            credit(resolveCredit());
            persist();
        } catch (DataAccessException e) {
            LOGGER.error("Could not settle blackjack game {}", id, e);
        } finally {
            settledEconomy = true;
            BET_MANAGER.removeBet(userId());
        }
    }

    private int resolveCredit() {
        return round.resolvedHands().stream().mapToInt(BlackjackResolvedHand::credit).sum();
    }

    private int resolveNet() {
        return round.resolvedHands().stream().mapToInt(BlackjackResolvedHand::net).sum();
    }

    private void credit(int amount) {
        if (amount <= 0) return;

        UserEconomy eco = ecoRepo.findByUserId(userId(), UserEconomy.fromUserId(userId()));
        long nextBank = (long) eco.getBank() + amount;
        eco.setBank((int) Math.min(nextBank, Integer.MAX_VALUE)).tickUpdate();
        ecoRepo.upsert(eco);
    }

    private void refund(int amount) {
        if (amount <= 0) return;

        UserEconomy eco = ecoRepo.findByUserId(userId(), UserEconomy.fromUserId(userId()));
        long nextBank = (long) eco.getBank() + amount;
        eco.setBank((int) Math.min(nextBank, Integer.MAX_VALUE)).tickUpdate();
        ecoRepo.upsert(eco);
    }

    private void persist() {
        BetGame game = new BetGame(id, status, formatBoard(), GameType.BLACKJACK, startedAt, endedAt);
        GameParticipant participant = new GameParticipant(id, userId(), resolveNet() > 0);

        betRepo.save(game);
        participantRepo.bulkSave(List.of(participant));
    }

    private String formatBoard() {
        String hands = round.resolvedHands().stream()
                .map(resolved -> resolved.outcome().name() + ":" + resolved.hand().value().total())
                .reduce((a, b) -> a + "," + b)
                .orElse("UNKNOWN");

        return "player=" + hands
                + ";dealer=" + round.dealer().value().total()
                + ";net=" + resolveNet()
                + ";cards_remaining=" + round.cardsRemaining();
    }

    public record MessageEmbedData(
            net.dv8tion.jda.api.entities.MessageEmbed embed,
            List<net.dv8tion.jda.api.components.actionrow.ActionRow> components
    ) {}
}
