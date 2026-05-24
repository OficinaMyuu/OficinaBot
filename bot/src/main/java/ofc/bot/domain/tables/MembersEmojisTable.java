package ofc.bot.domain.tables;

import ofc.bot.domain.abstractions.InitializableTable;
import ofc.bot.domain.entity.MemberEmoji;
import org.jetbrains.annotations.NotNull;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Query;
import org.jooq.exception.DataAccessException;
import org.jooq.impl.SQLDataType;

public class MembersEmojisTable extends InitializableTable<MemberEmoji> {
    public static final MembersEmojisTable MEMBERS_EMOJIS = new MembersEmojisTable();

    public final Field<Long> USER_ID    = newField("user_id",    SQLDataType.BIGINT.notNull());
    public final Field<String> EMOJI    = newField("emoji",      SQLDataType.CHAR.notNull());
    public final Field<Long> CREATED_AT = newField("created_at", SQLDataType.BIGINT.notNull());
    public final Field<Long> UPDATED_AT = newField("updated_at", SQLDataType.BIGINT.notNull());

    public MembersEmojisTable() {
        super("members_emojis");
    }

    @NotNull
    @Override
    public Class<MemberEmoji> getRecordType() {
        return MemberEmoji.class;
    }

    @Override
    public Query getSchema(@NotNull DSLContext ctx) throws DataAccessException {
        return ctx.createTableIfNotExists(this)
                .primaryKey(USER_ID)
                .columns(fields());
    }
}