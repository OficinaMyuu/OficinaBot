package ofc.bot.domain.entity;

import ofc.bot.domain.entity.enums.SubscriptionType;
import ofc.bot.domain.tables.UsersSubscriptionsTable;
import org.jetbrains.annotations.NotNull;

public class UserSubscription extends OficinaRecord<UserSubscription> {
    private static final UsersSubscriptionsTable USERS_SUBSCRIPTIONS = UsersSubscriptionsTable.USERS_SUBSCRIPTIONS;

    public UserSubscription() {
        super(USERS_SUBSCRIPTIONS);
    }

    public UserSubscription(long userId, @NotNull SubscriptionType type, long createdAt, long updatedAt) {
        this();
        set(USERS_SUBSCRIPTIONS.USER_ID, userId);
        set(USERS_SUBSCRIPTIONS.SUB_TYPE, type.name());
        set(USERS_SUBSCRIPTIONS.CREATED_AT, createdAt);
        set(USERS_SUBSCRIPTIONS.UPDATED_AT, updatedAt);
    }

    public int getId() {
        return get(USERS_SUBSCRIPTIONS.ID);
    }

    public SubscriptionType getType() {
        String type = get(USERS_SUBSCRIPTIONS.SUB_TYPE);
        return SubscriptionType.valueOf(type);
    }

    public long getTimeCreated() {
        return get(USERS_SUBSCRIPTIONS.CREATED_AT);
    }

    @Override
    public long getLastUpdated() {
        return get(USERS_SUBSCRIPTIONS.UPDATED_AT);
    }

    public UserSubscription setUserId(long userId) {
        set(USERS_SUBSCRIPTIONS.USER_ID, userId);
        return this;
    }

    public UserSubscription setType(SubscriptionType type) {
        set(USERS_SUBSCRIPTIONS.SUB_TYPE, type.name());
        return this;
    }

    public UserSubscription setTimeCreated(long createdAt) {
        set(USERS_SUBSCRIPTIONS.CREATED_AT, createdAt);
        return this;
    }

    @NotNull
    @Override
    public UserSubscription setLastUpdated(long timestamp) {
        set(USERS_SUBSCRIPTIONS.UPDATED_AT, timestamp);
        return this;
    }
}
