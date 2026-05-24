package ofc.bot.domain.entity;

import ofc.bot.domain.tables.UsersEconomyTable;
import ofc.bot.util.Bot;
import org.jetbrains.annotations.NotNull;

public class UserEconomy extends OficinaRecord<UserEconomy> {
    private static final UsersEconomyTable USERS_ECONOMY = UsersEconomyTable.USERS_ECONOMY;

    public static final String LEADERBOARD_ROW_FORMAT = "%d. `%s`**・**$%s";
    public static final String SYMBOL = "<a:coin:1160091618746060811>";
    public static final String BANK_SYMBOL = "\uD83C\uDFE6";
    public static final String WALLET_SYMBOL = "\uD83D\uDCB5";
    public static final String RANK_SYMBOL = "⬆️";
    public static final String BANK_ICON = "https://media.discordapp.net/attachments/506838906872922145/506899959816126493/h5D6Ei0.png";
    private boolean isGenerated;

    public UserEconomy() {
        super(USERS_ECONOMY);
        this.isGenerated = false;
    }

    public UserEconomy(long userId, int wallet, int bank, long lastDailyAt, long lastWorkAt, long createdAt, long updatedAt) {
        this();
        set(USERS_ECONOMY.USER_ID, userId);
        set(USERS_ECONOMY.WALLET, wallet);
        set(USERS_ECONOMY.BANK, bank);
        set(USERS_ECONOMY.LAST_DAILY_AT, lastDailyAt);
        set(USERS_ECONOMY.LAST_WORK_AT, lastWorkAt);
        set(USERS_ECONOMY.CREATED_AT, createdAt);
        set(USERS_ECONOMY.UPDATED_AT, updatedAt);
        this.isGenerated = true;
    }

    /**
     * Instantiates a new {@link UserEconomy} instance on-the-fly
     * with default values set.
     * Including {@code created_at} and {@code updated_at}.
     * <p>
     * You must call
     * {@link ofc.bot.domain.sqlite.repository.UserEconomyRepository#upsert(UserEconomy) UserEconomyRepository.save(UserEconomy)}
     * on this instance in order to persist it.
     *
     * @param userId the id of the user to be instantiated.
     * @return a new {@link UserEconomy} instance.
     */
    public static UserEconomy fromUserId(long userId) {
        long now = Bot.unixNow();
        return new UserEconomy(userId, 0, 0, 0, 0, now, now);
    }

    public long getUserId() {
        return get(USERS_ECONOMY.USER_ID);
    }

    public int getBank() {
        return get(USERS_ECONOMY.BANK);
    }

    public int getWallet() {
        return get(USERS_ECONOMY.WALLET);
    }

    public long getTotal() {
        return ((long) getWallet()) + ((long) getBank());
    }

    public long getLastDailyAt() {
        Long val = get(USERS_ECONOMY.LAST_DAILY_AT);
        return val == null ? 0 : val;
    }

    public long getLastWorkAt() {
        Long val = get(USERS_ECONOMY.LAST_WORK_AT);
        return val == null ? 0 : val;
    }

    public long getTimeCreated() {
        return get(USERS_ECONOMY.CREATED_AT);
    }

    public long getLastUpdated() {
        return get(USERS_ECONOMY.UPDATED_AT);
    }

    /**
     * Checks if this is a "generated" record.
     * <p>
     * Generated records are those from {@link #fromUserId(long)} calls
     * and through the {@link #UserEconomy(long, int, int, long, long, long, long)} constructor.
     * <p>
     * If the current record came from a database fetch, then this method will return {@code false}.
     *
     * @return {@code true} if this record was generated, {@code false} if it was fetched.
     */
    public boolean isGenerated() {
        return this.isGenerated;
    }

    public UserEconomy setUserId(long userId) {
        set(USERS_ECONOMY.USER_ID, userId);
        return this;
    }

    public UserEconomy setBank(int value) {
        set(USERS_ECONOMY.BANK, value);
        return this;
    }

    public UserEconomy setWallet(int value) {
        set(USERS_ECONOMY.WALLET, value);
        return this;
    }

    public UserEconomy setLastDailyAt(long lastDailyAt) {
        set(USERS_ECONOMY.LAST_DAILY_AT, lastDailyAt);
        return this;
    }

    public UserEconomy setLastWorkAt(long lastWorkAt) {
        set(USERS_ECONOMY.LAST_WORK_AT, lastWorkAt);
        return this;
    }

    public UserEconomy modifyBalance(int wallet, int bank) {
        setWallet(getWallet() + wallet);
        setBank(getBank() + bank);
        return this;
    }

    @NotNull
    public UserEconomy setLastUpdated(long updatedAt) {
        set(USERS_ECONOMY.UPDATED_AT, updatedAt);
        return this;
    }
}
