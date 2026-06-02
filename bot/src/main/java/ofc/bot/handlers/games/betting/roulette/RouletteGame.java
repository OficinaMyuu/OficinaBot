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

    public synchronized boolean isAcceptingBets() {
        return status == GameStatus.WAITING;
    }

    public synchronized boolean isEmpty() {
        return entries.isEmpty();
    }

    public synchronized RouletteGameSnapshot snapshot() {
        return new RouletteGameSnapshot(id, channelId, status, endsAt, entries);
    }

    public synchronized void start(Message message) {
        if (this.message != null || status != GameStatus.WAITING) return;

        this.message = message;
        this.startedAt = Bot.unixNow();
        this.endsAt = startedAt + TIMEOUT_SECONDS;
        refreshLobbyMessage();
        scheduler.start();
    }

    public synchronized void refreshLobbyMessage() {
        if (message == null || status != GameStatus.WAITING) return;

        message.editMessageEmbeds(RouletteMessageFactory.lobby(snapshot()))
                .queue(null, IGNORE_UNKNOWN_MESSAGE);
    }

    public synchronized void cancelAndRefund() {
        if (status != GameStatus.WAITING) return;

        status = GameStatus.INTERRUPTED;
        scheduler.shutdown();
        refundStakes();
        clearActiveBets();
        manager.remove(channelId, this);
    }

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
            message.editMessageEmbeds(RouletteMessageFactory.result(result))
                    .queue(null, IGNORE_UNKNOWN_MESSAGE);
        }
    }

    private boolean canReceiveMaxPotentialPayout(UserEconomy eco, long userId, RouletteBet bet, int amount) {
        long currentPotential = entries.stream()
                .filter(entry -> entry.userId() == userId)
                .mapToLong(RouletteEntry::maxPayout)
                .sum();
        long bankAfterStake = (long) eco.getBank() - amount;
        long maxPotential = currentPotential + bet.payoutFor(amount);

        return bankAfterStake + maxPotential <= Integer.MAX_VALUE;
    }

    private void refundStakes() {
        Map<Long, Integer> refunds = new LinkedHashMap<>();
        for (RouletteEntry entry : entries) {
            refunds.merge(entry.userId(), entry.amount(), Integer::sum);
        }
        addBankBalances(refunds);
    }

    private void payWinners(Map<Long, Integer> payouts) {
        addBankBalances(payouts);
    }

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

    private void clearActiveBets() {
        Set<Long> userIds = new LinkedHashSet<>();
        for (RouletteEntry entry : entries) {
            userIds.add(entry.userId());
        }
        BET_MANAGER.removeBets(userIds);
    }

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

    private String formatBoard(RouletteSpin spin) {
        return "number=" + spin.number() + ";color=" + spin.color().displayName();
    }
}
