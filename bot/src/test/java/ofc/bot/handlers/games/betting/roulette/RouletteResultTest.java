package ofc.bot.handlers.games.betting.roulette;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RouletteResultTest {
    @Test
    void shouldAggregatePayoutsByUser() {
        List<RouletteEntry> entries = List.of(
                entry(1L, "odd", 100),
                entry(1L, "3rd", 50),
                entry(2L, "black", 100)
        );

        RouletteResult result = RouletteResult.resolve(RouletteSpin.of(3), entries);

        assertEquals(Map.of(1L, 350), result.payoutsByUser());
        assertEquals(Set.of(1L), result.winners());
    }

    private RouletteEntry entry(long userId, String bet, int amount) {
        return new RouletteEntry(userId, RouletteBet.parse(bet).orElseThrow(), amount, 1L);
    }
}
