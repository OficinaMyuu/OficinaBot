package ofc.bot.listeners.discord.logs.messages;

final class AttachmentForwardingPolicy {
    static final String CHANNEL_CONFIG_KEY = "channels.attachments-log.id";

    private AttachmentForwardingPolicy() {}

    static boolean shouldForward(
            boolean fromGuild,
            boolean authorBot,
            boolean webhookMessage,
            boolean hasAttachments,
            Long archiveChannelId
    ) {
        if (!fromGuild || authorBot || webhookMessage || !hasAttachments) return false;
        return archiveChannelId != null;
    }
}
