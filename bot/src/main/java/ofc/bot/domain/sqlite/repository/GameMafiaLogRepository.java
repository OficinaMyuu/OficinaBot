package ofc.bot.domain.sqlite.repository;

import ofc.bot.domain.abstractions.InitializableTable;
import ofc.bot.domain.entity.GameMafiaLog;
import ofc.bot.domain.tables.GameMafiaLogsTable;
import org.jetbrains.annotations.NotNull;
import org.jooq.DSLContext;

import java.util.List;

/**
 * Repository for {@link GameMafiaLog} entries.
 */
public class GameMafiaLogRepository extends Repository<GameMafiaLog> {
    private static final GameMafiaLogsTable GAME_MAFIA_LOGS = GameMafiaLogsTable.GAME_MAFIA_LOGS;

    public GameMafiaLogRepository(@NotNull DSLContext ctx) {
        super(ctx);
    }

    @NotNull
    @Override
    public InitializableTable<GameMafiaLog> getTable() {
        return GAME_MAFIA_LOGS;
    }

    @NotNull
    public List<GameMafiaLog> findAllByMatchId(@NotNull String matchId) {
        return ctx.selectFrom(GAME_MAFIA_LOGS)
                .where(GAME_MAFIA_LOGS.MATCH_ID.eq(matchId))
                .orderBy(GAME_MAFIA_LOGS.CREATED_AT.asc(), GAME_MAFIA_LOGS.ID.asc())
                .fetch();
    }
}
