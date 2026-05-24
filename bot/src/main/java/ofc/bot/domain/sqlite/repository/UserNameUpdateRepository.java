package ofc.bot.domain.sqlite.repository;

import ofc.bot.domain.abstractions.InitializableTable;
import ofc.bot.domain.entity.enums.NameScope;
import ofc.bot.domain.tables.UserNamesUpdatesTable;
import ofc.bot.domain.viewmodels.NamesHistoryView;
import ofc.bot.domain.entity.UserNameUpdate;
import org.jetbrains.annotations.NotNull;
import org.jooq.DSLContext;

import java.util.List;

/**
 * Repository for {@link UserNameUpdate} entity.
 */
public class UserNameUpdateRepository extends Repository<UserNameUpdate> {
    private static final UserNamesUpdatesTable NAME_UPDATES = UserNamesUpdatesTable.USERNAMES_UPDATES;

    public UserNameUpdateRepository(DSLContext ctx) {
        super(ctx);
    }

    @NotNull
    @Override
    public InitializableTable<UserNameUpdate> getTable() {
        return NAME_UPDATES;
    }

    public NamesHistoryView viewByUserId(NameScope scope, long userId, int offset, int limit) {
        List<UserNameUpdate> names = findByUserId(scope, userId, offset, limit);
        int total = countByUserId(userId);
        int page = (int) (Math.floor(offset / 10.0) + 1);
        return new NamesHistoryView(names, total, offset, page);
    }

    public List<UserNameUpdate> findByUserId(NameScope scope, long userId, int offset, int limit) {
        return ctx.selectFrom(NAME_UPDATES)
                .where(NAME_UPDATES.USER_ID.eq(userId))
                .and(NAME_UPDATES.SCOPE.eq(scope.toString()))
                .groupBy(NAME_UPDATES.ID)
                .orderBy(NAME_UPDATES.CREATED_AT.desc())
                .limit(limit)
                .offset(offset)
                .fetch();
    }

    public int countByUserId(long userId) {
        return ctx.fetchCount(NAME_UPDATES, NAME_UPDATES.USER_ID.eq(userId));
    }
}