package ofc.bot.domain.tables;

import ofc.bot.domain.abstractions.InitializableTable;
import ofc.bot.domain.entity.GameMafiaLog;
import org.jetbrains.annotations.NotNull;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Query;

/**
 * Schema definition for persisted Oficina Dorme audit logs.
 */
public class GameMafiaLogsTable extends InitializableTable<GameMafiaLog> {
    public static final GameMafiaLogsTable GAME_MAFIA_LOGS = new GameMafiaLogsTable();

    public final Field<Integer> ID          = newField("id",             INT.identity(true));
    public final Field<String> MATCH_ID     = newField("match_id",       CHAR.notNull());
    public final Field<Long> GUILD_ID       = newField("guild_id",       BIGINT.notNull());
    public final Field<String> EVENT_TYPE   = newField("event_type",     CHAR.notNull());
    public final Field<String> ACTION       = newField("action",         CHAR.notNull());
    public final Field<Long> ACTOR_USER_ID  = newField("actor_user_id",  BIGINT.nullable(true));
    public final Field<Long> TARGET_USER_ID = newField("target_user_id", BIGINT.nullable(true));
    public final Field<Long> CHANNEL_ID     = newField("channel_id",     BIGINT.nullable(true));
    public final Field<String> PHASE        = newField("phase",          CHAR.nullable(true));
    public final Field<Long> CREATED_AT     = newField("created_at",     BIGINT.notNull());

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
