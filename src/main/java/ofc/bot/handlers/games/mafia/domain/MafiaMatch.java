package ofc.bot.handlers.games.mafia.domain;

import net.dv8tion.jda.api.components.selections.SelectMenu;
import ofc.bot.handlers.games.mafia.enums.MafiaPhase;
import ofc.bot.handlers.games.mafia.enums.MafiaRole;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Mutable in-memory state for one Oficina Dorme match.
 * <p>
 * A match is scoped to one main event text channel and may also own private threads for assassins, doctors,
 * and detectives.
 */
public class MafiaMatch {
    /**
     * Minimum amount of players required to start a match.
     */
    public static final int MIN_PLAYERS = 6;
    /**
     * Hard maximum amount of players supported by the current Discord select-menu UX.
     */
    public static final int MAX_PLAYERS = SelectMenu.OPTIONS_MAX_AMOUNT;

    private final String matchId = UUID.randomUUID().toString();
    private final Map<Long, MafiaPlayer> players = new LinkedHashMap<>();
    private final Map<MafiaRole, Long> privateThreadIds = new EnumMap<>(MafiaRole.class);
    private final Map<Long, Long> assassinVotes = new LinkedHashMap<>();
    private final Map<Long, Long> doctorVotes = new LinkedHashMap<>();
    private final Map<Long, Long> detectiveVotes = new LinkedHashMap<>();
    private final Map<Long, Long> dayVotes = new LinkedHashMap<>();
    private final long guildId;
    private final long mainChannelId;
    private final long hostId;
    private final int maxPlayers;
    private final MafiaRoleConfiguration requestedRoleConfiguration;

    private MafiaRoleConfiguration activeRoleConfiguration;
    private MafiaPhase phase = MafiaPhase.LOBBY;
    private long lobbyMessageId;
    private int nightNumber;
    private int dayNumber;

    /**
     * Creates a new lobby-scoped match state.
     *
     * @param guildId guild that owns the match
     * @param mainChannelId main event text channel id
     * @param hostId lobby creator user id
     * @param maxPlayers configured player cap
     * @param requestedRoleConfiguration optional manual role configuration requested by staff
     */
    public MafiaMatch(long guildId, long mainChannelId, long hostId, int maxPlayers,
                      @Nullable MafiaRoleConfiguration requestedRoleConfiguration) {
        this.guildId = guildId;
        this.mainChannelId = mainChannelId;
        this.hostId = hostId;
        this.maxPlayers = Math.min(Math.max(maxPlayers, MIN_PLAYERS), MAX_PLAYERS);
        this.requestedRoleConfiguration = requestedRoleConfiguration;
    }

    /**
     * Returns the opaque runtime identifier for this in-memory match.
     *
     * @return generated match id
     */
    public String getMatchId() {
        return matchId;
    }

    /**
     * Returns the guild id that owns the match.
     *
     * @return guild id
     */
    public long getGuildId() {
        return guildId;
    }

    /**
     * Returns the main village/lobby channel id.
     *
     * @return main text channel id
     */
    public long getMainChannelId() {
        return mainChannelId;
    }

    /**
     * Returns the user id of the lobby host.
     *
     * @return host user id
     */
    public long getHostId() {
        return hostId;
    }

    /**
     * Returns the configured player cap for the lobby.
     *
     * @return maximum allowed players
     */
    public int getMaxPlayers() {
        return maxPlayers;
    }

    /**
     * Returns the optional manual role counts requested when the match was created.
     *
     * @return requested role configuration, or {@code null} when auto-balance should be used
     */
    @Nullable
    public MafiaRoleConfiguration getRequestedRoleConfiguration() {
        return requestedRoleConfiguration;
    }

    /**
     * Returns the resolved role configuration actually applied when the match started.
     *
     * @return active role configuration, or {@code null} before role assignment
     */
    @Nullable
    public MafiaRoleConfiguration getActiveRoleConfiguration() {
        return activeRoleConfiguration;
    }

    /**
     * Stores the resolved role configuration used by the running match.
     *
     * @param activeRoleConfiguration resolved role counts
     */
    public void setActiveRoleConfiguration(MafiaRoleConfiguration activeRoleConfiguration) {
        this.activeRoleConfiguration = activeRoleConfiguration;
    }

    /**
     * Returns the current gameplay phase.
     *
     * @return current match phase
     */
    public MafiaPhase getPhase() {
        return phase;
    }

    /**
     * Updates the current phase directly.
     *
     * @param phase new phase
     */
    public void setPhase(MafiaPhase phase) {
        this.phase = phase;
    }

    /**
     * Enters the night phase, increments the night counter, and clears transient votes from previous rounds.
     */
    public void startNightPhase() {
        phase = MafiaPhase.NIGHT;
        nightNumber++;
        clearNightVotes();
        clearDayVotes();
    }

    /**
     * Enters the day-discussion phase and prepares the next village vote.
     */
    public void startDayDiscussionPhase() {
        phase = MafiaPhase.DAY_DISCUSSION;
        dayNumber++;
        clearDayVotes();
    }

    /**
     * Opens the village vote for the current day.
     */
    public void startDayVotingPhase() {
        phase = MafiaPhase.DAY_VOTING;
        clearDayVotes();
    }

    /**
     * Marks the match as ended.
     */
    public void endMatch() {
        phase = MafiaPhase.ENDED;
    }

    /**
     * Returns the current night number.
     *
     * @return current night count
     */
    public int getNightNumber() {
        return nightNumber;
    }

    /**
     * Returns the current day number.
     *
     * @return current day count
     */
    public int getDayNumber() {
        return dayNumber;
    }

    /**
     * Returns the stored lobby message id used for later embed refreshes.
     *
     * @return lobby message id, or {@code 0} when it has not been recorded yet
     */
    public long getLobbyMessageId() {
        return lobbyMessageId;
    }

    /**
     * Stores the lobby message id used for later embed refreshes.
     *
     * @param lobbyMessageId Discord message id
     */
    public void setLobbyMessageId(long lobbyMessageId) {
        this.lobbyMessageId = lobbyMessageId;
    }

    /**
     * Checks whether the given user is currently part of the roster.
     *
     * @param userId Discord user id
     * @return {@code true} when the user is part of the match
     */
    public boolean hasPlayer(long userId) {
        return players.containsKey(userId);
    }

    /**
     * Adds a participant to the roster when they are not already present.
     *
     * @param userId Discord user id to add
     */
    public void addPlayer(long userId) {
        players.putIfAbsent(userId, new MafiaPlayer(userId));
    }

    /**
     * Removes a participant from the roster and clears any votes cast by them or currently targeting them.
     * <p>
     * This is used both for voluntary lobby exits and for unexpected guild departures.
     *
     * @param userId Discord user id to remove
     * @return {@code true} when a player was actually removed
     */
    public boolean removePlayer(long userId) {
        MafiaPlayer removed = players.remove(userId);
        if (removed == null) {
            return false;
        }

        clearVotesForPlayer(userId);
        return true;
    }

    /**
     * Returns the runtime player state for the given user id.
     *
     * @param userId Discord user id to resolve
     * @return matching player, or {@code null} when the user is not part of the match
     */
    @Nullable
    public MafiaPlayer getPlayer(long userId) {
        return players.get(userId);
    }

    /**
     * Returns the total roster size, alive and dead combined.
     *
     * @return roster size
     */
    public int getPlayerCount() {
        return players.size();
    }

    /**
     * Indicates whether the roster already reached the configured lobby cap.
     *
     * @return {@code true} when the match is full
     */
    public boolean isFull() {
        return getPlayerCount() >= maxPlayers;
    }

    /**
     * Returns an immutable snapshot of all registered players.
     *
     * @return full roster
     */
    public List<MafiaPlayer> getPlayers() {
        return List.copyOf(players.values());
    }

    /**
     * Returns all players that are still alive in gameplay terms.
     *
     * @return alive players
     */
    public List<MafiaPlayer> getAlivePlayers() {
        return players.values().stream()
                .filter(MafiaPlayer::isAlive)
                .toList();
    }

    /**
     * Returns all alive players with the given role.
     *
     * @param role role filter
     * @return alive players with that role
     */
    public List<MafiaPlayer> getAlivePlayersByRole(MafiaRole role) {
        return players.values().stream()
                .filter(MafiaPlayer::isAlive)
                .filter(player -> player.getRole() == role)
                .toList();
    }

    /**
     * Returns how many players are still alive.
     *
     * @return alive player count
     */
    public int getAlivePlayerCount() {
        return (int) players.values().stream().filter(MafiaPlayer::isAlive).count();
    }

    /**
     * Associates a private thread id with one special role.
     *
     * @param role role that owns the thread
     * @param threadId Discord thread id
     */
    public void setPrivateThreadId(MafiaRole role, long threadId) {
        privateThreadIds.put(role, threadId);
    }

    /**
     * Returns the private thread id for the given role.
     *
     * @param role role to resolve
     * @return thread id, or {@code null} when the thread does not exist yet
     */
    @Nullable
    public Long getPrivateThreadId(MafiaRole role) {
        return privateThreadIds.get(role);
    }

    /**
     * Returns all registered private thread ids.
     *
     * @return copy of known thread ids
     */
    public Collection<Long> getPrivateThreadIds() {
        return new ArrayList<>(privateThreadIds.values());
    }

    /**
     * Indicates whether a channel id belongs to this match, either as the main event channel or as a private thread.
     *
     * @param channelId channel id to check
     * @return {@code true} when the channel is part of the match
     */
    public boolean managesChannel(long channelId) {
        return mainChannelId == channelId || privateThreadIds.containsValue(channelId);
    }

    /**
     * Returns assassin night votes keyed by assassin user id.
     *
     * @return mutable assassin vote map
     */
    public Map<Long, Long> getAssassinVotes() {
        return assassinVotes;
    }

    /**
     * Returns doctor night votes keyed by doctor user id.
     *
     * @return mutable doctor vote map
     */
    public Map<Long, Long> getDoctorVotes() {
        return doctorVotes;
    }

    /**
     * Returns detective night votes keyed by detective user id.
     *
     * @return mutable detective vote map
     */
    public Map<Long, Long> getDetectiveVotes() {
        return detectiveVotes;
    }

    /**
     * Returns day votes keyed by voter user id.
     *
     * @return mutable day vote map
     */
    public Map<Long, Long> getDayVotes() {
        return dayVotes;
    }

    /**
     * Clears all night-only vote maps.
     */
    public void clearNightVotes() {
        assassinVotes.clear();
        doctorVotes.clear();
        detectiveVotes.clear();
    }

    /**
     * Clears the day-vote map.
     */
    public void clearDayVotes() {
        dayVotes.clear();
    }

    /**
     * Indicates whether the given user already submitted their current role-specific night action.
     *
     * @param role role performing the action
     * @param userId acting user id
     * @return {@code true} when that action was already recorded
     */
    public boolean hasSubmittedNightVote(MafiaRole role, long userId) {
        return switch (role) {
            case ASSASSIN -> assassinVotes.containsKey(userId);
            case DOCTOR -> doctorVotes.containsKey(userId);
            case DETECTIVE -> detectiveVotes.containsKey(userId);
            case VILLAGER -> false;
        };
    }

    /**
     * Indicates whether the given user already voted during the current day.
     *
     * @param userId voting user id
     * @return {@code true} when a day vote exists for the user
     */
    public boolean hasSubmittedDayVote(long userId) {
        return dayVotes.containsKey(userId);
    }

    /**
     * Checks whether every alive special-role player still present in the match has already submitted their night action.
     *
     * @return {@code true} when the current night can be resolved immediately
     */
    public boolean hasAllNightActionsSubmitted() {
        return assassinVotes.size() == getAlivePlayersByRole(MafiaRole.ASSASSIN).size()
                && doctorVotes.size() == getAlivePlayersByRole(MafiaRole.DOCTOR).size()
                && detectiveVotes.size() == getAlivePlayersByRole(MafiaRole.DETECTIVE).size();
    }

    /**
     * Clears all votes cast by or targeting the removed player.
     *
     * @param userId removed player id
     */
    private void clearVotesForPlayer(long userId) {
        clearVotesForPlayer(assassinVotes, userId);
        clearVotesForPlayer(doctorVotes, userId);
        clearVotesForPlayer(detectiveVotes, userId);
        clearVotesForPlayer(dayVotes, userId);
    }

    /**
     * Removes the player's own vote entry and any vote currently pointing to them.
     *
     * @param votes vote map to sanitize
     * @param userId removed player id
     */
    private void clearVotesForPlayer(Map<Long, Long> votes, long userId) {
        votes.remove(userId);
        votes.entrySet().removeIf(entry -> entry.getValue() == userId);
    }
}
