package ofc.bot.handlers.games.betting;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public final class BetManager {
    private static final BetManager INSTANCE = new BetManager();
    // Mapping <UserID, Bet>
    private final Map<Long, Bet<?>> active = new HashMap<>();

    private BetManager() {}

    public static BetManager getManager() {
        return INSTANCE;
    }

    public synchronized void addBet(long userId, Bet<?> bet) {
        if (active.containsKey(userId))
            throw new IllegalArgumentException("User " + userId + " is already betting");

        active.put(userId, bet);
    }

    public synchronized void addBets(Bet<?> bet, Collection<Long> userIds) {
        for (long userId : userIds) {
            addBet(userId, bet);
        }
    }

    public void removeBet(long userId) {
        active.remove(userId);
    }

    public void removeBets(Collection<Long> userIds) {
        userIds.forEach(this::removeBet);
    }

    public boolean isBetting(long userId) {
        return this.active.containsKey(userId);
    }
}