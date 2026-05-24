package ofc.bot.handlers.giveaway;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GiveawayInputParserTest {
    @Test
    void shouldParseDurationPatternsToSeconds() {
        assertEquals(90, GiveawayInputParser.parseDurationSeconds("1m 30s"));
        assertEquals(604800, GiveawayInputParser.parseDurationSeconds("7d"));
    }

    @Test
    void shouldRejectInvalidDurations() {
        assertEquals(-1, GiveawayInputParser.parseDurationSeconds("wat"));
    }

    @Test
    void shouldParsePositiveMoneyShortcuts() {
        assertEquals(5_000, GiveawayInputParser.parsePositiveMoney("5k"));
        assertEquals(2_000_000, GiveawayInputParser.parsePositiveMoney("2m"));
    }

    @Test
    void shouldRejectInvalidMoney() {
        assertEquals(-1, GiveawayInputParser.parsePositiveMoney("all"));
        assertEquals(-1, GiveawayInputParser.parsePositiveMoney("-1"));
        assertEquals(-1, GiveawayInputParser.parsePositiveMoney(String.valueOf((long) Integer.MAX_VALUE + 1)));
    }
}
