package ofc.bot.domain.entity;

import net.dv8tion.jda.internal.utils.Checks;
import ofc.bot.domain.sqlite.repository.UserXPRepository;
import ofc.bot.domain.tables.UsersXPTable;
import ofc.bot.util.Bot;
import org.jetbrains.annotations.NotNull;

import java.util.function.BiConsumer;

public class UserXP extends OficinaRecord<UserXP> {
    private static final UsersXPTable USERS_XP = UsersXPTable.USERS_XP;
    public static final int MIN_CYCLE = 12;
    public static final int MAX_CYCLE = 25;
    public static final String LEADERBOARD_ROW_FORMAT = "` #%d ` **%s** ` nível %s `";

    private static final int BASE_XP = 100;
    private static final double SCALE = 1.3;
    private static final double OFFSET = 1.2;

    public UserXP() {
        super(USERS_XP);
    }

    public UserXP(int xp, int level, long userId, long createdAt, long updatedAt) {
        this();
        Checks.notNegative(xp, "XP");
        Checks.notNegative(level, "Level");
        set(USERS_XP.XP, xp);
        set(USERS_XP.LEVEL, level);
        set(USERS_XP.USER_ID, userId);
        set(USERS_XP.CREATED_AT, createdAt);
        set(USERS_XP.UPDATED_AT, updatedAt);
    }

    public static UserXP fromUserId(long userId) {
        long now = Bot.unixNow();
        return new UserXP(0, 0, userId, now, now);
    }

    public static int calcNextXp(int level) {
        return (int) Math.ceil(BASE_XP * (Math.pow(level, SCALE) + OFFSET));
    }

    public int getId() {
        return get(USERS_XP.ID);
    }

    public long getUserId() {
        return get(USERS_XP.USER_ID);
    }

    public int getXp() {
        return get(USERS_XP.XP);
    }

    public int fetchRank(UserXPRepository xpRepo) {
        return xpRepo.findRankByUserId(getUserId());
    }

    public int getLevel() {
        return get(USERS_XP.LEVEL);
    }

    public long getTimeCreated() {
        return get(USERS_XP.CREATED_AT);
    }

    public long getLastUpdated() {
        return get(USERS_XP.UPDATED_AT);
    }

    public UserXP setUserId(long userId) {
        set(USERS_XP.USER_ID, userId);
        return this;
    }

    public UserXP setXp(int xp) {
        set(USERS_XP.XP, xp);
        return this;
    }

    public UserXP modifyXp(int xp) {
        return setXp(getXp() + xp);
    }

    public UserXP setLevel(int level) {
        set(USERS_XP.LEVEL, level);
        return this;
    }

    @NotNull
    public UserXP setLastUpdated(long updatedAt) {
        set(USERS_XP.UPDATED_AT, updatedAt);
        return this;
    }

    public static void compute(int xp, int currentLevel, BiConsumer<Integer, Integer> act) {
        Checks.notNegative(currentLevel, "Level");
        if (xp <= 0 || currentLevel == Integer.MAX_VALUE) {
            act.accept(0, currentLevel);
            return;
        }

        int totalXp = xp;
        int level = currentLevel;
        while (totalXp >= calcNextXp(level)) {
            totalXp -= calcNextXp(level);
            level++;
        }
        act.accept(totalXp, level);
    }
}