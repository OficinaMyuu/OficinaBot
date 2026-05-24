package ofc.bot.domain.sqlite.repository;

import ofc.bot.domain.abstractions.InitializableTable;
import ofc.bot.domain.entity.AppUserBan;
import ofc.bot.domain.tables.AppUsersBanTable;
import ofc.bot.util.Bot;
import org.jetbrains.annotations.NotNull;
import org.jooq.DSLContext;

/**
 * Repository for {@link AppUserBan} entity.
 */
public class AppUserBanRepository extends Repository<AppUserBan> {
    private static final AppUsersBanTable APP_USERS_BAN = AppUsersBanTable.APP_USERS_BAN;

    public AppUserBanRepository(@NotNull DSLContext ctx) {
        super(ctx);
    }

    public boolean isBanned(long userId) {
        return existsAfter(userId, Bot.unixNow());
    }

    public boolean existsAfter(long userId, long timestamp) {
        return ctx.fetchExists(
                APP_USERS_BAN,
                APP_USERS_BAN.USER_ID.eq(userId)
                        .and(APP_USERS_BAN.EXPIRES_AT.gt(timestamp))
        );
    }

    @NotNull
    @Override
    public InitializableTable<AppUserBan> getTable() {
        return APP_USERS_BAN;
    }
}