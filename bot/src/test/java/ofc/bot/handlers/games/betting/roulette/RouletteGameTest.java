package ofc.bot.handlers.games.betting.roulette;

import ofc.bot.domain.entity.UserEconomy;
import ofc.bot.domain.sqlite.repository.BetGameRepository;
import ofc.bot.domain.sqlite.repository.GameParticipantRepository;
import ofc.bot.domain.sqlite.repository.UserEconomyRepository;
import ofc.bot.domain.tables.BetGamesTable;
import ofc.bot.domain.tables.GamesParticipantsTable;
import ofc.bot.domain.tables.UsersEconomyTable;
import ofc.bot.domain.tables.UsersTable;
import ofc.bot.util.Bot;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;

import static org.junit.jupiter.api.Assertions.*;

class RouletteGameTest {
    private static final UsersTable USERS = UsersTable.USERS;

    @Test
    void shouldChargeBankWhenBetIsAccepted() throws Exception {
        Path db = Files.createTempFile("roulette-game", ".db");

        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + db)) {
            DSLContext ctx = setup(connection);
            UserEconomyRepository ecoRepo = new UserEconomyRepository(ctx);
            RouletteGame game = new RouletteGame(
                    RouletteGameManager.getManager(),
                    123L,
                    ecoRepo,
                    new BetGameRepository(ctx),
                    new GameParticipantRepository(ctx),
                    () -> 16
            );

            insertUser(ctx, 1L);
            ecoRepo.save(new UserEconomy(1L, 0, 5_000, 0, 0, Bot.unixNow(), Bot.unixNow()));

            try {
                RoulettePlacementResult result = game.placeBet(
                        1L,
                        RouletteBet.parse("red").orElseThrow(),
                        1_000
                );

                assertInstanceOf(RoulettePlacementResult.Accepted.class, result);
                assertEquals(4_000, ecoRepo.findByUserId(1L).getBank());
            } finally {
                game.cancelAndRefund();
            }
        } finally {
            Files.deleteIfExists(db);
        }
    }

    @Test
    void shouldReplaceExistingUserBetAndReserveOnlyNewStake() throws Exception {
        Path db = Files.createTempFile("roulette-game-replace", ".db");

        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + db)) {
            DSLContext ctx = setup(connection);
            UserEconomyRepository ecoRepo = new UserEconomyRepository(ctx);
            RouletteGame game = newGame(ctx, ecoRepo);

            insertUser(ctx, 1L);
            ecoRepo.save(new UserEconomy(1L, 0, 5_000, 0, 0, Bot.unixNow(), Bot.unixNow()));

            try {
                RoulettePlacementResult first = game.placeBet(
                        1L,
                        RouletteBet.parse("red").orElseThrow(),
                        1_000
                );
                RoulettePlacementResult second = game.placeBet(
                        1L,
                        RouletteBet.parse("black").orElseThrow(),
                        2_000
                );

                assertInstanceOf(RoulettePlacementResult.Accepted.class, first);
                RoulettePlacementResult.Accepted accepted = assertInstanceOf(
                        RoulettePlacementResult.Accepted.class,
                        second
                );
                assertTrue(accepted.replacedExistingBet());
                assertEquals(3_000, ecoRepo.findByUserId(1L).getBank());

                RouletteGameSnapshot snapshot = game.snapshot();
                assertEquals(1, snapshot.entries().size());
                RouletteEntry entry = snapshot.entries().getFirst();
                assertEquals(1L, entry.userId());
                assertEquals("black", entry.bet().canonicalName());
                assertEquals(2_000, entry.amount());
            } finally {
                game.cancelAndRefund();
            }
        } finally {
            Files.deleteIfExists(db);
        }
    }

    @Test
    void shouldRefundDifferenceWhenReplacementStakeIsLower() throws Exception {
        Path db = Files.createTempFile("roulette-game-lower-replace", ".db");

        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + db)) {
            DSLContext ctx = setup(connection);
            UserEconomyRepository ecoRepo = new UserEconomyRepository(ctx);
            RouletteGame game = newGame(ctx, ecoRepo);

            insertUser(ctx, 1L);
            ecoRepo.save(new UserEconomy(1L, 0, 5_000, 0, 0, Bot.unixNow(), Bot.unixNow()));

            try {
                game.placeBet(1L, RouletteBet.parse("red").orElseThrow(), 2_000);
                RoulettePlacementResult replacement = game.placeBet(
                        1L,
                        RouletteBet.parse("black").orElseThrow(),
                        500
                );

                RoulettePlacementResult.Accepted accepted = assertInstanceOf(
                        RoulettePlacementResult.Accepted.class,
                        replacement
                );
                assertTrue(accepted.replacedExistingBet());
                assertEquals(4_500, ecoRepo.findByUserId(1L).getBank());
                assertEquals(1, game.snapshot().entries().size());
            } finally {
                game.cancelAndRefund();
            }
        } finally {
            Files.deleteIfExists(db);
        }
    }

    @Test
    void shouldAllowReplacementUsingReservedStake() throws Exception {
        Path db = Files.createTempFile("roulette-game-reserved-replace", ".db");

        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + db)) {
            DSLContext ctx = setup(connection);
            UserEconomyRepository ecoRepo = new UserEconomyRepository(ctx);
            RouletteGame game = newGame(ctx, ecoRepo);

            insertUser(ctx, 1L);
            ecoRepo.save(new UserEconomy(1L, 0, 1_000, 0, 0, Bot.unixNow(), Bot.unixNow()));

            try {
                game.placeBet(1L, RouletteBet.parse("red").orElseThrow(), 1_000);
                RoulettePlacementResult replacement = game.placeBet(
                        1L,
                        RouletteBet.parse("black").orElseThrow(),
                        1_000
                );

                RoulettePlacementResult.Accepted accepted = assertInstanceOf(
                        RoulettePlacementResult.Accepted.class,
                        replacement
                );
                assertTrue(accepted.replacedExistingBet());
                assertEquals(0, ecoRepo.findByUserId(1L).getBank());
                assertEquals("black", game.snapshot().entries().getFirst().bet().canonicalName());
            } finally {
                game.cancelAndRefund();
            }
        } finally {
            Files.deleteIfExists(db);
        }
    }

    private DSLContext setup(Connection connection) {
        DSLContext ctx = DSL.using(connection, SQLDialect.SQLITE);
        UsersTable.USERS.getSchema(ctx).execute();
        UsersEconomyTable.USERS_ECONOMY.getSchema(ctx).execute();
        BetGamesTable.BET_GAMES.getSchema(ctx).execute();
        GamesParticipantsTable.GAMES_PARTICIPANTS.getSchema(ctx).execute();
        return ctx;
    }

    private void insertUser(DSLContext ctx, long userId) {
        long now = Bot.unixNow();
        ctx.insertInto(USERS)
                .set(USERS.ID, userId)
                .set(USERS.NAME, "user-" + userId)
                .set(USERS.CREATED_AT, now)
                .set(USERS.UPDATED_AT, now)
                .execute();
    }

    private RouletteGame newGame(DSLContext ctx, UserEconomyRepository ecoRepo) {
        return new RouletteGame(
                RouletteGameManager.getManager(),
                123L,
                ecoRepo,
                new BetGameRepository(ctx),
                new GameParticipantRepository(ctx),
                () -> 16
        );
    }
}
