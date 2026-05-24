package ofc.bot.domain.sqlite.repository;

import ofc.bot.domain.abstractions.InitializableTable;
import ofc.bot.domain.entity.BlockedWord;
import ofc.bot.domain.tables.BlockedWordsTable;
import org.jetbrains.annotations.NotNull;
import org.jooq.DSLContext;

/**
 * Repository for {@link BlockedWord} entity.
 */
public class BlockedWordRepository extends Repository<BlockedWord> {
    private static final BlockedWordsTable BLOCKED_WORDS = BlockedWordsTable.BLOCKED_WORDS;

    public BlockedWordRepository(DSLContext ctx) {
        super(ctx);
    }

    @NotNull
    @Override
    public InitializableTable<BlockedWord> getTable() {
        return BLOCKED_WORDS;
    }
}