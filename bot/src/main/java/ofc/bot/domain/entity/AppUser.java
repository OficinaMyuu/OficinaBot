package ofc.bot.domain.entity;

import net.dv8tion.jda.api.entities.User;
import ofc.bot.domain.tables.UsersTable;
import ofc.bot.util.Bot;
import org.jetbrains.annotations.NotNull;

/**
 * This refers to the {@link UsersTable user} table in the database.
 * <p>
 * The name used is {@code AppUser} to avoid confusion
 * and import collisions with JDA's
 * {@link net.dv8tion.jda.api.entities.User User} interface.
 */
public class AppUser extends OficinaRecord<AppUser> {
    private static final UsersTable USERS = UsersTable.USERS;

    public AppUser() {
        super(USERS);
    }

    public AppUser(long id, String name, String globalName, long createdAt, long updatedAt) {
        this();
        set(USERS.ID, id);
        set(USERS.NAME, name);
        set(USERS.GLOBAL_NAME, globalName);
        set(USERS.CREATED_AT, createdAt);
        set(USERS.UPDATED_AT, updatedAt);
    }

    public static AppUser fromUser(User user) {
        long now = Bot.unixNow();
        return new AppUser(user.getIdLong(), user.getName(), user.getGlobalName(), now, now);
    }

    public long getId() {
        return get(USERS.ID);
    }

    public String getName() {
        return get(USERS.NAME);
    }

    public String getGlobalName() {
        return get(USERS.GLOBAL_NAME);
    }

    public String getDisplayName() {
        String global = getGlobalName();
        return global == null ? getName() : global;
    }

    public long getTimeCreated() {
        return get(USERS.CREATED_AT);
    }

    public long getLastUpdated() {
        return get(USERS.UPDATED_AT);
    }

    public AppUser setId(long id) {
        set(USERS.ID, id);
        return this;
    }

    public AppUser setName(String name) {
        set(USERS.NAME, name);
        return this;
    }

    public AppUser setGlobalName(String globalName) {
        set(USERS.GLOBAL_NAME, globalName);
        return this;
    }

    @NotNull
    public AppUser setLastUpdated(long updatedAt) {
        set(USERS.UPDATED_AT, updatedAt);
        return this;
    }
}