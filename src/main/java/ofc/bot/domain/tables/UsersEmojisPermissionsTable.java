package ofc.bot.domain.tables;

import ofc.bot.domain.abstractions.InitializableTable;
import ofc.bot.domain.entity.UserEmojiPermission;
import org.jetbrains.annotations.NotNull;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Query;

public class UsersEmojisPermissionsTable extends InitializableTable<UserEmojiPermission> {
    public static final UsersEmojisPermissionsTable USERS_EMOJIS_PERMS = new UsersEmojisPermissionsTable();

    public final Field<Integer> ID      = newField("id",         INT.identity(true));
    public final Field<Long> AUTHOR_ID  = newField("author_id",  BIGINT.notNull());
    public final Field<Long> USER_ID    = newField("user_id",    BIGINT.notNull());
    public final Field<String> EMOJI    = newField("emoji",      CHAR.notNull());
    public final Field<Long> CREATED_AT = newField("created_at", BIGINT.notNull());

    public UsersEmojisPermissionsTable() {
        super("users_emojis_permissions");
    }

    @Override
    public Query getSchema(@NotNull DSLContext ctx) {
        return ctx.createTableIfNotExists(this)
                .primaryKey(ID)
                .columns(fields());
    }

    @NotNull
    @Override
    public Class<UserEmojiPermission> getRecordType() {
        return UserEmojiPermission.class;
    }
}