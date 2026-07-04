package ofc.bot.domain.tables;

import ofc.bot.domain.abstractions.InitializableTable;
import ofc.bot.domain.entity.BlockedWord;
import org.jetbrains.annotations.NotNull;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Query;
import org.jooq.impl.SQLDataType;

public class BlockedWordsTable extends InitializableTable<BlockedWord> {
    public static final BlockedWordsTable BLOCKED_WORDS = new BlockedWordsTable();

    public final Field<Integer> ID          = newField("id",          SQLDataType.INTEGER.identity(true));
    public final Field<Long> GUILD_ID       = newField("guild_id",    SQLDataType.BIGINT.notNull());
    public final Field<String> WORD         = newField("word",        SQLDataType.VARCHAR(255).notNull());
    public final Field<Boolean> SEVERE      = newField("severe",      SQLDataType.BOOLEAN.notNull());
    public final Field<Boolean> MATCH_EXACT = newField("match_exact", SQLDataType.BOOLEAN.notNull());
    public final Field<Long> CREATED_AT     = newField("created_at",  SQLDataType.BIGINT.notNull());
    public final Field<Long> UPDATED_AT     = newField("updated_at",  SQLDataType.BIGINT.notNull());

    public BlockedWordsTable() {
        super("blocked_words");
    }

    @NotNull
    @Override
    public Class<BlockedWord> getRecordType() {
        return BlockedWord.class;
    }

    @Override
    public Query getSchema(@NotNull DSLContext ctx) {
        return ctx.createTableIfNotExists(this)
                .primaryKey(ID)
                .columns(fields());
    }
}