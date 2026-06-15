package ofc.bot.listeners.discord.guilds.messages;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class CoinflipInferenceHandlerTest {
    private static final long CHANNEL_ID = 10L;

    private final CoinflipInferenceHandler handler = new CoinflipInferenceHandler(new Random(0));

    @Test
    void shouldRespectCooldownForTwoNonStaffUsers() {
        long now = 1_000L;

        assertNull(handler.handleGuess(CHANNEL_ID, 1L, false, true, now));
        assertNotNull(handler.handleGuess(CHANNEL_ID, 2L, false, false, now + 1));

        assertNull(handler.handleGuess(CHANNEL_ID, 3L, false, true, now + 2));
        assertNull(handler.handleGuess(CHANNEL_ID, 4L, false, false, now + 3));
    }

    @Test
    void shouldIgnoreCooldownWhenPendingUserIsStaff() {
        long now = 10_000L;

        assertNull(handler.handleGuess(CHANNEL_ID, 1L, false, true, now));
        assertNotNull(handler.handleGuess(CHANNEL_ID, 2L, false, false, now + 1));

        assertNull(handler.handleGuess(CHANNEL_ID, 3L, true, true, now + 2));

        CoinflipInferenceHandler.CoinflipResult result = handler.handleGuess(CHANNEL_ID, 4L, false, false, now + 3);

        assertNotNull(result);
        assertEquals(3L, result.firstUserId());
        assertEquals(4L, result.secondUserId());
    }

    @Test
    void shouldIgnoreCooldownWhenCurrentUserIsStaff() {
        long now = 20_000L;

        assertNull(handler.handleGuess(CHANNEL_ID, 1L, false, true, now));
        assertNotNull(handler.handleGuess(CHANNEL_ID, 2L, false, false, now + 1));

        assertNull(handler.handleGuess(CHANNEL_ID, 3L, false, true, now + 2));

        CoinflipInferenceHandler.CoinflipResult result = handler.handleGuess(CHANNEL_ID, 4L, true, false, now + 3);

        assertNotNull(result);
        assertEquals(3L, result.firstUserId());
        assertEquals(4L, result.secondUserId());
    }

    @Test
    void shouldMatchConfiguredBannedChannelId() {
        assertTrue(CoinflipInferenceHandler.isConfiguredChannelId(
                new String[]{"1", Long.toString(CHANNEL_ID), "999"},
                CHANNEL_ID
        ));
    }

    @Test
    void shouldTrimConfiguredBannedChannelIds() {
        assertTrue(CoinflipInferenceHandler.isConfiguredChannelId(
                new String[]{"  " + CHANNEL_ID + "  "},
                CHANNEL_ID
        ));
    }

    @Test
    void shouldIgnoreDifferentConfiguredChannelIds() {
        assertFalse(CoinflipInferenceHandler.isConfiguredChannelId(
                new String[]{"1", "999"},
                CHANNEL_ID
        ));
    }
}
