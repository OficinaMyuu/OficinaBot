package ofc.bot.domain.sqlite.repository;

import ofc.bot.domain.abstractions.InitializableTable;
import ofc.bot.domain.entity.MessageVersion;
import ofc.bot.domain.tables.MessagesVersionsTable;
import org.jetbrains.annotations.NotNull;
import org.jooq.DSLContext;

import java.util.List;
import java.util.Set;

/**
 * Repository for {@link MessageVersion} entity.
 */
public class MessageVersionRepository extends Repository<MessageVersion> {
    private static final MessagesVersionsTable MESSAGES_VERSIONS = MessagesVersionsTable.MESSAGE_VERSIONS;

    public MessageVersionRepository(DSLContext ctx) {
        super(ctx);
    }

    @NotNull
    @Override
    public InitializableTable<MessageVersion> getTable() {
        return MESSAGES_VERSIONS;
    }

    /**
     * Gets the IDs of the users who talked in the given channel.
     *
     * @return A {@link Set Set&lt;Long&gt;} containing the ID of every user
     *         who sent at least one message in this channel.
     */
    public Set<Long> findUsersByChannelId(long chanId) {
        return ctx.selectDistinct(MESSAGES_VERSIONS.AUTHOR_ID)
                .from(MESSAGES_VERSIONS)
                .where(MESSAGES_VERSIONS.CHANNEL_ID.eq(chanId))
                .and(MESSAGES_VERSIONS.AUTHOR_ID.isNotNull())
                .fetchSet(MESSAGES_VERSIONS.AUTHOR_ID);
    }

    public List<MessageVersion> findLastValid(long chanId) {
        return ctx.selectFrom(MESSAGES_VERSIONS)
                .where(MESSAGES_VERSIONS.CHANNEL_ID.eq(chanId))
                .fetch();
    }

    /**
     * Checks if a given version matches the provided parameters.
     * <p>
     * This method <em>DOES NOT</em> check the {@link MessageVersion#isDeleted() is_deleted} column/field.
     *
     * @return {@code true} if the message exists, {@code false} otherwise.
     */
    public boolean findsByMessageAndAuthorId(long msgId, long authorId) {
        return ctx.fetchExists(MESSAGES_VERSIONS, MESSAGES_VERSIONS.MESSAGE_ID.eq(msgId)
                .and(MESSAGES_VERSIONS.AUTHOR_ID.eq(authorId))
        );
    }

    public MessageVersion findLastById(long msgId) {
        return ctx.selectFrom(MESSAGES_VERSIONS)
                .where(MESSAGES_VERSIONS.MESSAGE_ID.eq(msgId))
                .orderBy(MESSAGES_VERSIONS.CREATED_AT.desc())
                .limit(1)
                .fetchOne();
    }
}