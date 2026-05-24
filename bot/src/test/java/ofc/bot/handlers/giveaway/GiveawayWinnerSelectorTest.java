package ofc.bot.handlers.giveaway;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class GiveawayWinnerSelectorTest {
    @Test
    void shouldSelectUniqueWinnersAndRespectRequestedLimit() {
        GiveawayWinnerSelector selector = new GiveawayWinnerSelector(new Random(10));

        List<Long> winners = selector.selectWinners(List.of(1L, 1L, 2L, 3L), 2, Set.of());

        assertEquals(2, winners.size());
        assertEquals(2, winners.stream().distinct().count());
    }

    @Test
    void shouldExcludePreviousWinners() {
        GiveawayWinnerSelector selector = new GiveawayWinnerSelector(new Random(10));

        List<Long> winners = selector.selectWinners(List.of(1L, 2L, 3L), 3, Set.of(1L, 2L));

        assertEquals(List.of(3L), winners);
    }

    @Test
    void shouldReturnEmptyListWhenNoWinnersCanBeDrawn() {
        GiveawayWinnerSelector selector = new GiveawayWinnerSelector(new Random(10));

        assertTrue(selector.selectWinners(List.of(1L), 0, Set.of()).isEmpty());
        assertTrue(selector.selectWinners(List.of(1L), 1, Set.of(1L)).isEmpty());
    }
}
