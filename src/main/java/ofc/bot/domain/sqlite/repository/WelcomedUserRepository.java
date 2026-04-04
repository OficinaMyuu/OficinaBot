package ofc.bot.domain.sqlite.repository;

import ofc.bot.domain.abstractions.InitializableTable;
import ofc.bot.domain.entity.WelcomedUser;
import ofc.bot.domain.tables.WelcomedUsersTable;
import org.jetbrains.annotations.NotNull;
import org.jooq.DSLContext;

import java.util.List;

/**
 * Repository for {@link WelcomedUser} entity.
 */
public class WelcomedUserRepository extends Repository<WelcomedUser> {
    private static final WelcomedUsersTable WELCOMED_USERS = WelcomedUsersTable.WELCOMED_USERS;

    public WelcomedUserRepository(@NotNull DSLContext ctx) {
        super(ctx);
    }

    @NotNull
    @Override
    public InitializableTable<WelcomedUser> getTable() {
        return WELCOMED_USERS;
    }

    public List<WelcomedUser> findByTargetId(long targetId) {
        return ctx.selectFrom(WELCOMED_USERS)
                .where(WELCOMED_USERS.TARGET_ID.eq(targetId))
                .fetch();
    }
}