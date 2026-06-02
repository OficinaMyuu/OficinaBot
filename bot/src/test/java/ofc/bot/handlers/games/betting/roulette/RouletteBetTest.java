package ofc.bot.handlers.games.betting.roulette;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RouletteBetTest {
    @Test
    void shouldParseEverySupportedBetSpace() {
        Map<String, Integer> expectedMultipliers = Map.ofEntries(
                Map.entry("red", 2),
                Map.entry("black", 2),
                Map.entry("even", 2),
                Map.entry("odd", 2),
                Map.entry("1-18", 2),
                Map.entry("19-36", 2),
                Map.entry("1st", 3),
                Map.entry("2nd", 3),
                Map.entry("3rd", 3),
                Map.entry("1-12", 3),
                Map.entry("13-24", 3),
                Map.entry("25-36", 3),
                Map.entry("0", 36),
                Map.entry("16", 36),
                Map.entry("36", 36)
        );

        for (Map.Entry<String, Integer> expected : expectedMultipliers.entrySet()) {
            RouletteBet bet = RouletteBet.parse(expected.getKey()).orElseThrow();

            assertEquals(expected.getKey(), bet.canonicalName());
            assertEquals(expected.getValue(), bet.multiplier());
        }
    }

    @Test
    void shouldRejectInvalidBetSpaces() {
        assertTrue(RouletteBet.parse("-1").isEmpty());
        assertTrue(RouletteBet.parse("37").isEmpty());
        assertTrue(RouletteBet.parse("middle").isEmpty());
        assertTrue(RouletteBet.parse("").isEmpty());
    }

    @Test
    void shouldResolveWinningSpacesForSixteen() {
        RouletteSpin spin = RouletteSpin.of(16);

        assertTrue(RouletteBet.parse("red").orElseThrow().wins(spin));
        assertTrue(RouletteBet.parse("even").orElseThrow().wins(spin));
        assertTrue(RouletteBet.parse("1-18").orElseThrow().wins(spin));
        assertTrue(RouletteBet.parse("1st").orElseThrow().wins(spin));
        assertTrue(RouletteBet.parse("13-24").orElseThrow().wins(spin));
        assertTrue(RouletteBet.parse("16").orElseThrow().wins(spin));

        assertFalse(RouletteBet.parse("black").orElseThrow().wins(spin));
        assertFalse(RouletteBet.parse("odd").orElseThrow().wins(spin));
        assertFalse(RouletteBet.parse("2nd").orElseThrow().wins(spin));
        assertFalse(RouletteBet.parse("25-36").orElseThrow().wins(spin));
    }

    @Test
    void shouldOnlyMatchZeroForExactZeroBets() {
        RouletteSpin spin = RouletteSpin.of(0);

        assertTrue(RouletteBet.parse("0").orElseThrow().wins(spin));
        assertFalse(RouletteBet.parse("red").orElseThrow().wins(spin));
        assertFalse(RouletteBet.parse("black").orElseThrow().wins(spin));
        assertFalse(RouletteBet.parse("even").orElseThrow().wins(spin));
        assertFalse(RouletteBet.parse("1-18").orElseThrow().wins(spin));
    }
}
