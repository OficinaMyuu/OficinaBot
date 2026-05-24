package ofc.bot.domain.entity;

import ofc.bot.domain.tables.GamesParticipantsTable;
import ofc.bot.util.Bot;

public class GameParticipant extends OficinaRecord<GameParticipant> {
    private static final GamesParticipantsTable GAMES_PARTICIPANTS = GamesParticipantsTable.GAMES_PARTICIPANTS;

    public GameParticipant() {
        super(GAMES_PARTICIPANTS);
    }

    public GameParticipant(long gameId, long userId, boolean isWinner, long createdAt) {
        this();
        set(GAMES_PARTICIPANTS.GAME_ID, gameId);
        set(GAMES_PARTICIPANTS.USER_ID, userId);
        set(GAMES_PARTICIPANTS.HAS_WON, isWinner);
        set(GAMES_PARTICIPANTS.CREATED_AT, createdAt);
    }

    public GameParticipant(long gameId, long userId, boolean isWinner) {
        this(gameId, userId, isWinner, Bot.unixNow());
    }

    public int getId() {
        return get(GAMES_PARTICIPANTS.ID);
    }

    public long getGameId() {
        return get(GAMES_PARTICIPANTS.GAME_ID);
    }

    public long getUserId() {
        return get(GAMES_PARTICIPANTS.USER_ID);
    }

    public boolean isWinner() {
        return get(GAMES_PARTICIPANTS.HAS_WON);
    }

    public long getTimeCreated() {
        return get(GAMES_PARTICIPANTS.CREATED_AT);
    }
}