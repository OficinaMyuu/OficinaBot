package ofc.bot.domain.sqlite.repository;

import ofc.bot.domain.abstractions.InitializableTable;
import ofc.bot.domain.entity.CustomUserinfo;
import ofc.bot.domain.tables.CustomUserinfoTable;
import org.jetbrains.annotations.NotNull;
import org.jooq.DSLContext;

/**
 * Repository for {@link CustomUserinfo} entity.
 */
public class CustomUserinfoRepository extends Repository<CustomUserinfo> {
    private static final CustomUserinfoTable CUSTOM_USERINFO = CustomUserinfoTable.CUSTOM_USERINFO;

    public CustomUserinfoRepository(DSLContext ctx) {
        super(ctx);
    }

    @NotNull
    @Override
    public InitializableTable<CustomUserinfo> getTable() {
        return CUSTOM_USERINFO;
    }

    public void deleteByUserId(long userId) {
        ctx.deleteFrom(CUSTOM_USERINFO)
                .where(CUSTOM_USERINFO.USER_ID.eq(userId))
                .execute();
    }

    public CustomUserinfo findByUserId(long userId) {
        return ctx.selectFrom(CUSTOM_USERINFO)
                .where(CUSTOM_USERINFO.USER_ID.eq(userId))
                .fetchOne();
    }

    public CustomUserinfo findByUserId(long userId, CustomUserinfo fallback) {
        CustomUserinfo csInfo = findByUserId(userId);
        return csInfo == null ? fallback : csInfo;
    }
}