package ofc.bot.domain.sqlite.repository;

import ofc.bot.domain.abstractions.InitializableTable;
import ofc.bot.domain.entity.MessageTranscription;
import ofc.bot.domain.tables.MessagesTranscriptionsTable;
import ofc.bot.util.Bot;
import org.jetbrains.annotations.NotNull;
import org.jooq.DSLContext;

/**
 * Repository for {@link MessageTranscription} entity.
 */
public class MessageTranscriptionRepository extends Repository<MessageTranscription> {
    private static final MessagesTranscriptionsTable MESSAGES_TRANSCRIPTIONS = MessagesTranscriptionsTable.MESSAGES_TRANSCRIPTIONS;

    public MessageTranscriptionRepository(DSLContext ctx) {
        super(ctx);
    }

    @NotNull
    @Override
    public InitializableTable<MessageTranscription> getTable() {
        return MESSAGES_TRANSCRIPTIONS;
    }

    public int countDailyTranscriptionsByUserId(long userId) {
        long startOfDay = Bot.unixMidnightNow();
        return ctx.fetchCount(MESSAGES_TRANSCRIPTIONS,
                MESSAGES_TRANSCRIPTIONS.REQUESTER_ID.eq(userId)
                        .and(MESSAGES_TRANSCRIPTIONS.CREATED_AT.ge(startOfDay))
        );
    }

    public MessageTranscription findByMessageId(long messageId) {
        return ctx.selectFrom(MESSAGES_TRANSCRIPTIONS)
                .where(MESSAGES_TRANSCRIPTIONS.MESSAGE_ID.eq(messageId))
                .fetchOne();
    }
}