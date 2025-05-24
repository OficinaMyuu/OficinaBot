package ofc.bot.domain.tables;

import ofc.bot.domain.abstractions.InitializableTable;
import ofc.bot.domain.entity.UserSubscription;
import org.jetbrains.annotations.NotNull;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Query;

public class UsersSubscriptionsTable extends InitializableTable<UserSubscription> {
    public static final UsersSubscriptionsTable USERS_SUBSCRIPTIONS = new UsersSubscriptionsTable();

    public final Field<Integer> ID      = newField("id",                INT.identity(true));
    public final Field<Long> USER_ID    = newField("user_id",           BIGINT.notNull());
    public final Field<String> SUB_TYPE = newField("subscription_type", CHAR.notNull());
    public final Field<Long> CREATED_AT = newField("created_at",        BIGINT.notNull());
    public final Field<Long> UPDATED_AT = newField("updated_at",        BIGINT.notNull());

    public UsersSubscriptionsTable() {
        super("users_subscriptions");
    }

    @Override
    public Query getSchema(@NotNull DSLContext ctx) {
        return ctx.createTableIfNotExists(this)
                .primaryKey(ID)
                .columns(fields())
                .unique(USER_ID, SUB_TYPE);
    }

    @NotNull
    @Override
    public Class<UserSubscription> getRecordType() {
        return UserSubscription.class;
    }
}