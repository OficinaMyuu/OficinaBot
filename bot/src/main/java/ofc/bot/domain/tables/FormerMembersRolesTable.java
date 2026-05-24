package ofc.bot.domain.tables;

import ofc.bot.domain.abstractions.InitializableTable;
import ofc.bot.domain.entity.FormerMemberRole;
import org.jetbrains.annotations.NotNull;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Query;
import org.jooq.exception.DataAccessException;
import org.jooq.impl.SQLDataType;

public class FormerMembersRolesTable extends InitializableTable<FormerMemberRole> {
    public static final FormerMembersRolesTable FORMER_MEMBERS_ROLES = new FormerMembersRolesTable();

    public final Field<Integer> ID         = newField("id",         SQLDataType.INTEGER.identity(true));
    public final Field<Integer> PRIVILEGED = newField("privileged", SQLDataType.INTEGER.notNull());
    public final Field<Long> USER_ID       = newField("user_id",    SQLDataType.BIGINT.notNull());
    public final Field<Long> GUILD_ID      = newField("guild_id",   SQLDataType.BIGINT.notNull());
    public final Field<Long> ROLE_ID       = newField("role_id",    SQLDataType.BIGINT.notNull());
    public final Field<Long> CREATED_AT    = newField("created_at", SQLDataType.BIGINT.notNull());

    public FormerMembersRolesTable() {
        super("former_members_roles");
    }

    @NotNull
    @Override
    public Class<FormerMemberRole> getRecordType() {
        return FormerMemberRole.class;
    }

    @Override
    public Query getSchema(@NotNull DSLContext ctx) throws DataAccessException {
        return ctx.createTableIfNotExists(this)
                .primaryKey(ID)
                .columns(fields())
                .unique(USER_ID, ROLE_ID);
    }
}