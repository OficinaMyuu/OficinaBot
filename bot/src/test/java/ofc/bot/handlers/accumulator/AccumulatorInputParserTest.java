package ofc.bot.handlers.accumulator;

import ofc.bot.domain.entity.enums.AccumulatorPrizeType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AccumulatorInputParserTest {
    @Test
    void shouldParseBulkMoneyWithPerLineAndDefaultAmounts() {
        AccumulatorInputParser.Result result = AccumulatorInputParser.parse(
                AccumulatorPrizeType.MONEY,
                null,
                "<@123> 500\n456",
                250
        );

        assertTrue(result.isOk());
        assertEquals(2, result.entries().size());
        assertEquals(123L, result.entries().getFirst().userId());
        assertEquals(500, result.entries().getFirst().amount());
        assertEquals(456L, result.entries().get(1).userId());
        assertEquals(250, result.entries().get(1).amount());
    }

    @Test
    void shouldRejectMoneyWithoutAmount() {
        AccumulatorInputParser.Result result = AccumulatorInputParser.parse(
                AccumulatorPrizeType.MONEY,
                null,
                "123",
                null
        );

        assertFalse(result.isOk());
        assertTrue(result.errors().getFirst().contains("amount"));
    }

    @Test
    void shouldRejectAmountsOverLimit() {
        AccumulatorInputParser.Result result = AccumulatorInputParser.parse(
                AccumulatorPrizeType.MONEY,
                null,
                "123 1000001",
                null
        );

        assertFalse(result.isOk());
        assertTrue(result.errors().getFirst().contains("1000000"));
    }

    @Test
    void shouldRejectColorRoleEntriesWithExtraTokens() {
        AccumulatorInputParser.Result result = AccumulatorInputParser.parse(
                AccumulatorPrizeType.COLOR_ROLE,
                null,
                "123 500",
                null
        );

        assertFalse(result.isOk());
        assertTrue(result.errors().getFirst().contains("only accept a user id"));
    }
}
