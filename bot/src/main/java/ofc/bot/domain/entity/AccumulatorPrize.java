package ofc.bot.domain.entity;

import ofc.bot.domain.entity.enums.AccumulatorPrizeStatus;
import ofc.bot.domain.entity.enums.AccumulatorPrizeType;
import ofc.bot.domain.tables.AccumulatorPrizesTable;
import ofc.bot.handlers.economy.CurrencyType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AccumulatorPrize extends OficinaRecord<AccumulatorPrize> {
    public static final int MAX_AMOUNT = 1_000_000;
    private static final AccumulatorPrizesTable ACCUMULATOR_PRIZES = AccumulatorPrizesTable.ACCUMULATOR_PRIZES;

    public AccumulatorPrize() {
        super(ACCUMULATOR_PRIZES);
    }

    public AccumulatorPrize(
            long guildId,
            long targetId,
            long createdBy,
            AccumulatorPrizeType type,
            @Nullable Integer amount,
            @Nullable Long colorDurationSeconds,
            long createdAt
    ) {
        this();
        set(ACCUMULATOR_PRIZES.GUILD_ID, guildId);
        set(ACCUMULATOR_PRIZES.TARGET_ID, targetId);
        set(ACCUMULATOR_PRIZES.CREATED_BY, createdBy);
        set(ACCUMULATOR_PRIZES.TYPE, type.name());
        set(ACCUMULATOR_PRIZES.STATUS, AccumulatorPrizeStatus.PENDING.name());
        set(ACCUMULATOR_PRIZES.AMOUNT, amount);
        set(ACCUMULATOR_PRIZES.CURRENCY, type == AccumulatorPrizeType.MONEY ? CurrencyType.UNBELIEVABOAT.name() : null);
        set(ACCUMULATOR_PRIZES.COLOR_DURATION_SECONDS, colorDurationSeconds);
        set(ACCUMULATOR_PRIZES.CREATED_AT, createdAt);
        set(ACCUMULATOR_PRIZES.UPDATED_AT, createdAt);
    }

    public int getId() {
        return get(ACCUMULATOR_PRIZES.ID);
    }

    public long getGuildId() {
        return get(ACCUMULATOR_PRIZES.GUILD_ID);
    }

    public long getTargetId() {
        return get(ACCUMULATOR_PRIZES.TARGET_ID);
    }

    public long getCreatedBy() {
        return get(ACCUMULATOR_PRIZES.CREATED_BY);
    }

    public AccumulatorPrizeType getType() {
        return AccumulatorPrizeType.valueOf(get(ACCUMULATOR_PRIZES.TYPE));
    }

    public AccumulatorPrizeStatus getStatus() {
        return AccumulatorPrizeStatus.valueOf(get(ACCUMULATOR_PRIZES.STATUS));
    }

    @Nullable
    public Integer getAmount() {
        return get(ACCUMULATOR_PRIZES.AMOUNT);
    }

    @Nullable
    public CurrencyType getCurrency() {
        String currency = get(ACCUMULATOR_PRIZES.CURRENCY);
        return currency == null ? null : CurrencyType.valueOf(currency);
    }

    @Nullable
    public Long getColorRoleId() {
        return get(ACCUMULATOR_PRIZES.COLOR_ROLE_ID);
    }

    @Nullable
    public Long getColorDurationSeconds() {
        return get(ACCUMULATOR_PRIZES.COLOR_DURATION_SECONDS);
    }

    @Nullable
    public Long getApprovedBy() {
        return get(ACCUMULATOR_PRIZES.APPROVED_BY);
    }

    @Nullable
    public Long getApprovedAt() {
        return get(ACCUMULATOR_PRIZES.APPROVED_AT);
    }

    @Nullable
    public Long getRejectedBy() {
        return get(ACCUMULATOR_PRIZES.REJECTED_BY);
    }

    @Nullable
    public Long getRejectedAt() {
        return get(ACCUMULATOR_PRIZES.REJECTED_AT);
    }

    @Nullable
    public String getLastError() {
        return get(ACCUMULATOR_PRIZES.LAST_ERROR);
    }

    public long getTimeCreated() {
        return get(ACCUMULATOR_PRIZES.CREATED_AT);
    }

    @Override
    public long getLastUpdated() {
        return get(ACCUMULATOR_PRIZES.UPDATED_AT);
    }

    public AccumulatorPrize setStatus(@NotNull AccumulatorPrizeStatus status) {
        set(ACCUMULATOR_PRIZES.STATUS, status.name());
        return this;
    }

    public AccumulatorPrize setCurrency(@Nullable CurrencyType currency) {
        set(ACCUMULATOR_PRIZES.CURRENCY, currency == null ? null : currency.name());
        return this;
    }

    public AccumulatorPrize setColorRoleId(@Nullable Long roleId) {
        set(ACCUMULATOR_PRIZES.COLOR_ROLE_ID, roleId);
        return this;
    }

    public AccumulatorPrize setApproved(long userId, long timestamp) {
        set(ACCUMULATOR_PRIZES.APPROVED_BY, userId);
        set(ACCUMULATOR_PRIZES.APPROVED_AT, timestamp);
        return setStatus(AccumulatorPrizeStatus.PAID);
    }

    public AccumulatorPrize setRejected(long userId, long timestamp) {
        set(ACCUMULATOR_PRIZES.REJECTED_BY, userId);
        set(ACCUMULATOR_PRIZES.REJECTED_AT, timestamp);
        return setStatus(AccumulatorPrizeStatus.REJECTED);
    }

    public AccumulatorPrize setLastError(@Nullable String error) {
        set(ACCUMULATOR_PRIZES.LAST_ERROR, error);
        return this;
    }

    public static boolean isValidAmount(@Nullable Integer amount) {
        return amount != null && amount > 0 && amount <= MAX_AMOUNT;
    }

    @NotNull
    @Override
    public AccumulatorPrize setLastUpdated(long timestamp) {
        set(ACCUMULATOR_PRIZES.UPDATED_AT, timestamp);
        return this;
    }
}
