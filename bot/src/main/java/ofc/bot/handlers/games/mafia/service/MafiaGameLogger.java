package ofc.bot.handlers.games.mafia.service;

import ofc.bot.domain.entity.GameMafiaLog;
import ofc.bot.domain.database.repository.GameMafiaLogRepository;
import ofc.bot.domain.database.repository.Repositories;
import ofc.bot.handlers.games.mafia.domain.MafiaMatch;
import ofc.bot.handlers.games.mafia.enums.MafiaEventType;
import ofc.bot.handlers.games.mafia.enums.MafiaPhase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Persists structured audit entries for Oficina Dorme matches.
 */
public final class MafiaGameLogger {
    private static final Logger LOGGER = LoggerFactory.getLogger(MafiaGameLogger.class);
    private static final MafiaGameLogger INSTANCE = new MafiaGameLogger();

    private MafiaGameLogger() {}

    public static MafiaGameLogger getInstance() {
        return INSTANCE;
    }

    public void log(@NotNull MafiaMatch match, @NotNull MafiaEventType eventType, @NotNull String action,
                    @Nullable Long actorUserId, @Nullable Long targetUserId, @Nullable Long channelId) {
        log(match.getMatchId(), match.getGuildId(), match.getPhase(), eventType, action, actorUserId, targetUserId, channelId);
    }

    public void log(@NotNull String matchId, long guildId, @Nullable MafiaPhase phase, @NotNull MafiaEventType eventType,
                    @NotNull String action, @Nullable Long actorUserId, @Nullable Long targetUserId,
                    @Nullable Long channelId) {
        try {
            getRepository().save(new GameMafiaLog(
                    matchId,
                    guildId,
                    eventType,
                    action,
                    actorUserId,
                    targetUserId,
                    channelId,
                    phase
            ));
        } catch (Exception exception) {
            LOGGER.error("Failed to persist mafia log for match {}", matchId, exception);
        }
    }

    /**
     * Returns all persisted audit entries for a match.
     *
     * @param matchId match id to export
     * @return ordered audit entries, or an empty list when loading fails
     */
    public java.util.List<GameMafiaLog> findLogs(@NotNull String matchId) {
        try {
            return getRepository().findAllByMatchId(matchId);
        } catch (Exception exception) {
            LOGGER.error("Failed to fetch mafia logs for match {}", matchId, exception);
            return java.util.List.of();
        }
    }

    private GameMafiaLogRepository getRepository() {
        return Repositories.getGameMafiaLogRepository();
    }
}
