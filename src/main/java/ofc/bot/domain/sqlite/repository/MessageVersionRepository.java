package ofc.bot.domain.sqlite.repository;

import ofc.bot.domain.abstractions.InitializableTable;
import ofc.bot.domain.entity.MessageVersion;
import ofc.bot.domain.tables.MessagesVersionsTable;
import org.jetbrains.annotations.NotNull;
import org.jooq.DSLContext;

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

    public MessageVersion findLastById(long msgId) {
        return ctx.selectFrom(MESSAGES_VERSIONS)
                .where(MESSAGES_VERSIONS.MESSAGE_ID.eq(msgId))
                .orderBy(MESSAGES_VERSIONS.CREATED_AT.desc())
                .limit(1)
                .fetchOne();
    }
}