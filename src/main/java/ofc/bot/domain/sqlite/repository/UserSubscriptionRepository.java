package ofc.bot.domain.sqlite.repository;

import ofc.bot.domain.abstractions.InitializableTable;
import ofc.bot.domain.entity.UserSubscription;
import ofc.bot.domain.entity.enums.SubscriptionType;
import ofc.bot.domain.tables.UsersSubscriptionsTable;
import org.jetbrains.annotations.NotNull;
import org.jooq.DSLContext;

import java.util.List;

/**
 * Repository for {@link UserSubscription} entity.
 */
public class UserSubscriptionRepository extends Repository<UserSubscription> {
    private final UsersSubscriptionsTable USERS_SUBSCRIPTIONS = UsersSubscriptionsTable.USERS_SUBSCRIPTIONS;

    public UserSubscriptionRepository(DSLContext ctx) {
        super(ctx);
    }

    @NotNull
    @Override
    public InitializableTable<UserSubscription> getTable() {
        return USERS_SUBSCRIPTIONS;
    }

    public List<UserSubscription> findAllByType(SubscriptionType type) {
        return ctx.selectFrom(USERS_SUBSCRIPTIONS)
                .where(USERS_SUBSCRIPTIONS.SUB_TYPE.eq(type.name()))
                .fetch();
    }

    public UserSubscription findBy(long userId, SubscriptionType type) {
        return ctx.selectFrom(USERS_SUBSCRIPTIONS)
                .where(USERS_SUBSCRIPTIONS.USER_ID.eq(userId))
                .and(USERS_SUBSCRIPTIONS.SUB_TYPE.eq(type.name()))
                .fetchOne();
    }
}