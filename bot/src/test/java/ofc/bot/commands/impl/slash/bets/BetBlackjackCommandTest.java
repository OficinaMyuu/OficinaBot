package ofc.bot.commands.impl.slash.bets;

import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import ofc.bot.domain.entity.UserEconomy;
import ofc.bot.domain.sqlite.repository.UserEconomyRepository;
import ofc.bot.domain.tables.UsersEconomyTable;
import ofc.bot.domain.tables.UsersTable;
import ofc.bot.handlers.games.betting.blackjack.BlackjackGame;
import ofc.bot.util.Bot;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BetBlackjackCommandTest {
    @Test
    void shouldParseAmountShorthandsAgainstBankBalance() {
        assertEquals(2_000, BetBlackjackCommand.parseBetAmount("2k", 10_000));
        assertEquals(750_000, BetBlackjackCommand.parseBetAmount("750k", 1_000_000));
        assertEquals(5_000, BetBlackjackCommand.parseBetAmount("all", 5_000));
    }

    @Test
    void shouldRejectInvalidOrOutOfRangeAmounts() {
        assertEquals(-1, BetBlackjackCommand.parseBetAmount("banana", 10_000));
        assertEquals(-1, BetBlackjackCommand.parseBetAmount("99", 10_000));
        assertEquals(-1, BetBlackjackCommand.parseBetAmount("1001k", 2_000_000));
        assertEquals(-1, BetBlackjackCommand.parseBetAmount("all", BlackjackGame.MAX_AMOUNT + 1));
    }

    @Test
    void shouldRegisterAmountAsStringOption() {
        BetBlackjackCommand command = new BetBlackjackCommand(null, null, null);
        List<OptionData> options = command.getOptions();

        assertEquals(OptionType.STRING, options.getFirst().getType());
        assertEquals("amount", options.getFirst().getName());
        assertTrue(options.getFirst().isRequired());
    }

    @Test
    void shouldReserveInitialStakeFromBank() throws Exception {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite::memory:")) {
            DSLContext ctx = setup(connection);
            UserEconomyRepository ecoRepo = new UserEconomyRepository(ctx);
            BetBlackjackCommand command = new BetBlackjackCommand(ecoRepo, null, null);
            insertUser(ctx, 1L);
            UserEconomy eco = new UserEconomy(1L, 0, 3_000, 0, 0, Bot.unixNow(), Bot.unixNow());
            ecoRepo.save(eco);

            command.reserveInitialStake(eco, 1_000);

            assertEquals(2_000, ecoRepo.findByUserId(1L).getBank());
        }
    }

    @Test
    void shouldRejectInitialWinThatWouldOverflowBank() {
        UserEconomy eco = new UserEconomy(1L, 0, Integer.MAX_VALUE, 0, 0, Bot.unixNow(), Bot.unixNow());

        assertFalse(BetBlackjackCommand.canReceiveInitialWin(eco, 1_000_000));
    }

    private DSLContext setup(Connection connection) {
        DSLContext ctx = DSL.using(connection, SQLDialect.SQLITE);
        UsersTable.USERS.getSchema(ctx).execute();
        UsersEconomyTable.USERS_ECONOMY.getSchema(ctx).execute();
        return ctx;
    }

    private void insertUser(DSLContext ctx, long userId) {
        long now = Bot.unixNow();
        UsersTable users = UsersTable.USERS;
        ctx.insertInto(users)
                .set(users.ID, userId)
                .set(users.NAME, "user-" + userId)
                .set(users.CREATED_AT, now)
                .set(users.UPDATED_AT, now)
                .execute();
    }
}
