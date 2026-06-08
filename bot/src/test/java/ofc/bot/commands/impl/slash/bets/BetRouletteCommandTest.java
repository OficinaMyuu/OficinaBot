package ofc.bot.commands.impl.slash.bets;

import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import ofc.bot.handlers.games.betting.roulette.RouletteGame;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BetRouletteCommandTest {
    @Test
    void shouldParseAmountShorthandsAgainstBankBalance() {
        assertEquals(2_000, BetRouletteCommand.parseBetAmount("2k", 10_000));
        assertEquals(750_000, BetRouletteCommand.parseBetAmount("750k", 1_000_000));
        assertEquals(5_000, BetRouletteCommand.parseBetAmount("all", 5_000));
    }

    @Test
    void shouldRejectInvalidOrOutOfRangeAmounts() {
        assertEquals(-1, BetRouletteCommand.parseBetAmount("banana", 10_000));
        assertEquals(-1, BetRouletteCommand.parseBetAmount("99", 10_000));
        assertEquals(-1, BetRouletteCommand.parseBetAmount("1001k", 2_000_000));
        assertEquals(-1, BetRouletteCommand.parseBetAmount("all", RouletteGame.MAX_AMOUNT + 1));
    }

    @Test
    void shouldRegisterAmountAsStringOption() {
        BetRouletteCommand command = new BetRouletteCommand(null, null, null);
        List<OptionData> options = command.getOptions();

        assertEquals(OptionType.STRING, options.get(1).getType());
        assertEquals("amount", options.get(1).getName());
        assertEquals(true, options.get(1).isRequired());
    }
}
