package ofc.bot.handlers.games.mafia.models;

import ofc.bot.handlers.games.mafia.enums.MatchPhase;

import java.util.*;

public class MatchState {
    public static final int MIN_PLAYERS = 10;

    private final String matchId = UUID.randomUUID().toString();
    private final Map<Long, Player> players = new HashMap<>();
    private final long guildId;
    private final long mainChannelId;
    private final long hostId;
    private final int maxPlayers;

    private MatchPhase phase = MatchPhase.LOBBY;

    private long assassinsChannelId;
    private long doctorsChannelId;
    private long detectivesChannelId;

    public MatchState(long guildId, long mainChannelId, long hostId, int maxPlayers) {
        this.guildId = guildId;
        this.mainChannelId = mainChannelId;
        this.hostId = hostId;
        this.maxPlayers = maxPlayers < MIN_PLAYERS ? -1 : maxPlayers;
    }

    public String getMatchId() {
        return matchId;
    }

    public List<Player> getPlayers() {
        return List.copyOf(this.players.values());
    }

    public Player getPlayer(long id) {
        return this.players.get(id);
    }

    public int getPlayerCount() {
        return this.players.size();
    }

    public boolean hasPlayer(long userId) {
        return this.players.containsKey(userId);
    }

    public long getDetectivesChannelId() {
        return this.detectivesChannelId;
    }

    public long getDoctorsChannelId() {
        return this.doctorsChannelId;
    }

    public long getAssassinsChannelId() {
        return this.assassinsChannelId;
    }

    public long getMainChannelId() {
        return this.mainChannelId;
    }

    public long getHostId() {
        return this.hostId;
    }

    public int getMaxPlayers() {
        return this.maxPlayers;
    }

    public MatchPhase getPhase() {
        return this.phase;
    }

    public long getGuildId() {
        return this.guildId;
    }

    public void addPlayer(Player player) {
        this.players.put(player.getUserId(), player);
    }

    public void removePlayer(long userId) {
        this.players.remove(userId);
    }

    public void setPhase(MatchPhase phase) {
        this.phase = phase;
    }

    public void setAssassinsChannelId(long chanId) {
        this.assassinsChannelId = chanId;
    }

    public void setDoctorsChannelId(long chanId) {
        this.doctorsChannelId = chanId;
    }

    public void setDetectivesChannelId(long chanId) {
        this.detectivesChannelId = chanId;
    }
}