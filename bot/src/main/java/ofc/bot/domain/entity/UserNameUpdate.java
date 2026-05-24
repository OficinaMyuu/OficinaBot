package ofc.bot.domain.entity;

import ofc.bot.domain.entity.enums.NameScope;
import ofc.bot.domain.tables.UserNamesUpdatesTable;
import org.jetbrains.annotations.NotNull;

public class UserNameUpdate extends OficinaRecord<UserNameUpdate> {
    private static final UserNamesUpdatesTable USERNAMES_UPDATES = UserNamesUpdatesTable.USERNAMES_UPDATES;

    public UserNameUpdate() {
        super(USERNAMES_UPDATES);
    }

    public UserNameUpdate(
            long userId, long authorId, Long guildId,
            NameScope scope, String oldValue,
            String newValue, long createdAt
    ) {
        this();
        set(USERNAMES_UPDATES.USER_ID, userId);
        set(USERNAMES_UPDATES.GUILD_ID, guildId);
        set(USERNAMES_UPDATES.SCOPE, scope.toString());
        set(USERNAMES_UPDATES.AUTHOR_ID, authorId);
        set(USERNAMES_UPDATES.OLD_VALUE, oldValue);
        set(USERNAMES_UPDATES.NEW_VALUE, newValue);
        set(USERNAMES_UPDATES.CREATED_AT, createdAt);
    }

    public long getId() {
        return getValue(USERNAMES_UPDATES.ID);
    }

    public long getUserId() {
        return get(USERNAMES_UPDATES.USER_ID);
    }

    /**
     * This method will always return {@code 0} if
     * the {@link #getScope()} is not of type {@link NameScope#GUILD_NICK}.
     *
     * @return the guild id of this action.
     * @see #isFromGuild()
     */
    public long getGuildId() {
        Long id = get(USERNAMES_UPDATES.GUILD_ID);
        return id == null ? 0 : id;
    }

    /**
     * Checks whether this action came from a guild or not.
     * <p>
     * This will always return {@code false} if its not of type {@link NameScope#GUILD_NICK}.
     * <p>
     * This method simply checks if the value of {@link UserNamesUpdatesTable#GUILD_ID GUILD_ID}
     * is not {@code null}.
     *
     * @return {@code true} if the action came from a guild, {@code false} otherwise.
     */
    public boolean isFromGuild() {
        return get(USERNAMES_UPDATES.GUILD_ID) != null;
    }

    /**
     * This method is to diferentiate between the possible user's names updates
     * on Discord, instead of making a different table for each:
     * {@link NameScope#USERNAME USERNAME}, {@link NameScope#GLOBAL_NAME GLOBAL_NAME}
     * and {@link NameScope#GUILD_NICK GUILD_NICK} name updates, we use this value
     * to separate them.
     */
    public NameScope getScope() {
        String scope = get(USERNAMES_UPDATES.SCOPE);
        return NameScope.findByName(scope);
    }

    /**
     * The author of this action. That is, who changed the {@link #getUserId()}'s name.
     * <p>
     * If this entity is of scope
     * {@link NameScope#USERNAME USERNAME} or {@link NameScope#GLOBAL_NAME GLOBAL_NAME},
     * expect this method to always return the same value as {@link #getUserId()}.
     */
    public long getAuthorId() {
        return get(USERNAMES_UPDATES.AUTHOR_ID);
    }

    public String getOldValue() {
        return get(USERNAMES_UPDATES.OLD_VALUE);
    }

    public String getNewValue() {
        return get(USERNAMES_UPDATES.NEW_VALUE);
    }

    public long getTimeCreated() {
        return get(USERNAMES_UPDATES.CREATED_AT);
    }

    @Override
    public long getLastUpdated() {
        return 0;
    }

    @NotNull
    @Override
    public UserNameUpdate setLastUpdated(long timestamp) {
        return null;
    }
}