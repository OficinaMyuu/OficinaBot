package ofc.bot.handlers.games.betting;

import ofc.bot.handlers.games.Game;
import ofc.bot.handlers.games.GameArgs;
import ofc.bot.handlers.games.GameType;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public interface Bet<T> extends Game {

    /**
     * Allows a user to join the game by providing their bet or guess for the outcome.
     * <p>
     * This method ensures that the user is added to the game with a specific bet/role,
     * which is required for games that involve user predictions or wagers.
     * <p>
     * For instance, if this is an instance of a {@link GameType#TIC_TAC_TOE} game,
     * you can use this method as follows:
     * <pre>
     *   {@code
     * Bet<Character> game = new TicTacToeGame(ecoRepo, betRepo, betUsersRepo, api, 200);
     * game.join(123, 'X');
     * game.join(456, 'O');
     *
     * game.start(msg);
     *   }
     * </pre>
     * <p>
     * <b>Note:</b> This method will likely not enforce a limit on the number of users who can join.
     * This is only checked when calling {@link #start(GameArgs)}.
     * It is the developer's responsability to check the number of participants joining the game
     * over the {@link #getMaxUsers()}
     *
     * @param userId The ID of the user joining the game.
     * @param bet The user's bet, role or guess for the outcome of the game.
     * @throws IllegalArgumentException If the bet is {@code null} or invalid for the game type.
     * @throws IllegalStateException If the game is not accepting new participants.
     */
    void join(long userId, @NotNull T bet);

    /**
     * Just a shortcut for {@link #join(long, Object)}.
     *
     * @param users A {@link Map Map&lt;Long, T&gt;} containing the user IDs and bets.
     */
    default void join(@NotNull Map<Long, T> users) {
        users.forEach(this::join);
    }
}