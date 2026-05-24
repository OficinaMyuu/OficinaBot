package ofc.bot.domain.tables;

import ofc.bot.domain.abstractions.InitializableTable;
import ofc.bot.domain.entity.UserNameUpdate;
import org.jetbrains.annotations.NotNull;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Query;
import org.jooq.exception.DataAccessException;
import org.jooq.impl.SQLDataType;

import static ofc.bot.domain.tables.UsersTable.USERS;

public class UserNamesUpdatesTable extends InitializableTable<UserNameUpdate> {
    public static final UserNamesUpdatesTable USERNAMES_UPDATES = new UserNamesUpdatesTable();

    public final Field<Integer> ID       = newField("id",         SQLDataType.INTEGER.identity(true));
    public final Field<Long> USER_ID     = newField("user_id",    SQLDataType.BIGINT.notNull());
    public final Field<Long> GUILD_ID    = newField("guild_id",   SQLDataType.BIGINT);
    public final Field<String> SCOPE     = newField("scope",      SQLDataType.CHAR.notNull());
    public final Field<Long> AUTHOR_ID   = newField("author_id",  SQLDataType.BIGINT.notNull());
    public final Field<String> OLD_VALUE = newField("old_value",  SQLDataType.CHAR);
    public final Field<String> NEW_VALUE = newField("new_value",  SQLDataType.CHAR);
    public final Field<Long> CREATED_AT  = newField("created_at", SQLDataType.BIGINT.notNull());

    public UserNamesUpdatesTable() {
        super("usernames_updates");
    }

    @NotNull
    @Override
    public Class<UserNameUpdate> getRecordType() {
        return UserNameUpdate.class;
    }

    @Override
    public Query getSchema(@NotNull DSLContext ctx) throws DataAccessException {
        return ctx.createTableIfNotExists(this)
                .primaryKey(ID)
                .columns(fields())
                .constraints(
                        foreignKey(USER_ID).references(USERS, USERS.ID),
                        foreignKey(AUTHOR_ID).references(USERS, USERS.ID)
                );
    }
}