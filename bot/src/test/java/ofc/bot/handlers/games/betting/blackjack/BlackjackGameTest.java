package ofc.bot.handlers.games.betting.blackjack;

import ofc.bot.testing.MySQLTestDatabase;

import ofc.bot.domain.entity.UserEconomy;
import ofc.bot.domain.database.repository.BetGameRepository;
import ofc.bot.domain.database.repository.GameParticipantRepository;
import ofc.bot.domain.database.repository.UserEconomyRepository;
import ofc.bot.domain.tables.BetGamesTable;
import ofc.bot.domain.tables.GamesParticipantsTable;
import ofc.bot.domain.tables.UsersEconomyTable;
import ofc.bot.domain.tables.UsersTable;
import ofc.bot.handlers.games.GameType;
import ofc.bot.handlers.interactions.commands.responses.states.Status;
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

class BlackjackGameTest {
    private static final UsersTable USERS = UsersTable.USERS;
    private static final BetGamesTable BET_GAMES = BetGamesTable.BET_GAMES;
    private static final GamesParticipantsTable PARTICIPANTS = GamesParticipantsTable.GAMES_PARTICIPANTS;

    @Test
    void shouldCreditEvenMoneyWinAndPersistGame() throws Exception {
        withDatabase((ctx, ecoRepo) -> {
            BlackjackGame game = game(ctx, ecoRepo, BlackjackShoe.fixed(
                    BlackjackCard.TEN_OF_CLUBS,
                    BlackjackCard.TEN_OF_HEARTS,
                    BlackjackCard.NINE_OF_SPADES,
                    BlackjackCard.SEVEN_OF_CLUBS
            ));
            reserveInitial(ecoRepo, 1_000);

            assertEquals(Status.OK, game.apply(BlackjackAction.STAND).getStatus());

            assertEquals(6_000, ecoRepo.findByUserId(1L).getBank());
            assertEquals(1, ctx.fetchCount(BET_GAMES, BET_GAMES.BET_TYPE.eq(GameType.BLACKJACK.name())));
            assertTrue(ctx.select(PARTICIPANTS.HAS_WON)
                    .from(PARTICIPANTS)
                    .where(PARTICIPANTS.USER_ID.eq(1L))
                    .fetchOneInto(boolean.class));
        });
    }

    @Test
    void shouldReserveDoubleStakeAndCreditDoubleWin() throws Exception {
        withDatabase((ctx, ecoRepo) -> {
            BlackjackGame game = game(ctx, ecoRepo, BlackjackShoe.fixed(
                    BlackjackCard.FIVE_OF_CLUBS,
                    BlackjackCard.TEN_OF_HEARTS,
                    BlackjackCard.SIX_OF_SPADES,
                    BlackjackCard.NINE_OF_CLUBS,
                    BlackjackCard.KING_OF_DIAMONDS
            ));
            reserveInitial(ecoRepo, 1_000);

            assertEquals(Status.OK, game.apply(BlackjackAction.DOUBLE_DOWN).getStatus());

            assertEquals(7_000, ecoRepo.findByUserId(1L).getBank());
            assertEquals(2_000, game.round().totalStake());
        });
    }

    @Test
    void shouldReserveSplitStakeAndSettleBothHands() throws Exception {
        withDatabase((ctx, ecoRepo) -> {
            BlackjackGame game = game(ctx, ecoRepo, BlackjackShoe.fixed(
                    BlackjackCard.EIGHT_OF_CLUBS,
                    BlackjackCard.TEN_OF_HEARTS,
                    BlackjackCard.EIGHT_OF_SPADES,
                    BlackjackCard.SEVEN_OF_CLUBS,
                    BlackjackCard.THREE_OF_DIAMONDS,
                    BlackjackCard.KING_OF_HEARTS
            ));
            reserveInitial(ecoRepo, 1_000);

            assertEquals(Status.OK, game.apply(BlackjackAction.SPLIT).getStatus());
            assertEquals(Status.OK, game.apply(BlackjackAction.STAND).getStatus());
            assertEquals(Status.OK, game.apply(BlackjackAction.STAND).getStatus());

            assertEquals(5_000, ecoRepo.findByUserId(1L).getBank());
            assertEquals(2, game.round().resolvedHands().size());
            assertFalse(ctx.select(PARTICIPANTS.HAS_WON)
                    .from(PARTICIPANTS)
                    .where(PARTICIPANTS.USER_ID.eq(1L))
                    .fetchOneInto(boolean.class));
        });
    }

    @Test
    void shouldRejectDoubleDownWhenBankCannotReserveExtraStake() throws Exception {
        withDatabase((ctx, ecoRepo) -> {
            BlackjackGame game = game(ctx, ecoRepo, BlackjackShoe.fixed(
                    BlackjackCard.FIVE_OF_CLUBS,
                    BlackjackCard.TEN_OF_HEARTS,
                    BlackjackCard.SIX_OF_SPADES,
                    BlackjackCard.NINE_OF_CLUBS,
                    BlackjackCard.KING_OF_DIAMONDS
            ));

            UserEconomy eco = ecoRepo.findByUserId(1L);
            eco.setBank(1_000).tickUpdate();
            ecoRepo.upsert(eco);
            reserveInitial(ecoRepo, 1_000);

            assertEquals(Status.INSUFFICIENT_BALANCE, game.apply(BlackjackAction.DOUBLE_DOWN).getStatus());
            assertEquals(0, ecoRepo.findByUserId(1L).getBank());
            assertFalse(game.round().isSettled());
        });
    }

    private void withDatabase(DatabaseCase testCase) throws Exception {
        Path db = Files.createTempFile("blackjack-game", ".db");

        try (Connection connection = MySQLTestDatabase.open()) {
            DSLContext ctx = setup(connection);
            UserEconomyRepository ecoRepo = new UserEconomyRepository(ctx);
            insertUser(ctx, 1L);
            ecoRepo.save(new UserEconomy(1L, 0, 5_000, 0, 0, Bot.unixNow(), Bot.unixNow()));
            testCase.run(ctx, ecoRepo);
        } finally {
            Files.deleteIfExists(db);
        }
    }

    private BlackjackGame game(DSLContext ctx, UserEconomyRepository ecoRepo, BlackjackShoe shoe) {
        return new BlackjackGame(
                new BlackjackPlayerView(1L, "leeo13", null),
                1_000,
                ecoRepo,
                new BetGameRepository(ctx),
                new GameParticipantRepository(ctx),
                shoe
        );
    }

    private DSLContext setup(Connection connection) {
        DSLContext ctx = MySQLTestDatabase.context(connection);
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

    private void reserveInitial(UserEconomyRepository ecoRepo, int amount) {
        UserEconomy eco = ecoRepo.findByUserId(1L);
        eco.modifyBalance(0, -amount).tickUpdate();
        ecoRepo.upsert(eco);
    }

    private interface DatabaseCase {
        void run(DSLContext ctx, UserEconomyRepository ecoRepo);
    }
}
