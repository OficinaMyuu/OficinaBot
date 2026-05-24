package ofc.bot.domain.sqlite;

import ofc.bot.domain.entity.AppUser;
import ofc.bot.domain.tables.*;
import ofc.bot.domain.viewmodels.LeaderboardUser;
import ofc.bot.domain.viewmodels.LevelView;
import ofc.bot.domain.viewmodels.MarriageView;
import org.jooq.Record;

public final class MapperFactory {
    private static final UsersEconomyTable USERS_ECONOMY = UsersEconomyTable.USERS_ECONOMY;
    private static final MarriagesTable MARRIAGES = MarriagesTable.MARRIAGES;
    private static final UsersTable USERS = UsersTable.USERS;
    private static final UsersXPTable USERS_XP = UsersXPTable.USERS_XP;

    private MapperFactory() {}

    public static MarriageView mapMarriage(Record rec) {
        if (rec == null) return null;

        AppUser req = new AppUser(
                rec.get("req_id", long.class),
                rec.get("req_name", String.class),
                rec.get("req_global_name", String.class),
                rec.get("req_created_at", long.class),
                rec.get("req_updated_at", long.class)
        );
        AppUser tar = new AppUser(
                rec.get("tar_id", long.class),
                rec.get("tar_name", String.class),
                rec.get("tar_global_name", String.class),
                rec.get("tar_created_at", long.class),
                rec.get("tar_updated_at", long.class)
        );

        return new MarriageView(
                req,
                tar,
                rec.get(MARRIAGES.ID),
                rec.get(MARRIAGES.MARRIED_AT),
                rec.get(MARRIAGES.CREATED_AT),
                rec.get(MARRIAGES.UPDATED_AT)
        );
    }

    public static LeaderboardUser mapLeaderboardUsers(Record rec) {
        return new LeaderboardUser(
                rec.get(USERS.NAME),
                rec.get(USERS_ECONOMY.USER_ID),
                rec.get("balance", long.class)
        );
    }

    public static LevelView mapLevel(Record rec, int rank) {
        return new LevelView(
                rec.get(USERS.NAME),
                rec.get(USERS_XP.USER_ID),
                rec.get(USERS_XP.LEVEL),
                rank,
                rec.get(USERS_XP.XP)
        );
    }
}