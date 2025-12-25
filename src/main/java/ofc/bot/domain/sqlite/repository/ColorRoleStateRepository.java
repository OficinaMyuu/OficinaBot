package ofc.bot.domain.sqlite.repository;

import ofc.bot.domain.abstractions.InitializableTable;
import ofc.bot.domain.entity.ColorRoleState;
import ofc.bot.domain.tables.ColorRolesStateTable;
import org.jetbrains.annotations.NotNull;
import org.jooq.DSLContext;

import java.util.List;

/**
 * Repository for {@link ColorRoleState} entity.
 */
public class ColorRoleStateRepository extends Repository<ColorRoleState> {
    private static final ColorRolesStateTable COLOR_ROLES_STATE = ColorRolesStateTable.COLOR_ROLES_STATES;

    public ColorRoleStateRepository(DSLContext ctx) {
        super(ctx);
    }

    @NotNull
    @Override
    public InitializableTable<ColorRoleState> getTable() {
        return COLOR_ROLES_STATE;
    }

    public void deleteByUserAndRoleId(long userId, long roleId) {
        ctx.deleteFrom(COLOR_ROLES_STATE)
                .where(COLOR_ROLES_STATE.USER_ID.eq(userId))
                .and(COLOR_ROLES_STATE.ROLE_ID.eq(roleId))
                .execute();
    }

    public List<ColorRoleState> findByUserId(long userId) {
        return ctx.selectFrom(COLOR_ROLES_STATE)
                .where(COLOR_ROLES_STATE.USER_ID.eq(userId))
                .fetch();
    }

    public ColorRoleState findByUserAndRoleId(long userId, long roleId) {
        return ctx.selectFrom(COLOR_ROLES_STATE)
                .where(COLOR_ROLES_STATE.USER_ID.eq(userId))
                .and(COLOR_ROLES_STATE.ROLE_ID.eq(roleId))
                .fetchOne();
    }
}