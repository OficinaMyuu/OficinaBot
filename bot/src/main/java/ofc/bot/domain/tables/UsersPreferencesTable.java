package ofc.bot.domain.tables;

import ofc.bot.domain.abstractions.InitializableTable;
import ofc.bot.domain.entity.UserPreference;
import org.jetbrains.annotations.NotNull;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Query;
import org.jooq.exception.DataAccessException;
import org.jooq.impl.SQLDataType;

import static ofc.bot.domain.tables.UsersTable.USERS;

public class UsersPreferencesTable extends InitializableTable<UserPreference> {
    public static final UsersPreferencesTable USERS_PREFERENCES = new UsersPreferencesTable();

    public final Field<Long> USER_ID = newField("user_id", SQLDataType.BIGINT.notNull());
    public final Field<String> LOCALE = newField("locale", SQLDataType.VARCHAR(255));
    public final Field<Boolean> RANKUP_PINGS_ENABLED = newField(
            "rankup_pings_enabled",
            SQLDataType.BOOLEAN.notNull().defaultValue(true)
    );
    public final Field<Long> CREATED_AT = newField("created_at", SQLDataType.BIGINT.notNull());
    public final Field<Long> UPDATED_AT = newField("updated_at", SQLDataType.BIGINT.notNull());

    public UsersPreferencesTable() {
        super("users_preferences");
    }

    @NotNull
    @Override
    public Class<UserPreference> getRecordType() {
        return UserPreference.class;
    }

    @Override
    public Query getSchema(@NotNull DSLContext ctx) throws DataAccessException {
        return ctx.createTableIfNotExists(this)
                .primaryKey(USER_ID)
                .columns(fields())
                .constraint(
                        foreignKey(USER_ID).references(USERS, USERS.ID)
                );
    }
}
