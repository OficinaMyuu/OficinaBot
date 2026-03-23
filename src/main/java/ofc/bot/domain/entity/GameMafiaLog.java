package ofc.bot.domain.entity;

import ofc.bot.domain.tables.GameMafiaLogsTable;

public class GameMafiaLog extends OficinaRecord<GameMafiaLog> {
    private static final GameMafiaLogsTable GAME_MAFIA_LOGS = GameMafiaLogsTable.GAME_MAFIA_LOGS;

    public GameMafiaLog() {
        super(GAME_MAFIA_LOGS);
    }

    public int getId() {
        return get(GAME_MAFIA_LOGS.ID);
    }

    public String getMatchId() {
        return get(GAME_MAFIA_LOGS.MATCH_ID);
    }

    public long getGuildId() {
        return get(GAME_MAFIA_LOGS.GUILD_ID);
    }

    public String getActionText() {
        return get(GAME_MAFIA_LOGS.ACTION_TEXT);
    }

    public long getTimeCreated() {
        return get(GAME_MAFIA_LOGS.CREATED_AT);
    }
}
