package ofc.bot.handlers.giveaway;

import org.jetbrains.annotations.NotNull;

import java.security.SecureRandom;
import java.util.*;
import java.util.random.RandomGenerator;

public class GiveawayWinnerSelector {
    private final RandomGenerator random;

    public GiveawayWinnerSelector() {
        this(new SecureRandom());
    }

    public GiveawayWinnerSelector(@NotNull RandomGenerator random) {
        this.random = Objects.requireNonNull(random);
    }

    public List<Long> selectWinners(
            @NotNull Collection<Long> entries,
            int winnerCount,
            @NotNull Set<Long> excludedUserIds
    ) {
        if (winnerCount <= 0) {
            return List.of();
        }

        List<Long> candidates = new ArrayList<>(new LinkedHashSet<>(entries));
        candidates.removeIf(excludedUserIds::contains);
        shuffle(candidates);

        int resultSize = Math.min(winnerCount, candidates.size());
        return List.copyOf(candidates.subList(0, resultSize));
    }

    private void shuffle(List<Long> values) {
        for (int i = values.size() - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            Collections.swap(values, i, j);
        }
    }
}
