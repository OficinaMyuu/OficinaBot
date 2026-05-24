package ofc.bot.handlers.games;

import java.util.Set;

public interface Game {
    /**
     * Starts the current game.
     *
     * @param args the arguments to be passed to the start command.
     */
    void start(GameArgs args);

    /**
     * Stops this current game.
     * <p>
     * This method should (usually) never throw exceptions,
     * but instead, finish the game gracefully (even though it was a "forced" operation).
     *
     * @param args the arguments to be passed to the end command.
     */
    void end(GameArgs args);

    /**
     * <b>This method is intended to be called exclusively by the instance itself.</b>
     * <p>
     * It should be invoked when a specified period of user inactivity is detected,
     * as defined by the {@link #getTimeout()} value.
     * <p>
     * The implementor must invoke this method when the user(s) fail to interact with the game
     * for a duration that exceeds the specified timeout period.
     */
    void timeout();

    int getTimeout();

    void join(long... userIds);

    GameType getType();

    GameStatus getStatus();


    /**
     * Gets the time (in {@link java.time.Instant#getEpochSecond() Instant.getEpochSecond()} form)
     * of when the game has started.
     *
     * @return The starting time of this game, or {@code 0} if it has not started yet.
     */
    long getTimeStarted();

    /**
     * Gets all the participants of this game.
     *
     * @return A {@link Set Set&lt;Long&gt;} containing the ID of the users.
     */
    Set<Long> getParticipants();

    Set<Long> getWinners();

    /**
     * Gets the maximum amount of users that can join this game.
     *
     * @return The max amount of users allowed to join this game, or {@code 0} if there is no limit.
     */
    int getMaxUsers();

    /**
     * Gets the minimum amount of users that should join this game.
     * <p>
     * It is expected from the implementor to never return negative values.
     *
     * @return The always-positive min amount of users to start this game.
     */
    int getMinUsers();

    default boolean isParticipating(long userId) {
        return getParticipants().contains(userId);
    }

    default boolean hasWinner() {
        return !getWinners().isEmpty();
    }
}