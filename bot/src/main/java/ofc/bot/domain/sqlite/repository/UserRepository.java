package ofc.bot.domain.sqlite.repository;

import ofc.bot.domain.abstractions.InitializableTable;
import ofc.bot.domain.entity.AppUser;
import ofc.bot.domain.tables.UsersTable;
import org.jetbrains.annotations.NotNull;
import org.jooq.DSLContext;

import java.util.List;

/**
 * Repository for {@link AppUser} entity.
 */
public class UserRepository extends Repository<AppUser> {
    private final UsersTable USERS = UsersTable.USERS;

    public UserRepository(DSLContext ctx) {
        super(ctx);
    }

    @NotNull
    @Override
    public InitializableTable<AppUser> getTable() {
        return USERS;
    }

    public AppUser findById(long id) {
        return ctx.fetchOne(USERS, USERS.ID.eq(id));
    }

    public List<AppUser> findByNames(String search, int limit) {
        return ctx.selectFrom(USERS)
                .where(USERS.NAME.like('%' + search + '%'))
                .or(USERS.GLOBAL_NAME.like('%' + search + '%'))
                .limit(limit)
                .fetch();
    }
}