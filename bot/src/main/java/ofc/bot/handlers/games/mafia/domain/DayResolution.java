package ofc.bot.handlers.games.mafia.domain;

import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * Immutable result of a resolved day vote.
 *
 * @param eliminatedPlayerId player eliminated by the village vote, or {@code null} on tie/no elimination
 * @param tie whether the final vote result was a tie
 * @param voteCounts final tally keyed by target player id
 */
public record DayResolution(
        @Nullable Long eliminatedPlayerId,
        boolean tie,
        Map<Long, Integer> voteCounts
) {
    /**
     * Indicates whether somebody was actually eliminated.
     *
     * @return {@code true} when the day vote produced a single winner
     */
    public boolean hasElimination() {
        return eliminatedPlayerId != null;
    }
}
