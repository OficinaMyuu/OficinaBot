package ofc.bot.domain.sqlite.repository;

import ofc.bot.domain.abstractions.InitializableTable;
import ofc.bot.domain.entity.MentionLog;
import ofc.bot.domain.tables.MentionsLogTable;
import org.jetbrains.annotations.NotNull;
import org.jooq.DSLContext;

/**
 * Repository for {@link MentionLog} entity.
 */
public class MentionLogRepository extends Repository<MentionLog> {
    private static final MentionsLogTable MENTIONS_LOG = MentionsLogTable.MENTIONS_LOG;

    public MentionLogRepository(@NotNull DSLContext ctx) {
        super(ctx);
    }

    @NotNull
    @Override
    public InitializableTable<MentionLog> getTable() {
        return MENTIONS_LOG;
    }
}