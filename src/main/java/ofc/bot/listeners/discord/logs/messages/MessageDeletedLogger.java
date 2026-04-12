package ofc.bot.listeners.discord.logs.messages;

import net.dv8tion.jda.api.audit.ActionType;
import net.dv8tion.jda.api.audit.AuditLogEntry;
import net.dv8tion.jda.api.events.message.MessageDeleteEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.utils.TimeUtil;
import ofc.bot.domain.entity.MessageVersion;
import ofc.bot.domain.sqlite.repository.MessageVersionRepository;
import ofc.bot.util.Bot;
import ofc.bot.util.content.annotations.listeners.DiscordEventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@DiscordEventHandler
public class MessageDeletedLogger extends ListenerAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(MessageDeletedLogger.class);
    private static final ExecutorService EXECUTOR = Executors.newVirtualThreadPerTaskExecutor();
    private static final int AUDIT_LOG_MAX_DAYS = 45;

    private final MessageVersionRepository msgVrsRepo;

    public MessageDeletedLogger(MessageVersionRepository msgVrsRepo) {
        this.msgVrsRepo = msgVrsRepo;
    }

    @Override
    public void onMessageDelete(MessageDeleteEvent e) {
        if (!e.isFromGuild()) return;

        CompletableFuture.runAsync(() -> handleDelete(e), EXECUTOR)
                .exceptionally(err -> {
                    LOGGER.error("Failed to process message deletion for ID {}", e.getMessageIdLong(), err);
                    return null;
                });
    }

    private void handleDelete(MessageDeleteEvent e) {
        long start = System.currentTimeMillis();
        long now = Bot.nowMillis();
        long messageId = e.getMessageIdLong();
        long channelId = e.getChannel().getIdLong();

        if (!msgVrsRepo.existsByMessageId(messageId)) return;

        resolveDeletedBy(e, messageId, now)
                .exceptionally(err -> {
                    LOGGER.warn("Could not resolve audit log entry for deleted message {}", messageId, err);
                    return null;
                })
                .thenAccept(deletedById -> {
                    saveDeletionVersion(messageId, channelId, now, deletedById);

                    long elapsed = System.currentTimeMillis() - start;
                    LOGGER.info("Processed message deletion for ID {}, took {}ms", messageId, elapsed);
                })
                .join();
    }

    private CompletableFuture<Long> resolveDeletedBy(MessageDeleteEvent e, long messageId, long now) {
        if (isOlderThanAuditLogWindow(messageId, now)) {
            return CompletableFuture.completedFuture(null);
        }

        return fetchLatestMessageDeleteAuditEntry(e)
                .thenApply(entry -> resolveDeletedByFromAuditEntry(messageId, entry));
    }

    private boolean isOlderThanAuditLogWindow(long messageId, long now) {
        OffsetDateTime msgCreateTime = TimeUtil.getTimeCreated(messageId);
        OffsetDateTime oldestRelevantAuditLogTime = Instant.ofEpochMilli(now)
                .atOffset(ZoneOffset.UTC)
                .minusDays(AUDIT_LOG_MAX_DAYS);

        return msgCreateTime.isBefore(oldestRelevantAuditLogTime);
    }

    private CompletableFuture<AuditLogEntry> fetchLatestMessageDeleteAuditEntry(MessageDeleteEvent e) {
        CompletableFuture<AuditLogEntry> future = new CompletableFuture<>();

        e.getGuild().retrieveAuditLogs()
                .limit(1)
                .type(ActionType.MESSAGE_DELETE)
                .queue(
                        entries -> future.complete(entries.isEmpty() ? null : entries.getFirst()),
                        future::completeExceptionally
                );

        return future;
    }

    private Long resolveDeletedByFromAuditEntry(long messageId, AuditLogEntry entry) {
        if (entry == null) return null;

        long targetId = entry.getTargetIdLong();
        boolean matchesAuthor = msgVrsRepo.findsByMessageAndAuthorId(messageId, targetId);

        return matchesAuthor ? entry.getUserIdLong() : null;
    }

    private void saveDeletionVersion(long messageId, long channelId, long now, Long deletedById) {
        MessageVersion version = new MessageVersion()
                .setMessageId(messageId)
                .setAuthorId(0)
                .setChannelId(channelId)
                .setDeleted(true)
                .setDeletedById(deletedById)
                .setTimeCreated(now);

        msgVrsRepo.save(version);
    }
}