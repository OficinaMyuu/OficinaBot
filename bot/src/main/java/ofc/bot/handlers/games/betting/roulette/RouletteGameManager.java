package ofc.bot.handlers.games.betting.roulette;

import ofc.bot.domain.sqlite.repository.BetGameRepository;
import ofc.bot.domain.sqlite.repository.GameParticipantRepository;
import ofc.bot.domain.sqlite.repository.UserEconomyRepository;

import java.util.HashMap;
import java.util.Map;
import java.util.function.IntSupplier;

public final class RouletteGameManager {
    private static final RouletteGameManager INSTANCE = new RouletteGameManager();

    private final Map<Long, RouletteGame> gamesByChannel = new HashMap<>();

    private RouletteGameManager() {}

    public static RouletteGameManager getManager() {
        return INSTANCE;
    }

    public synchronized RouletteGame getOrCreate(
            long channelId,
            UserEconomyRepository ecoRepo,
            BetGameRepository betRepo,
            GameParticipantRepository participantRepo
    ) {
        return getOrCreate(channelId, ecoRepo, betRepo, participantRepo, () -> RouletteSpin.random().number());
    }

    synchronized RouletteGame getOrCreate(
            long channelId,
            UserEconomyRepository ecoRepo,
            BetGameRepository betRepo,
            GameParticipantRepository participantRepo,
            IntSupplier spinSupplier
    ) {
        RouletteGame existing = gamesByChannel.get(channelId);
        if (existing != null && existing.isAcceptingBets()) {
            return existing;
        }

        RouletteGame created = new RouletteGame(this, channelId, ecoRepo, betRepo, participantRepo, spinSupplier);
        gamesByChannel.put(channelId, created);
        return created;
    }

    synchronized void remove(long channelId, RouletteGame game) {
        gamesByChannel.remove(channelId, game);
    }

    public synchronized void removeIfEmpty(long channelId, RouletteGame game) {
        if (game.isEmpty()) {
            remove(channelId, game);
        }
    }
}
