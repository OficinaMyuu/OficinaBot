package ofc.bot.domain.sqlite.repository;

import net.dv8tion.jda.internal.utils.Checks;
import ofc.bot.commands.impl.slash.economy.LeaderboardCommand;
import ofc.bot.domain.abstractions.InitializableTable;
import ofc.bot.domain.entity.UserEconomy;
import ofc.bot.domain.sqlite.MapperFactory;
import ofc.bot.domain.tables.UsersEconomyTable;
import ofc.bot.domain.tables.UsersTable;
import ofc.bot.domain.viewmodels.LeaderboardUser;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Result;

import java.util.Collection;
import java.util.List;

/**
 * Repository for {@link UserEconomy} entity.
 */
public class UserEconomyRepository extends Repository<UserEconomy> {
    private static final UsersEconomyTable USERS_ECONOMY = UsersEconomyTable.USERS_ECONOMY;
    private static final UsersTable USERS = UsersTable.USERS;

    public UserEconomyRepository(DSLContext ctx) {
        super(ctx);
    }

    @NotNull
    @Override
    public InitializableTable<UserEconomy> getTable() {
        return USERS_ECONOMY;
    }

    public boolean hasEnoughWallet(long userId, int amount) {
        return ctx.fetchExists(
                USERS_ECONOMY,
                USERS_ECONOMY.USER_ID.eq(userId).and(USERS_ECONOMY.WALLET.ge(amount))
        );
    }

    public boolean hasEnoughBank(long userId, int amount) {
        return ctx.fetchExists(
                USERS_ECONOMY,
                USERS_ECONOMY.USER_ID.eq(userId).and(USERS_ECONOMY.BANK.ge(amount))
        );
    }

    /**
     * Checks both {@link UserEconomy#getBank() bank} and {@link UserEconomy#getWallet() wallet}.
     * <p>
     * This method sums both values and check against the provided {@code amount}.
     * If you want to check each value independently, call {@link #hasEnoughWallet(long, int)}
     * or {@link #hasEnoughBank(long, int)} instead.
     * <p>
     * If the provided {@code userId} does not exist in the database, {@code false} is returned.
     *
     * @param userId The ID of the user to be checked.
     * @param amount The amount to be checked against the user's total balance.
     * @return {@code true} if the users has, in total, the enough money provided, {@code false} otherwise.
     */
    public boolean hasEnough(long userId, long amount) {
        return ctx.fetchExists(
                USERS_ECONOMY,
                USERS_ECONOMY.USER_ID.eq(userId)
                        .and(USERS_ECONOMY.WALLET.plus(USERS_ECONOMY.BANK).cast(long.class).ge(amount))
        );
    }

    public int fetchWalletByUserId(long userId) {
        return ctx.select(USERS_ECONOMY.WALLET)
                .from(USERS_ECONOMY)
                .where(USERS_ECONOMY.USER_ID.eq(userId))
                .fetchOptionalInto(int.class)
                .orElse(0);
    }

    public void transfer(@NotNull UserEconomy from, @NotNull UserEconomy to, int sendWallet, int takeWallet, int sendBank, int takeBank) {
        Checks.notNull(from, "From User");
        Checks.notNull(to, "To User");
        Checks.notNegative(sendWallet, "Send Wallet");
        Checks.notNegative(takeWallet, "Take Wallet");
        Checks.notNegative(sendBank, "Send Bank");
        Checks.notNegative(takeBank, "Take Bank");

        ctx.transaction((cfg) -> {
            DSLContext trsCtx = cfg.dsl();

            from.modifyBalance(-takeWallet, -takeBank).tickUpdate();
            to.modifyBalance(sendWallet, sendBank).tickUpdate();

            upsert(trsCtx, from);
            upsert(trsCtx, to);
        });
    }

    public void transfer(long fromId, long toId, int sendWallet, int takeWallet, int sendBank, int takeBank) {
        UserEconomy from = findByUserId(fromId);

        if (from == null)
            throw new UnsupportedOperationException("User for ID " + fromId + " does not exist. " +
                    "How are we supposed to take money from a user that has never interacted with our economy???");

        // The receiver user does not have to actually exist, we can pseudo-generate a new one
        UserEconomy to = findByUserId(toId, UserEconomy.fromUserId(toId));
        transfer(from, to, sendWallet, takeWallet, sendBank, takeBank);
    }

    public void transfer(UserEconomy from, UserEconomy to, int wallet, int bank) {
        transfer(from, to, wallet, wallet, bank, bank);
    }

    public void transfer(long fromId, long toId, int wallet, int bank) {
        transfer(fromId, toId, wallet, wallet, bank, bank);
    }

    public void transferWallet(long fromId, long toId, int send, int take) {
        transfer(fromId, toId, send, take, 0, 0);
    }

    public void transferWallet(long fromId, long toId, int amount) {
        transferWallet(fromId, toId, amount, amount);
    }

    public void transferBank(long fromId, long toId, int send, int take) {
        transfer(fromId, toId, 0, 0, send, take);
    }

    public void transferBank(long fromId, long toId, int amount) {
        transferBank(fromId, toId, amount, amount);
    }

    public UserEconomy findByUserId(long userId) {
        return ctx.selectFrom(USERS_ECONOMY)
                .where(USERS_ECONOMY.USER_ID.eq(userId))
                .fetchOne();
    }

    public List<UserEconomy> findByIds(Collection<Long> userIds) {
        return ctx.selectFrom(USERS_ECONOMY)
                .where(USERS_ECONOMY.USER_ID.in(userIds))
                .fetch();
    }

    public UserEconomy findByUserId(long userId, UserEconomy fallback) {
        UserEconomy eco = findByUserId(userId);
        return eco == null ? fallback : eco;
    }

    /**
     * This method returns the last time the user has
     * run the {@code /daily} command successfully.
     * <p>
     * If no users for the given id are present in this table,
     * {@code 0} is returned.
     *
     * @param userId the id of the user to be checked.
     * @return the last time the user has successfully run the {@code /daily} command,
     * {@code 0} otherwise.
     */
    public long fetchLastDailyByUserId(long userId) {
        return ctx.select(USERS_ECONOMY.LAST_DAILY_AT)
                .from(USERS_ECONOMY)
                .where(USERS_ECONOMY.USER_ID.eq(userId))
                .fetchOptionalInto(long.class)
                .orElse(0L);
    }

    /**
     * This method returns the last time the user has
     * run the {@code /work} command successfully.
     * <p>
     * If no users for the given id are present in this table,
     * {@code 0} is returned.
     *
     * @param userId the id of the user to be checked.
     * @return the last time the user has successfully run the {@code /work} command,
     * {@code 0} otherwise.
     */
    public long fetchLastWorkByUserId(long userId) {
        return ctx.select(USERS_ECONOMY.LAST_WORK_AT)
                .from(USERS_ECONOMY)
                .where(USERS_ECONOMY.USER_ID.eq(userId))
                .fetchOptionalInto(long.class)
                .orElse(0L);
    }

    /**
     * Retrieves the rank of a user based on the specified {@link UserEconomy} instance.
     * <p>
     * If {@code null} is provided in the {@code eco} parameter, {@code 0} is returned.
     * <p>
     * <b>Note:</b> While <i>YOU CAN</i> use {@linkplain UserEconomy#isGenerated() generated}
     * records to estimate where a particular amount or user would fit in the leaderboard,
     * be aware that generated records are not fetched from the database,
     * and their ranking might be imprecise.
     *
     * @param eco the {@link UserEconomy} instance representing the user's economy data.
     * @return the rank of the user, or {@code 0} if {@code eco} is {@code null}.
     */
    public int findRankByUser(@Nullable UserEconomy eco) {
        if (eco == null) return 0;

        Field<Long> fieldTotal = USERS_ECONOMY.WALLET.cast(long.class).plus(USERS_ECONOMY.BANK.cast(long.class));
        long userId = eco.getUserId();
        long total = eco.getTotal();
        return ctx.fetchCount(
                USERS_ECONOMY,
                fieldTotal.gt(total)
                        .or(fieldTotal.eq(total).and(USERS_ECONOMY.USER_ID.gt(userId)))
        ) + 1;
    }

    public int findRankByUserId(long userId) {
        return findRankByUser(findByUserId(userId));
    }

    public List<LeaderboardUser> viewLeaderboard(LeaderboardCommand.Scope scope, int offset, int limit) {
        var valueField = switch (scope) {
            case WALLET -> USERS_ECONOMY.WALLET;
            case BANK -> USERS_ECONOMY.BANK;
            case ALL -> USERS_ECONOMY.WALLET.plus(USERS_ECONOMY.BANK);
        };

        Result<?> userEconomyData = ctx.select(USERS_ECONOMY.USER_ID, valueField.cast(long.class).as("balance"), USERS.NAME)
                .from(USERS_ECONOMY)
                .leftJoin(USERS).on(USERS_ECONOMY.USER_ID.eq(USERS.ID))
                .groupBy(USERS_ECONOMY.USER_ID)
                .orderBy(valueField.desc())
                .offset(offset)
                .limit(limit)
                .fetch();

        return userEconomyData.map(MapperFactory::mapLeaderboardUsers);
    }
}