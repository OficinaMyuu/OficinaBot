package ofc.bot.domain.entity;

import net.dv8tion.jda.internal.utils.Checks;
import ofc.bot.domain.tables.BetGamesTable;
import ofc.bot.handlers.games.GameStatus;
import ofc.bot.handlers.games.GameType;
import ofc.bot.util.Bot;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class BetGame extends OficinaRecord<BetGame> {
    private static final BetGamesTable BET_GAMES = BetGamesTable.BET_GAMES;

    public BetGame() {
        super(BET_GAMES);
    }

    public BetGame(long id, @NotNull GameStatus status, @Nullable String board,
                   @NotNull GameType type, long startedAt, long endedAt, long createdAt) {
        this();
        Checks.notNull(status, "Bet Status");
        Checks.notNull(type, "Bet Type");

        set(BET_GAMES.ID, id);
        set(BET_GAMES.STATUS, status.name());
        set(BET_GAMES.BOARD, board);
        set(BET_GAMES.BET_TYPE, type.name());
        set(BET_GAMES.STARTED_AT, startedAt);
        set(BET_GAMES.ENDED_AT, endedAt);
        set(BET_GAMES.CREATED_AT, createdAt);
    }

    public BetGame(long id, @NotNull GameStatus status, @Nullable String board,
                   @NotNull GameType type, long startedAt, long endedAt) {
        this(id, status, board, type, startedAt, endedAt, Bot.unixNow());
    }

    public long getId() {
        return get(BET_GAMES.ID);
    }

    public GameStatus getStatus() {
        String status = get(BET_GAMES.STATUS);
        return GameStatus.valueOf(status);
    }

    public String getBoard() {
        return get(BET_GAMES.BOARD);
    }

    public GameType getType() {
        String type = get(BET_GAMES.BET_TYPE);
        return GameType.valueOf(type);
    }

    public long getTimeStarted() {
        return get(BET_GAMES.STARTED_AT);
    }

    public long getTimeEnded() {
        return get(BET_GAMES.ENDED_AT);
    }

    public long getTimeCreated() {
        return get(BET_GAMES.CREATED_AT);
    }
}