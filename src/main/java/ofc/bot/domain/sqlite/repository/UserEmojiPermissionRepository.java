package ofc.bot.domain.sqlite.repository;

import ofc.bot.domain.abstractions.InitializableTable;
import ofc.bot.domain.entity.UserEmojiPermission;
import ofc.bot.domain.tables.UsersEmojisPermissionsTable;
import org.jetbrains.annotations.NotNull;
import org.jooq.DSLContext;

/**
 * Repository for {@link UserEmojiPermission} entity.
 */
public class UserEmojiPermissionRepository extends Repository<UserEmojiPermission> {
    private static final UsersEmojisPermissionsTable USERS_EMOJIS_PERMS = UsersEmojisPermissionsTable.USERS_EMOJIS_PERMS;

    public UserEmojiPermissionRepository(DSLContext ctx) {
        super(ctx);
    }

    @NotNull
    @Override
    public InitializableTable<UserEmojiPermission> getTable() {
        return USERS_EMOJIS_PERMS;
    }

    public boolean existsByUserAndEmoji(long userId, String emoji) {
        return ctx.fetchExists(USERS_EMOJIS_PERMS,
                USERS_EMOJIS_PERMS.USER_ID.eq(userId)
                        .and(USERS_EMOJIS_PERMS.EMOJI.eq(emoji))
        );
    }

    public UserEmojiPermission findByUserAndEmoji(long userId, String emoji) {
        return ctx.selectFrom(USERS_EMOJIS_PERMS)
                .where(USERS_EMOJIS_PERMS.USER_ID.eq(userId))
                .and(USERS_EMOJIS_PERMS.EMOJI.eq(emoji))
                .fetchOne();
    }

    public void delete(UserEmojiPermission perm) {
        ctx.deleteFrom(USERS_EMOJIS_PERMS)
                .where(USERS_EMOJIS_PERMS.ID.eq(perm.getId()))
                .execute();
    }
}