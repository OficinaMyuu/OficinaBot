package ofc.bot.handlers.games.mafia.service;

import ofc.bot.domain.entity.GameMafiaLog;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Formats persisted Oficina Dorme audit rows into a downloadable text transcript.
 */
public final class MafiaLogExporter {
    private static final ZoneId LOG_ZONE = ZoneId.of("America/Sao_Paulo");
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(LOG_ZONE);

    /**
     * Formats all logs for one match.
     *
     * @param matchId exported match id
     * @param logs ordered log rows
     * @return human-readable log transcript
     */
    public String export(@NotNull String matchId, @NotNull List<GameMafiaLog> logs) {
        StringBuilder builder = new StringBuilder();
        builder.append("Oficina Dorme match logs\n");
        builder.append("Match ID: ").append(matchId).append('\n');
        builder.append("Events: ").append(logs.size()).append("\n\n");

        for (GameMafiaLog log : logs) {
            builder.append(formatLog(log));
        }

        return builder.toString();
    }

    /**
     * Builds the download filename for one match.
     *
     * @param matchId match id
     * @return stable filename
     */
    public String fileName(@NotNull String matchId) {
        String shortId = matchId.length() <= 8 ? matchId : matchId.substring(0, 8);
        return "oficina-dorme-" + shortId + "-logs.txt";
    }

    /**
     * Formats one log row with second-precision local time.
     *
     * @param log persisted log row
     * @return transcript line
     */
    private String formatLog(GameMafiaLog log) {
        return "%s | %-22s | phase=%-14s | actor=%s | target=%s | channel=%s | %s%n".formatted(
                TIMESTAMP_FORMATTER.format(Instant.ofEpochSecond(log.getTimeCreated())),
                log.getEventType().name(),
                log.getPhase() == null ? "-" : log.getPhase().name(),
                formatId(log.getActorUserId()),
                formatId(log.getTargetUserId()),
                formatId(log.getChannelId()),
                log.getAction()
        );
    }

    /**
     * Formats nullable Discord ids for the transcript.
     *
     * @param id nullable id
     * @return id or {@code -}
     */
    private String formatId(Long id) {
        return id == null ? "-" : String.valueOf(id);
    }
}
