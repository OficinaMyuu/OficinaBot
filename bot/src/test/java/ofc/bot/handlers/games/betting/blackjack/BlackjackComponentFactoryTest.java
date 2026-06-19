package ofc.bot.handlers.games.betting.blackjack;

import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
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

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BlackjackComponentFactoryTest {
    @Test
    void activeButtonsShouldUsePortugueseLabelsAndNoHelpButton() throws Exception {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite::memory:")) {
            DSLContext ctx = setup(connection);
            UserEconomyRepository ecoRepo = new UserEconomyRepository(ctx);
            insertUser(ctx, 1L);
            ecoRepo.save(new UserEconomy(1L, 0, 5_000, 0, 0, Bot.unixNow(), Bot.unixNow()));

            BlackjackGame game = new BlackjackGame(
                    new BlackjackPlayerView(1L, "leeo13", null),
                    1_000,
                    ecoRepo,
                    new BetGameRepository(ctx),
                    new GameParticipantRepository(ctx),
                    BlackjackShoe.fixed(
                            BlackjackCard.FIVE_OF_CLUBS,
                            BlackjackCard.TEN_OF_HEARTS,
                            BlackjackCard.SIX_OF_SPADES,
                            BlackjackCard.NINE_OF_CLUBS
                    )
            );

            List<String> labels = labels(BlackjackComponentFactory.active(game).getFirst());

            assertEquals(List.of("Pedir Carta", "Parar", "Dobrar", "Dividir"), labels);
            assertFalse(labels.contains("Help"));
        }
    }

    @Test
    void finishedButtonsShouldBeDisabledAndHaveNoHelpButton() {
        ActionRow row = BlackjackComponentFactory.finished().getFirst();
        List<Button> buttons = row.getComponents().stream()
                .map(component -> (Button) component)
                .toList();

        assertEquals(List.of("Pedir Carta", "Parar", "Dobrar", "Dividir"), labels(row));
        assertTrue(buttons.stream().allMatch(Button::isDisabled));
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
        UsersTable users = UsersTable.USERS;
        ctx.insertInto(users)
                .set(users.ID, userId)
                .set(users.NAME, "user-" + userId)
                .set(users.CREATED_AT, now)
                .set(users.UPDATED_AT, now)
                .execute();
    }

    private List<String> labels(ActionRow row) {
        return row.getComponents().stream()
                .map(component -> ((Button) component).getLabel())
                .toList();
    }
}
