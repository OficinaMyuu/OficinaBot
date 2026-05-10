package ofc.bot.handlers.games.mafia.service;

import ofc.bot.domain.entity.GameMafiaLog;
import ofc.bot.handlers.games.mafia.enums.MafiaEventType;
import ofc.bot.handlers.games.mafia.enums.MafiaPhase;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MafiaLogExporterTest {
    private final MafiaLogExporter exporter = new MafiaLogExporter();

    @Test
    void shouldExportSecondPrecisionLogRows() {
        GameMafiaLog first = new GameMafiaLog(
                "match-123",
                1L,
                MafiaEventType.LOBBY_CREATED,
                "Lobby created.",
                10L,
                null,
                20L,
                MafiaPhase.LOBBY,
                1_700_000_000L
        );
        GameMafiaLog second = new GameMafiaLog(
                "match-123",
                1L,
                MafiaEventType.GAME_WON,
                "Game finished with winner VILLAGE.",
                null,
                null,
                20L,
                MafiaPhase.ENDED,
                1_700_000_061L
        );

        String output = exporter.export("match-123", List.of(first, second));

        assertTrue(output.contains("Match ID: match-123"));
        assertTrue(output.contains("Events: 2"));
        assertTrue(output.matches("(?s).*\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2} \\| LOBBY_CREATED.*"));
        assertTrue(output.contains("phase=LOBBY"));
        assertTrue(output.contains("actor=10"));
        assertTrue(output.contains("target=-"));
        assertTrue(output.contains("Game finished with winner VILLAGE."));
    }

    @Test
    void shouldCreateStableShortFileName() {
        assertEquals("oficina-dorme-12345678-logs.txt", exporter.fileName("12345678-90ab-cdef"));
    }
}
