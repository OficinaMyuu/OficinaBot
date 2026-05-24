package ofc.bot.domain.tables;

import ofc.bot.domain.abstractions.InitializableTable;
import ofc.bot.domain.entity.MemberPunishment;
import org.jetbrains.annotations.NotNull;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Query;
import org.jooq.exception.DataAccessException;
import org.jooq.impl.SQLDataType;

import static ofc.bot.domain.tables.UsersTable.USERS;

public class MembersPunishmentsTable extends InitializableTable<MemberPunishment> {
    public static final MembersPunishmentsTable MEMBERS_PUNISHMENTS = new MembersPunishmentsTable();

    public final Field<Integer> ID              = newField("id",                 SQLDataType.INTEGER.identity(true));
    public final Field<Long> GUILD_ID           = newField("guild_id",           SQLDataType.BIGINT.notNull());
    public final Field<Long> USER_ID            = newField("user_id",            SQLDataType.BIGINT.notNull());
    public final Field<Long> MODERATOR_ID       = newField("moderator_id",       SQLDataType.BIGINT.notNull());
    public final Field<String> REASON           = newField("reason",             SQLDataType.CHAR.notNull());
    public final Field<Boolean> ACTIVE          = newField("active",             SQLDataType.BOOLEAN.notNull());
    public final Field<Long> DELETION_AUTHOR_ID = newField("deletion_author_id", SQLDataType.BIGINT);
    public final Field<Long> CREATED_AT         = newField("created_at",         SQLDataType.BIGINT.notNull());
    public final Field<Long> UPDATED_AT         = newField("updated_at",         SQLDataType.BIGINT.notNull());

    public MembersPunishmentsTable() {
        super("members_punishments");
    }

    @NotNull
    @Override
    public Class<MemberPunishment> getRecordType() {
        return MemberPunishment.class;
    }

    @Override
    public Query getSchema(@NotNull DSLContext ctx) throws DataAccessException {
        return ctx.createTableIfNotExists(this)
                .primaryKey(ID)
                .columns(fields())
                .constraints(
                        foreignKey(USER_ID).references(USERS, USERS.ID),
                        foreignKey(MODERATOR_ID).references(USERS, USERS.ID),
                        foreignKey(DELETION_AUTHOR_ID).references(USERS, USERS.ID)
                );
    }
}