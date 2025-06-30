package ofc.bot.domain.sqlite.repository;

import ofc.bot.domain.abstractions.InitializableTable;
import ofc.bot.domain.entity.DiscordMessage;
import ofc.bot.domain.tables.DiscordMessagesTable;
import ofc.bot.util.Bot;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jooq.DSLContext;

import java.util.List;

/**
 * Repository for {@link DiscordMessage} entity.
 */
public class DiscordMessageRepository extends Repository<DiscordMessage> {
    private static final DiscordMessagesTable DISCORD_MESSAGES = DiscordMessagesTable.DISCORD_MESSAGES;

    public DiscordMessageRepository(DSLContext ctx) {
        super(ctx);
    }

    @NotNull
    @Override
    public InitializableTable<DiscordMessage> getTable() {
        return DISCORD_MESSAGES;
    }

    public DiscordMessage findById(long msgId, DiscordMessage fallback) {
        DiscordMessage msg = findById(msgId);
        return msg == null ? fallback : msg;
    }

    public DiscordMessage findFirstMessageByChannelId(long chanId) {
        return ctx.selectFrom(DISCORD_MESSAGES)
                .where(DISCORD_MESSAGES.CHANNEL_ID.eq(chanId))
                .orderBy(DISCORD_MESSAGES.CREATED_AT.asc())
                .limit(1)
                .fetchOne();
    }

    public int countByChannelId(long chanId) {
        return ctx.fetchCount(DISCORD_MESSAGES, DISCORD_MESSAGES.CHANNEL_ID.eq(chanId));
    }

    public boolean existsByMessageAndAuthorId(long msgId, long authorId) {
        return ctx.fetchExists(DISCORD_MESSAGES, DISCORD_MESSAGES.ID.eq(msgId)
                .and(DISCORD_MESSAGES.AUTHOR_ID.eq(authorId))
        );
    }

    public DiscordMessage findById(long msgId) {
        return ctx.selectFrom(DISCORD_MESSAGES)
                .where(DISCORD_MESSAGES.ID.eq(msgId))
                .fetchOne();
    }

    /**
     * This is not the same as calling {@link #softDeleteByIdAndAuthor(long, Long)}
     * multiple times in a loop.
     * <p>
     * This method is far more efficient by deleting all records with the given ids
     * in a single query.
     *
     * @param ids the id of the messages to be deleted.
     * @param deletionAuthorId the author of the deletion of these messages.
     */
    public void softDeleteByIdsAndAuthor(@NotNull List<Long> ids, long deletionAuthorId) {
        ctx.update(DISCORD_MESSAGES)
                .set(DISCORD_MESSAGES.DELETED, true)
                .set(DISCORD_MESSAGES.DEL_AUTHOR_ID, deletionAuthorId)
                .set(DISCORD_MESSAGES.UPDATED_AT, Bot.unixNow())
                .where(DISCORD_MESSAGES.ID.in(ids))
                .execute();
    }

    public void softDeleteByIdAndAuthor(long msgId, @Nullable Long deletionAuthorId) {
        ctx.update(DISCORD_MESSAGES)
                .set(DISCORD_MESSAGES.DELETED, true)
                .set(DISCORD_MESSAGES.DEL_AUTHOR_ID, deletionAuthorId)
                .set(DISCORD_MESSAGES.UPDATED_AT, Bot.unixNow())
                .where(DISCORD_MESSAGES.ID.eq(msgId))
                .execute();
    }
}