package ofc.bot.domain.sqlite.repository;

import ofc.bot.domain.abstractions.InitializableTable;
import ofc.bot.domain.entity.UserXP;
import ofc.bot.domain.sqlite.MapperFactory;
import ofc.bot.domain.tables.UsersTable;
import ofc.bot.domain.tables.UsersXPTable;
import ofc.bot.domain.viewmodels.LevelView;
import org.jetbrains.annotations.NotNull;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Result;

import java.util.List;

/**
 * Repository for {@link UserXP} entity.
 */
public class UserXPRepository extends Repository<UserXP> {
    private final UsersXPTable USERS_XP = UsersXPTable.USERS_XP;
    private final UsersTable USERS = UsersTable.USERS;

    public UserXPRepository(DSLContext ctx) {
        super(ctx);
    }

    @NotNull
    @Override
    public InitializableTable<UserXP> getTable() {
        return USERS_XP;
    }

    public int findRankByUserId(long userId) {
        UserXP userXp = findByUserId(userId);
        if (userXp == null) return 0;

        int level = userXp.getLevel();
        int xp = userXp.getXp();
        return ctx.fetchCount(
                USERS_XP,
                USERS_XP.LEVEL.gt(level)
                        .or(USERS_XP.LEVEL.eq(level).and(USERS_XP.XP.gt(xp)))
                        .or(USERS_XP.LEVEL.eq(level).and(USERS_XP.XP.eq(xp)).and(USERS_XP.USER_ID.lt(userId)))
        ) + 1;
    }

    public UserXP findByUserId(long userId, UserXP fallback) {
        UserXP userXp = findByUserId(userId);
        return userXp == null ? fallback : userXp;
    }

    public UserXP findByUserId(long userId) {
        return ctx.selectFrom(USERS_XP)
                .where(USERS_XP.USER_ID.eq(userId))
                .fetchOne();
    }

    public List<UserXP> findAllOrdered() {
        return ctx.selectFrom(USERS_XP)
                .orderBy(USERS_XP.LEVEL.desc(), USERS_XP.XP.desc(), USERS_XP.USER_ID.asc())
                .fetch();
    }

    public int findLevelByUserId(long userId) {
        return ctx.select(USERS_XP.LEVEL)
                .from(USERS_XP)
                .where(USERS_XP.USER_ID.eq(userId))
                .fetchOptionalInto(int.class)
                .orElse(0);
    }

    public LevelView viewLevelByUserId(long userId) {
        Record levelData = ctx.select(USERS_XP.USER_ID, USERS_XP.LEVEL, USERS_XP.XP, USERS.NAME)
                .from(USERS_XP)
                .leftJoin(USERS).on(USERS_XP.USER_ID.eq(USERS.ID))
                .where(USERS_XP.USER_ID.eq(userId))
                .fetchOne();

        if (levelData == null)
            return LevelView.empty(userId);

        int rank = findRankByUserId(userId);
        return MapperFactory.mapLevel(levelData, rank);
    }

    public List<LevelView> viewLevels(int offset, int limit) {
        Result<?> levelsData = ctx.select(USERS_XP.USER_ID, USERS_XP.LEVEL, USERS_XP.XP, USERS.NAME)
                .from(USERS_XP)
                .leftJoin(USERS).on(USERS_XP.USER_ID.eq(USERS.ID))
                .groupBy(USERS_XP.USER_ID)
                .orderBy(USERS_XP.LEVEL.desc(), USERS_XP.XP.desc(), USERS_XP.USER_ID.asc())
                .offset(offset)
                .limit(limit)
                .fetch();
        return levelsData.map(r -> MapperFactory.mapLevel(r, 0));
    }
}