package ofc.bot.handlers.interactions;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.IPermissionHolder;
import net.dv8tion.jda.internal.utils.Checks;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.TimeUnit;

public abstract class EntityContext<E, C extends EntityContext<E, C>> {
    public static final long DEFAULT_EXPIRE_AFTER_MILLIS = 5 * 60 * 1000; // 5 minutes
    private final Map<String, Object> data;
    private final List<Long> allowedUsers;
    private E entity;
    private String scope;
    private Permission permission;
    private long expireAfterMillis;

    public EntityContext() {
        this.data = new HashMap<>();
        this.allowedUsers = new ArrayList<>();
        this.expireAfterMillis = DEFAULT_EXPIRE_AFTER_MILLIS;
    }

    public EntityContext(@NotNull E entity) {
        this();
        this.entity = entity;
    }

    public abstract C setDisabled(boolean disabled);

    public abstract String getId();

    public final void checkFields() {
        Checks.notNull(this.entity, "entity");
        Checks.notNull(this.scope, "scope");
        Checks.notNull(getId(), "Entity ID");

        Checks.check(this.permission != Permission.UNKNOWN,
                "Permission cannot be of type " + this.permission);
    }

    @SuppressWarnings("unchecked")
    public final C put(String key, Object value) {
        Checks.notNull(key, "Key");
        this.data.put(key, value);
        return (C) this;
    }

    @SuppressWarnings("unchecked")
    public final C putAll(Map<String, Object> values) {
        Checks.notNull(values, "Map");
        values.forEach(this::put);
        return (C) this;
    }

    @SuppressWarnings("unchecked")
    public final C addUser(long userId) {
        Checks.notNull(userId, "userId");
        this.allowedUsers.add(userId);
        return (C) this;
    }

    @SuppressWarnings("unchecked")
    public final C addUsers(@NotNull long... ids) {
        Checks.notNull(ids, "ids");
        for (long id : ids) {
            this.addUser(id);
        }
        return (C) this;
    }

    @SuppressWarnings("unchecked")
    public final C setEntity(@NotNull E entity) {
        Checks.notNull(entity, "entity");
        this.entity = entity;
        return (C) this;
    }

    public C setEnabled(boolean enabled) {
        return setDisabled(!enabled);
    }

    public C disabled() {
        return setDisabled(true);
    }

    public C enabled() {
        return setDisabled(false);
    }

    @SuppressWarnings("unchecked")
    public C setScope(@NotNull String scope) {
        Checks.notNull(scope, "scope");
        this.scope = scope;
        return (C) this;
    }

    @SuppressWarnings("unchecked")
    public C setPermission(@Nullable Permission permission) {
        if (permission == Permission.UNKNOWN)
            throw new IllegalArgumentException("UNKNOWN permission is not allowed for a button context");

        this.permission = permission;
        return (C) this;
    }

    @SuppressWarnings("unchecked")
    public C setValidity(long delay, @NotNull TimeUnit unit) {
        Checks.notNull(unit, "unit");
        this.expireAfterMillis = unit.toMillis(delay);
        return (C) this;
    }

    public final E getEntity() {
        return this.entity;
    }

    @NotNull
    public final String getScope() {
        return this.scope;
    }

    @Nullable
    public final Permission getPermission() {
        return this.permission;
    }

    public final long getValidityMillis() {
        return this.expireAfterMillis;
    }

    public final boolean isPermitted(@NotNull IPermissionHolder entity) {
        if (permission != null && !entity.hasPermission(permission)) return false;
        return allowedUsers.isEmpty() || allowedUsers.contains(entity.getIdLong());
    }

    @SuppressWarnings("unchecked")
    @NotNull
    public final <T> T get(String key) {
        Object obj = this.data.get(key);
        if (obj == null)
            throw new UnsupportedOperationException("No data found for key: " + key);

        return (T) obj;
    }

    @SuppressWarnings("unchecked")
    @Nullable
    public final <T> T find(String key) {
        Object obj = this.data.get(key);
        return (T) obj;
    }

    @SuppressWarnings("unchecked")
    @Contract("_, null -> null")
    public final <T> T getOrDefault(String key, T defaultValue) {
        Object obj = this.data.get(key);
        return obj == null ? defaultValue : (T) obj;
    }
}