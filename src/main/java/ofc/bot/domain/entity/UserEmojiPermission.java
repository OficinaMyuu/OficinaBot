package ofc.bot.domain.entity;

import ofc.bot.domain.tables.UsersEmojisPermissionsTable;
import org.jetbrains.annotations.NotNull;

public class UserEmojiPermission extends OficinaRecord<UserEmojiPermission> {
    public static final UsersEmojisPermissionsTable USERS_EMOJIS_PERMS = UsersEmojisPermissionsTable.USERS_EMOJIS_PERMS;

    public UserEmojiPermission() {
        super(USERS_EMOJIS_PERMS);
    }

    public UserEmojiPermission(long authorId, long userId, @NotNull String emoji, long timeCreated) {
        this();
        setAuthorId(authorId);
        setUserId(userId);
        setEmoji(emoji);
        setTimeCreated(timeCreated);
    }

    public int getId() {
        return get(USERS_EMOJIS_PERMS.ID);
    }

    public long getAuthorId() {
        return get(USERS_EMOJIS_PERMS.AUTHOR_ID);
    }

    public long getUserId() {
        return get(USERS_EMOJIS_PERMS.USER_ID);
    }

    public String getEmoji() {
        return get(USERS_EMOJIS_PERMS.EMOJI);
    }

    public long getTimeCreated() {
        return get(USERS_EMOJIS_PERMS.CREATED_AT);
    }

    public UserEmojiPermission setAuthorId(long authorId) {
        set(USERS_EMOJIS_PERMS.AUTHOR_ID, authorId);
        return this;
    }

    public UserEmojiPermission setUserId(long userId) {
        set(USERS_EMOJIS_PERMS.USER_ID, userId);
        return this;
    }

    public UserEmojiPermission setEmoji(String emoji) {
        set(USERS_EMOJIS_PERMS.EMOJI, emoji);
        return this;
    }

    public UserEmojiPermission setTimeCreated(long timeCreated) {
        set(USERS_EMOJIS_PERMS.CREATED_AT, timeCreated);
        return this;
    }
}