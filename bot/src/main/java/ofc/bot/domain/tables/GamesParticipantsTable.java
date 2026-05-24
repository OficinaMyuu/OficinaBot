package ofc.bot.domain.tables;

import ofc.bot.domain.abstractions.InitializableTable;
import ofc.bot.domain.entity.GameParticipant;
import org.jetbrains.annotations.NotNull;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Query;

import static ofc.bot.domain.tables.BetGamesTable.BET_GAMES;
import static ofc.bot.domain.tables.UsersTable.USERS;

public class GamesParticipantsTable extends InitializableTable<GameParticipant> {
    public static final GamesParticipantsTable GAMES_PARTICIPANTS = new GamesParticipantsTable();

    public final Field<Integer> ID      = newField("id",         INT.identity(true));
    public final Field<Long> GAME_ID    = newField("game_id",    BIGINT.notNull());
    public final Field<Long> USER_ID    = newField("user_id",    BIGINT.notNull());
    public final Field<Boolean> HAS_WON = newField("has_won",    BOOL.notNull());
    public final Field<Long> CREATED_AT = newField("created_at", BIGINT.notNull());

    public GamesParticipantsTable() {
        super("games_participants");
    }

    @NotNull
    @Override
    public Class<GameParticipant> getRecordType() {
        return GameParticipant.class;
    }

    @Override
    public Query getSchema(@NotNull DSLContext ctx) {
        return ctx.createTableIfNotExists(this)
                .primaryKey(ID)
                .columns(fields())
                .unique(GAME_ID, USER_ID)
                .constraints(
                        foreignKey(GAME_ID).references(BET_GAMES, BET_GAMES.ID),
                        foreignKey(USER_ID).references(USERS, USERS.ID)
                );
    }
}