package ofc.bot.domain.tables;

import ofc.bot.domain.abstractions.InitializableTable;
import ofc.bot.domain.entity.BetGame;
import org.jetbrains.annotations.NotNull;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Query;

public class BetGamesTable extends InitializableTable<BetGame> {
    public static final BetGamesTable BET_GAMES = new BetGamesTable();

    public final Field<Long> ID         = newField("id",         BIGINT.notNull());
    public final Field<String> STATUS   = newField("status",     CHAR.notNull());
    public final Field<String> BOARD    = newField("board",      CHAR);
    public final Field<String> BET_TYPE = newField("bet_type",   CHAR.notNull());
    public final Field<Long> STARTED_AT = newField("started_at", BIGINT.notNull());
    public final Field<Long> ENDED_AT   = newField("ended_at",   BIGINT.notNull());
    public final Field<Long> CREATED_AT = newField("created_at", BIGINT.notNull());

    public BetGamesTable() {
        super("bet_games");
    }

    @NotNull
    @Override
    public Class<BetGame> getRecordType() {
        return BetGame.class;
    }

    @Override
    public Query getSchema(@NotNull DSLContext ctx) {
        return ctx.createTableIfNotExists(this)
                .primaryKey(ID)
                .columns(fields());
    }
}