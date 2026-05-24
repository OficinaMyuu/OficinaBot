package ofc.bot.domain.tables;

import ofc.bot.domain.abstractions.InitializableTable;
import ofc.bot.domain.entity.UserEconomy;
import org.jetbrains.annotations.NotNull;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Query;
import org.jooq.exception.DataAccessException;

import static ofc.bot.domain.tables.UsersTable.USERS;

public class UsersEconomyTable extends InitializableTable<UserEconomy> {
    public static final UsersEconomyTable USERS_ECONOMY = new UsersEconomyTable();

    public final Field<Long> USER_ID       = newField("user_id",       BIGINT.notNull());
    public final Field<Integer> BANK       = newField("bank",          INT.notNull().defaultValue(0));
    public final Field<Integer> WALLET     = newField("wallet",        INT.notNull().defaultValue(0));
    public final Field<Long> LAST_DAILY_AT = newField("last_daily_at", BIGINT);
    public final Field<Long> LAST_WORK_AT  = newField("last_work_at",  BIGINT);
    public final Field<Long> CREATED_AT    = newField("created_at",    BIGINT.notNull());
    public final Field<Long> UPDATED_AT    = newField("updated_at",    BIGINT.notNull());

    public UsersEconomyTable() {
        super("users_economy");
    }

    @NotNull
    @Override
    public Class<UserEconomy> getRecordType() {
        return UserEconomy.class;
    }

    @Override
    public Query getSchema(@NotNull DSLContext ctx) throws DataAccessException {
        return ctx.createTableIfNotExists(this)
                .primaryKey(USER_ID)
                .columns(fields())
                .check(WALLET.ge(0).and(WALLET.le(Integer.MAX_VALUE)))
                .check(BANK.ge(Integer.MIN_VALUE).and(BANK.le(Integer.MAX_VALUE)))
                .constraint(
                        foreignKey(USER_ID).references(USERS, USERS.ID)
                );
    }
}