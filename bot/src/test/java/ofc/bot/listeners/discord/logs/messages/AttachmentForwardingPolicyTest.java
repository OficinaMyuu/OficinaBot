package ofc.bot.listeners.discord.logs.messages;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AttachmentForwardingPolicyTest {
    private static final long ARCHIVE_CHANNEL_ID = 20L;

    @Test
    void shouldForwardGuildUserMessageWithAttachments() {
        assertTrue(AttachmentForwardingPolicy.shouldForward(
                true,
                false,
                false,
                true,
                ARCHIVE_CHANNEL_ID
        ));
    }

    @Test
    void shouldIgnoreMessagesWithoutAttachments() {
        assertFalse(AttachmentForwardingPolicy.shouldForward(
                true,
                false,
                false,
                false,
                ARCHIVE_CHANNEL_ID
        ));
    }

    @Test
    void shouldIgnoreBotMessages() {
        assertFalse(AttachmentForwardingPolicy.shouldForward(
                true,
                true,
                false,
                true,
                ARCHIVE_CHANNEL_ID
        ));
    }

    @Test
    void shouldIgnoreWebhookMessages() {
        assertFalse(AttachmentForwardingPolicy.shouldForward(
                true,
                false,
                true,
                true,
                ARCHIVE_CHANNEL_ID
        ));
    }

    @Test
    void shouldIgnoreNonGuildMessages() {
        assertFalse(AttachmentForwardingPolicy.shouldForward(
                false,
                false,
                false,
                true,
                ARCHIVE_CHANNEL_ID
        ));
    }

    @Test
    void shouldIgnoreMessagesWhenArchiveChannelIsNotConfigured() {
        assertFalse(AttachmentForwardingPolicy.shouldForward(
                true,
                false,
                false,
                true,
                null
        ));
    }
}
