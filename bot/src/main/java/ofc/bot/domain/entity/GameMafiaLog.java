package ofc.bot.domain.entity;

import net.dv8tion.jda.internal.utils.Checks;
import ofc.bot.domain.tables.GameMafiaLogsTable;
import ofc.bot.handlers.games.mafia.enums.MafiaEventType;
import ofc.bot.handlers.games.mafia.enums.MafiaPhase;
import ofc.bot.util.Bot;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Persisted audit entry for one Oficina Dorme event.
 */
public class GameMafiaLog extends OficinaRecord<GameMafiaLog> {
    private static final GameMafiaLogsTable GAME_MAFIA_LOGS = GameMafiaLogsTable.GAME_MAFIA_LOGS;

    public GameMafiaLog() {
        super(GAME_MAFIA_LOGS);
    }

    public GameMafiaLog(@NotNull String matchId, long guildId, @NotNull MafiaEventType eventType, @NotNull String action,
                        @Nullable Long actorUserId, @Nullable Long targetUserId, @Nullable Long channelId,
                        @Nullable MafiaPhase phase, long createdAt) {
        this();
        Checks.notNull(matchId, "Match Id");
        Checks.notNull(eventType, "Event Type");
        Checks.notNull(action, "Action");
        set(GAME_MAFIA_LOGS.MATCH_ID, matchId);
        set(GAME_MAFIA_LOGS.GUILD_ID, guildId);
        set(GAME_MAFIA_LOGS.EVENT_TYPE, eventType.name());
        set(GAME_MAFIA_LOGS.ACTION, action);
        set(GAME_MAFIA_LOGS.ACTOR_USER_ID, actorUserId);
        set(GAME_MAFIA_LOGS.TARGET_USER_ID, targetUserId);
        set(GAME_MAFIA_LOGS.CHANNEL_ID, channelId);
        set(GAME_MAFIA_LOGS.PHASE, phase == null ? null : phase.name());
        set(GAME_MAFIA_LOGS.CREATED_AT, createdAt);
    }

    public GameMafiaLog(@NotNull String matchId, long guildId, @NotNull MafiaEventType eventType, @NotNull String action,
                        @Nullable Long actorUserId, @Nullable Long targetUserId, @Nullable Long channelId,
                        @Nullable MafiaPhase phase) {
        this(matchId, guildId, eventType, action, actorUserId, targetUserId, channelId, phase, Bot.unixNow());
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

    public MafiaEventType getEventType() {
        return MafiaEventType.valueOf(get(GAME_MAFIA_LOGS.EVENT_TYPE));
    }

    public String getAction() {
        return get(GAME_MAFIA_LOGS.ACTION);
    }

    @Nullable
    public Long getActorUserId() {
        return get(GAME_MAFIA_LOGS.ACTOR_USER_ID);
    }

    @Nullable
    public Long getTargetUserId() {
        return get(GAME_MAFIA_LOGS.TARGET_USER_ID);
    }

    @Nullable
    public Long getChannelId() {
        return get(GAME_MAFIA_LOGS.CHANNEL_ID);
    }

    @Nullable
    public MafiaPhase getPhase() {
        String phase = get(GAME_MAFIA_LOGS.PHASE);
        return phase == null ? null : MafiaPhase.valueOf(phase);
    }

    public long getTimeCreated() {
        return get(GAME_MAFIA_LOGS.CREATED_AT);
    }
}
