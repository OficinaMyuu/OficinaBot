package ofc.bot.handlers.games.betting.roulette;

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
import ofc.bot.util.Bot;
import ofc.bot.util.time.ElasticScheduler;
import org.jooq.exception.DataAccessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.IntSupplier;

/**
 * Coordinates one timed roulette betting lobby in a single Discord channel.
 * <p>
 * The game owns the accepted entries, active-bet locks, countdown scheduler,
 * economy charging/refunding, settlement payouts, persistence, and Discord lobby
 * updates for one shared spin. Instances are created and removed by
 * {@link RouletteGameManager}; callers should interact through the synchronized
 * methods because the timer thread and slash-command thread can both access the
 * same game.
 */
public final class RouletteGame {
    public static final int MIN_AMOUNT = 100;
    public static final int MAX_AMOUNT = 1_000_000;
    public static final int TIMEOUT_SECONDS = 30;

    private static final Logger LOGGER = LoggerFactory.getLogger(RouletteGame.class);
    private static final ErrorHandler IGNORE_UNKNOWN_MESSAGE = new ErrorHandler().ignore(ErrorResponse.UNKNOWN_MESSAGE);
    private static final BetManager BET_MANAGER = BetManager.getManager();

    private final RouletteGameManager manager;
    private final long channelId;
    private final long id;
    private final UserEconomyRepository ecoRepo;
    private final BetGameRepository betRepo;
    private final GameParticipantRepository participantRepo;
    private final IntSupplier spinSupplier;
    private final ElasticScheduler scheduler;
    private final List<RouletteEntry> entries;

    private GameStatus status;
    private Message message;
    private long startedAt;
    private long endsAt;

    /**
     * Creates an in-memory roulette game for a channel.
     *
     * @param manager owner that tracks active channel games
     * @param channelId Discord channel where the lobby is running
     * @param ecoRepo repository used to charge and pay user bank balances
     * @param betRepo repository used to persist completed game metadata
     * @param participantRepo repository used to persist game participants
     * @param spinSupplier deterministic or random supplier for the landed number
     */
    RouletteGame(
            RouletteGameManager manager,
            long channelId,
            UserEconomyRepository ecoRepo,
            BetGameRepository betRepo,
            GameParticipantRepository participantRepo,
            IntSupplier spinSupplier
    ) {
        this.manager = manager;
        this.channelId = channelId;
        this.id = System.nanoTime();
        this.ecoRepo = ecoRepo;
        this.betRepo = betRepo;
        this.participantRepo = participantRepo;
        this.spinSupplier = spinSupplier;
        this.scheduler = new ElasticScheduler(this::settle, TIMEOUT_SECONDS, TimeUnit.SECONDS);
        this.entries = new ArrayList<>();
        this.status = GameStatus.WAITING;
    }

    /**
     * Attempts to accept a bet into the open lobby.
     * <p>
     * Accepted bets are charged from bank immediately and create an active bet
     * lock for the user when needed. Rejections leave economy state untouched and
     * explain whether the game is closed, the amount is invalid, another bet is
     * active, the user lacks bank balance, or the maximum possible payout would
     * overflow the supported account range.
     *
     * @param userId Discord user placing the bet
     * @param bet parsed roulette space
     * @param amount bank stake to charge immediately
     * @return accepted entry metadata or a rejection reason
     */
    public synchronized RoulettePlacementResult placeBet(long userId, RouletteBet bet, int amount) {
        if (!isAcceptingBets()) {
            return new RoulettePlacementResult.Rejected(RoulettePlacementResult.Reason.GAME_CLOSED);
        }
        if (amount < MIN_AMOUNT || amount > MAX_AMOUNT) {
            return new RoulettePlacementResult.Rejected(RoulettePlacementResult.Reason.INVALID_AMOUNT);
        }

        Object active = BET_MANAGER.getActive(userId);
        if (active != null && active != this) {
            return new RoulettePlacementResult.Rejected(RoulettePlacementResult.Reason.ACTIVE_OTHER_GAME);
        }

        UserEconomy eco = ecoRepo.findByUserId(userId);
        if (eco == null || eco.getBank() < amount) {
            return new RoulettePlacementResult.Rejected(RoulettePlacementResult.Reason.INSUFFICIENT_BALANCE);
        }
        if (!canReceiveMaxPotentialPayout(eco, userId, bet, amount)) {
            return new RoulettePlacementResult.Rejected(RoulettePlacementResult.Reason.PAYOUT_LIMIT);
        }

        boolean firstBet = entries.isEmpty();
        RouletteEntry entry = new RouletteEntry(userId, bet, amount, Bot.unixNow());

        eco.modifyBalance(0, -amount).tickUpdate();
        ecoRepo.upsert(eco);

        if (active == null) {
            BET_MANAGER.addSession(userId, this);
        }
        entries.add(entry);

        return new RoulettePlacementResult.Accepted(entry, firstBet);
    }

    /**
     * Checks whether new bets may still enter this lobby.
     *
     * @return {@code true} while the game is waiting for the countdown to finish
     */
    public synchronized boolean isAcceptingBets() {
        return status == GameStatus.WAITING;
    }

    /**
     * Checks whether the lobby has no accepted entries.
     *
     * @return {@code true} when no stake has been charged into this game
     */
    public synchronized boolean isEmpty() {
        return entries.isEmpty();
    }

    /**
     * Captures a stable view of the current lobby for message rendering.
     *
     * @return immutable snapshot of identity, channel, status, deadline, and bets
     */
    public synchronized RouletteGameSnapshot snapshot() {
        return new RouletteGameSnapshot(id, channelId, status, endsAt, entries);
    }

    /**
     * Starts the countdown after the initial Discord lobby message exists.
     * <p>
     * This method binds the game to the message that will receive live lobby
     * edits, initializes the deadline used by relative timestamps, refreshes the
     * first rendered lobby, and starts the one-shot scheduler.
     *
     * @param message Discord lobby message created from the first accepted bet
     */
    public synchronized void start(Message message) {
        if (this.message != null || status != GameStatus.WAITING) return;

        this.message = message;
        this.startedAt = Bot.unixNow();
        this.endsAt = startedAt + TIMEOUT_SECONDS;
        refreshLobbyMessage();
        scheduler.start();
    }

    /**
     * Refreshes the existing lobby message with the latest accepted bets.
     * <p>
     * Unknown-message errors are ignored because users or moderators can delete
     * the lobby message while the in-memory game is still valid.
     */
    public synchronized void refreshLobbyMessage() {
        if (message == null || status != GameStatus.WAITING) return;

        message.editMessageEmbeds(RouletteMessageFactory.lobby(snapshot()))
                .queue(null, IGNORE_UNKNOWN_MESSAGE);
    }

    /**
     * Cancels an open lobby and returns every charged stake to bank.
     * <p>
     * This is used when the initial interaction cannot create or retrieve the
     * lobby message. Completed or already-closing games are left untouched.
     */
    public synchronized void cancelAndRefund() {
        if (status != GameStatus.WAITING) return;

        status = GameStatus.INTERRUPTED;
        scheduler.shutdown();
        refundStakes();
        clearActiveBets();
        manager.remove(channelId, this);
    }

    /**
     * Resolves the active roulette lobby once the countdown finishes.
     * <p>
     * Settlement is responsible for closing the in-memory lobby, calculating the
     * deterministic spin from the configured supplier, paying winners, persisting
     * the completed game, clearing active betting locks, and publishing the result
     * as a new chat message. The original lobby message is intentionally left
     * untouched so players can still see the final pre-spin bet list.
     */
    private synchronized void settle() {
        if (status != GameStatus.WAITING) return;

        status = GameStatus.RUNNING;
        RouletteSpin spin = RouletteSpin.random(spinSupplier);
        RouletteResult result = RouletteResult.resolve(spin, List.copyOf(entries));
        long endedAt = Bot.unixNow();

        try {
            payWinners(result.payoutsByUser());
            persist(result, endedAt);
        } catch (DataAccessException e) {
            LOGGER.error("Could not settle roulette game {}", id, e);
        } finally {
            status = GameStatus.COMPLETE;
            scheduler.shutdown();
            clearActiveBets();
            manager.remove(channelId, this);
        }

        if (message != null) {
            message.getChannel().sendMessageEmbeds(RouletteMessageFactory.result(result)).queue();
        }
    }

    /**
     * Checks whether accepting another bet could overflow the user's bank after
     * the maximum possible payout is returned.
     *
     * @param eco current user economy record
     * @param userId Discord user placing the bet
     * @param bet parsed roulette space
     * @param amount proposed stake
     * @return {@code true} when the user's bank can safely receive all potential
     * payouts for their current and proposed bets
     */
    private boolean canReceiveMaxPotentialPayout(UserEconomy eco, long userId, RouletteBet bet, int amount) {
        long currentPotential = entries.stream()
                .filter(entry -> entry.userId() == userId)
                .mapToLong(RouletteEntry::maxPayout)
                .sum();
        long bankAfterStake = (long) eco.getBank() - amount;
        long maxPotential = currentPotential + bet.payoutFor(amount);

        return bankAfterStake + maxPotential <= Integer.MAX_VALUE;
    }

    /**
     * Returns all accepted stakes to their original bettors.
     */
    private void refundStakes() {
        Map<Long, Integer> refunds = new LinkedHashMap<>();
        for (RouletteEntry entry : entries) {
            refunds.merge(entry.userId(), entry.amount(), Integer::sum);
        }
        addBankBalances(refunds);
    }

    /**
     * Credits winner payouts to bank using the same balance helper as refunds.
     *
     * @param payouts aggregate bank payouts keyed by Discord user id
     */
    private void payWinners(Map<Long, Integer> payouts) {
        addBankBalances(payouts);
    }

    /**
     * Adds bank amounts to users, creating economy rows when necessary and
     * capping balances at the supported signed integer maximum.
     *
     * @param values bank deltas keyed by Discord user id
     */
    private void addBankBalances(Map<Long, Integer> values) {
        for (Map.Entry<Long, Integer> value : values.entrySet()) {
            long userId = value.getKey();
            int amount = value.getValue();
            UserEconomy eco = ecoRepo.findByUserId(userId, UserEconomy.fromUserId(userId));
            long nextBank = (long) eco.getBank() + amount;

            eco.setBank((int) Math.min(nextBank, Integer.MAX_VALUE)).tickUpdate();
            ecoRepo.upsert(eco);
        }
    }

    /**
     * Releases active-bet locks for every user that entered this lobby.
     */
    private void clearActiveBets() {
        Set<Long> userIds = new LinkedHashSet<>();
        for (RouletteEntry entry : entries) {
            userIds.add(entry.userId());
        }
        BET_MANAGER.removeBets(userIds);
    }

    /**
     * Persists the completed roulette game and one participant row per distinct
     * player, marking players as winners when at least one of their bets paid out.
     *
     * @param result settled spin and resolved entries
     * @param endedAt unix timestamp captured at settlement
     */
    private void persist(RouletteResult result, long endedAt) {
        BetGame game = new BetGame(id, GameStatus.COMPLETE, formatBoard(result.spin()), GameType.ROULETTE, startedAt, endedAt);
        Set<Long> winners = result.winners();
        List<GameParticipant> participants = entries.stream()
                .map(RouletteEntry::userId)
                .distinct()
                .map(userId -> new GameParticipant(id, userId, winners.contains(userId)))
                .toList();

        betRepo.save(game);
        participantRepo.bulkSave(participants);
    }

    /**
     * Formats the persisted board summary for roulette history.
     *
     * @param spin landed roulette spin
     * @return compact key-value board representation
     */
    private String formatBoard(RouletteSpin spin) {
        return "number=" + spin.number() + ";color=" + spin.color().displayName();
    }
}
