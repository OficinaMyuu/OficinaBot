package ofc.bot.domain.tables;

import ofc.bot.domain.abstractions.InitializableTable;
import ofc.bot.domain.entity.UserXP;
import org.jetbrains.annotations.NotNull;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Query;

import static ofc.bot.domain.tables.UsersTable.USERS;

public class UsersXPTable extends InitializableTable<UserXP> {
    public static final UsersXPTable USERS_XP = new UsersXPTable();

    public final Field<Integer> ID      = newField("id",         INT.identity(true));
    public final Field<Integer> XP      = newField("xp",         INT.notNull());
    public final Field<Integer> LEVEL   = newField("level",      INT.notNull());
    public final Field<Long> USER_ID    = newField("user_id",    BIGINT.notNull());
    public final Field<Long> CREATED_AT = newField("created_at", BIGINT.notNull());
    public final Field<Long> UPDATED_AT = newField("updated_at", BIGINT.notNull());

    public UsersXPTable() {
        super("users_xp");
    }

    @NotNull
    @Override
    public Class<UserXP> getRecordType() {
        return UserXP.class;
    }

    @Override
    public Query getSchema(@NotNull DSLContext ctx) {
        return ctx.createTableIfNotExists(this)
                .primaryKey(ID)
                .columns(fields())
                .unique(USER_ID)
                .constraints(
                        foreignKey(USER_ID).references(USERS, USERS.ID)
                );
    }
}