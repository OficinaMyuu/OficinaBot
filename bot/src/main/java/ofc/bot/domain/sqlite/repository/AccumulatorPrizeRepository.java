package ofc.bot.domain.sqlite.repository;

import ofc.bot.domain.abstractions.InitializableTable;
import ofc.bot.domain.entity.AccumulatorPrize;
import ofc.bot.domain.entity.enums.AccumulatorPrizeStatus;
import ofc.bot.domain.tables.AccumulatorPrizesTable;
import ofc.bot.handlers.economy.CurrencyType;
import org.jetbrains.annotations.NotNull;
import org.jooq.DSLContext;

import java.util.Comparator;
import java.util.List;

public class AccumulatorPrizeRepository extends Repository<AccumulatorPrize> {
    private static final AccumulatorPrizesTable ACCUMULATOR_PRIZES = AccumulatorPrizesTable.ACCUMULATOR_PRIZES;

    public AccumulatorPrizeRepository(DSLContext ctx) {
        super(ctx);
    }

    @NotNull
    @Override
    public InitializableTable<AccumulatorPrize> getTable() {
        return ACCUMULATOR_PRIZES;
    }

    public int countPending(long guildId) {
        return ctx.fetchCount(
                ACCUMULATOR_PRIZES,
                ACCUMULATOR_PRIZES.GUILD_ID.eq(guildId)
                        .and(ACCUMULATOR_PRIZES.STATUS.eq(AccumulatorPrizeStatus.PENDING.name()))
        );
    }

    public List<AccumulatorPrize> findPending(long guildId, int offset, int limit) {
        return findAllPending(guildId).stream()
                .skip(offset)
                .limit(limit)
                .toList();
    }

    public List<AccumulatorPrize> findAllPending(long guildId) {
        return ctx.selectFrom(ACCUMULATOR_PRIZES)
                .where(ACCUMULATOR_PRIZES.GUILD_ID.eq(guildId))
                .and(ACCUMULATOR_PRIZES.STATUS.eq(AccumulatorPrizeStatus.PENDING.name()))
                .fetch()
                .stream()
                .sorted(Comparator
                        .comparingInt((AccumulatorPrize prize) -> prize.getType().getPriority())
                        .thenComparing(Comparator.comparingInt(this::amountForSort).reversed())
                        .thenComparingLong(AccumulatorPrize::getTimeCreated)
                        .thenComparingInt(AccumulatorPrize::getId))
                .toList();
    }

    public AccumulatorPrize findById(long guildId, int id) {
        return ctx.selectFrom(ACCUMULATOR_PRIZES)
                .where(ACCUMULATOR_PRIZES.GUILD_ID.eq(guildId))
                .and(ACCUMULATOR_PRIZES.ID.eq(id))
                .fetchOne();
    }

    public AccumulatorPrize findPendingById(long guildId, int id) {
        return ctx.selectFrom(ACCUMULATOR_PRIZES)
                .where(ACCUMULATOR_PRIZES.GUILD_ID.eq(guildId))
                .and(ACCUMULATOR_PRIZES.ID.eq(id))
                .and(ACCUMULATOR_PRIZES.STATUS.eq(AccumulatorPrizeStatus.PENDING.name()))
                .fetchOne();
    }

    public boolean updateCurrency(long guildId, int id, CurrencyType currency, long updatedAt) {
        int updated = ctx.update(ACCUMULATOR_PRIZES)
                .set(ACCUMULATOR_PRIZES.CURRENCY, currency.name())
                .set(ACCUMULATOR_PRIZES.LAST_ERROR, (String) null)
                .set(ACCUMULATOR_PRIZES.UPDATED_AT, updatedAt)
                .where(ACCUMULATOR_PRIZES.GUILD_ID.eq(guildId))
                .and(ACCUMULATOR_PRIZES.ID.eq(id))
                .and(ACCUMULATOR_PRIZES.STATUS.eq(AccumulatorPrizeStatus.PENDING.name()))
                .execute();

        return updated == 1;
    }

    public boolean updateColorRole(long guildId, int id, long roleId, long updatedAt) {
        int updated = ctx.update(ACCUMULATOR_PRIZES)
                .set(ACCUMULATOR_PRIZES.COLOR_ROLE_ID, roleId)
                .set(ACCUMULATOR_PRIZES.LAST_ERROR, (String) null)
                .set(ACCUMULATOR_PRIZES.UPDATED_AT, updatedAt)
                .where(ACCUMULATOR_PRIZES.GUILD_ID.eq(guildId))
                .and(ACCUMULATOR_PRIZES.ID.eq(id))
                .and(ACCUMULATOR_PRIZES.STATUS.eq(AccumulatorPrizeStatus.PENDING.name()))
                .execute();

        return updated == 1;
    }

    public boolean reject(long guildId, int id, long rejectedBy, long rejectedAt) {
        int updated = ctx.update(ACCUMULATOR_PRIZES)
                .set(ACCUMULATOR_PRIZES.STATUS, AccumulatorPrizeStatus.REJECTED.name())
                .set(ACCUMULATOR_PRIZES.REJECTED_BY, rejectedBy)
                .set(ACCUMULATOR_PRIZES.REJECTED_AT, rejectedAt)
                .set(ACCUMULATOR_PRIZES.LAST_ERROR, (String) null)
                .set(ACCUMULATOR_PRIZES.UPDATED_AT, rejectedAt)
                .where(ACCUMULATOR_PRIZES.GUILD_ID.eq(guildId))
                .and(ACCUMULATOR_PRIZES.ID.eq(id))
                .and(ACCUMULATOR_PRIZES.STATUS.eq(AccumulatorPrizeStatus.PENDING.name()))
                .execute();

        return updated == 1;
    }

    public int markPaid(long guildId, List<Integer> ids, long approvedBy, long approvedAt) {
        if (ids.isEmpty()) {
            return 0;
        }

        return ctx.update(ACCUMULATOR_PRIZES)
                .set(ACCUMULATOR_PRIZES.STATUS, AccumulatorPrizeStatus.PAID.name())
                .set(ACCUMULATOR_PRIZES.APPROVED_BY, approvedBy)
                .set(ACCUMULATOR_PRIZES.APPROVED_AT, approvedAt)
                .set(ACCUMULATOR_PRIZES.LAST_ERROR, (String) null)
                .set(ACCUMULATOR_PRIZES.UPDATED_AT, approvedAt)
                .where(ACCUMULATOR_PRIZES.GUILD_ID.eq(guildId))
                .and(ACCUMULATOR_PRIZES.ID.in(ids))
                .and(ACCUMULATOR_PRIZES.STATUS.eq(AccumulatorPrizeStatus.PENDING.name()))
                .execute();
    }

    public void saveLastError(long guildId, List<Integer> ids, String error, long updatedAt) {
        if (ids.isEmpty()) {
            return;
        }

        ctx.update(ACCUMULATOR_PRIZES)
                .set(ACCUMULATOR_PRIZES.LAST_ERROR, error)
                .set(ACCUMULATOR_PRIZES.UPDATED_AT, updatedAt)
                .where(ACCUMULATOR_PRIZES.GUILD_ID.eq(guildId))
                .and(ACCUMULATOR_PRIZES.ID.in(ids))
                .and(ACCUMULATOR_PRIZES.STATUS.eq(AccumulatorPrizeStatus.PENDING.name()))
                .execute();
    }

    private int amountForSort(AccumulatorPrize prize) {
        Integer amount = prize.getAmount();
        return amount == null ? 0 : amount;
    }
}
