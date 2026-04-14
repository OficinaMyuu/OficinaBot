package ofc.bot.handlers.games.mafia.domain;

import ofc.bot.handlers.games.mafia.enums.MafiaRole;
import org.jetbrains.annotations.Nullable;

/**
 * Represents one participant inside an Oficina Dorme match.
 * <p>
 * Runtime state is keyed only by Discord user id so the match can continue to reason about a player even if the
 * backing {@code Member} object becomes unavailable.
 */
public class MafiaPlayer {
    private final long userId;
    private MafiaRole role;
    private boolean alive;
    /**
     * Stores the last night target used for the consecutive-night restriction.
     * <p>
     * This field only matters for doctors and detectives. It is not used for villagers, assassin kills,
     * or any day vote state.
     */
    private Long previousNightTargetId;

    /**
     * Creates a new alive participant with no role assigned yet.
     *
     * @param userId Discord user id represented by this player
     */
    public MafiaPlayer(long userId) {
        this.userId = userId;
        this.alive = true;
    }

    /**
     * Returns the Discord user id represented by this player.
     *
     * @return Discord user id
     */
    public long getUserId() {
        return userId;
    }

    /**
     * Returns the mention string for this player.
     *
     * @return formatted Discord mention
     */
    public String getMention() {
        return "<@" + userId + ">";
    }

    /**
     * Returns the role assigned to the player for this match.
     *
     * @return assigned role, or {@code null} before the match starts
     */
    public MafiaRole getRole() {
        return role;
    }

    /**
     * Assigns the role for this match.
     *
     * @param role role assigned at match start
     */
    public void setRole(MafiaRole role) {
        this.role = role;
    }

    /**
     * Indicates whether the player is still alive in gameplay terms.
     *
     * @return {@code true} when the player can still act and vote
     */
    public boolean isAlive() {
        return alive;
    }

    /**
     * Updates the alive state.
     *
     * @param alive whether the player remains alive in the match
     */
    public void setAlive(boolean alive) {
        this.alive = alive;
    }

    /**
     * Returns the previous night target for the doctor/detective consecutive-night restriction.
     *
     * @return previous target id, or {@code null} when there is none
     */
    @Nullable
    public Long getPreviousNightTargetId() {
        return previousNightTargetId;
    }

    /**
     * Stores the previous night target for the doctor/detective consecutive-night restriction.
     *
     * @param previousNightTargetId previous target id, or {@code null} to clear it
     */
    public void setPreviousNightTargetId(Long previousNightTargetId) {
        this.previousNightTargetId = previousNightTargetId;
    }
}
