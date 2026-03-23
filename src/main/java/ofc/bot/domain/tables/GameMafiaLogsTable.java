package ofc.bot.domain.tables;

import ofc.bot.domain.abstractions.InitializableTable;
import ofc.bot.domain.entity.GameMafiaLog;
import org.jetbrains.annotations.NotNull;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Query;

public class GameMafiaLogsTable extends InitializableTable<GameMafiaLog> {
    public static final GameMafiaLogsTable GAME_MAFIA_LOGS = new GameMafiaLogsTable();

    public final Field<Integer> ID         = newField("id",          INT.identity(true));
    public final Field<String> MATCH_ID    = newField("match_id",    CHAR.notNull());
    public final Field<Long> GUILD_ID      = newField("guild_id",    BIGINT.notNull());
    public final Field<String> ACTION_TEXT = newField("action_text", CHAR.notNull());
    public final Field<Long> CREATED_AT    = newField("created_at",  BIGINT.notNull());

    public GameMafiaLogsTable() {
        super("game_mafia_logs");
    }

    @Override
    public Query getSchema(@NotNull DSLContext ctx) {
        return ctx.createTableIfNotExists(this)
                .primaryKey(ID)
                .columns(fields());
    }

    @NotNull
    @Override
    public Class<GameMafiaLog> getRecordType() {
        return GameMafiaLog.class;
    }
}
