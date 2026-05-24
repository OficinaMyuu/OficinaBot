package ofc.bot.domain.sqlite.repository;

import ofc.bot.domain.abstractions.InitializableTable;
import ofc.bot.domain.entity.LevelRole;
import ofc.bot.domain.tables.LevelsRolesTable;
import org.jetbrains.annotations.NotNull;
import org.jooq.DSLContext;

/**
 * Repository for {@link LevelRole} entity.
 */
public class LevelRoleRepository extends Repository<LevelRole> {
    private static final LevelsRolesTable LEVELS_ROLES = LevelsRolesTable.LEVELS_ROLES;

    public LevelRoleRepository(DSLContext ctx) {
        super(ctx);
    }

    @NotNull
    @Override
    public InitializableTable<LevelRole> getTable() {
        return LEVELS_ROLES;
    }

    public LevelRole findLastByLevel(int level) {
        return ctx.selectFrom(LEVELS_ROLES)
                .where(LEVELS_ROLES.LEVEL.le(level))
                .orderBy(LEVELS_ROLES.LEVEL.desc())
                .limit(1)
                .fetchOne();
    }
}