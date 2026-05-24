package ofc.bot.domain.entity;

import ofc.bot.domain.entity.enums.GiveawayWinnerStatus;
import ofc.bot.domain.tables.GiveawayWinnersTable;
import ofc.bot.handlers.economy.CurrencyType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class GiveawayWinner extends OficinaRecord<GiveawayWinner> {
    private static final GiveawayWinnersTable GIVEAWAY_WINNERS = GiveawayWinnersTable.GIVEAWAY_WINNERS;

    public GiveawayWinner() {
        super(GIVEAWAY_WINNERS);
    }

    public GiveawayWinner(
            @NotNull String giveawayId,
            long userId,
            @NotNull GiveawayWinnerStatus status,
            @Nullable CurrencyType currency,
            @Nullable Long colorRoleId,
            @Nullable Long claimedAt,
            long createdAt,
            long updatedAt
    ) {
        this();
        setGiveawayId(giveawayId);
        setUserId(userId);
        setStatus(status);
        setCurrency(currency);
        setColorRoleId(colorRoleId);
        setClaimedAt(claimedAt);
        setTimeCreated(createdAt);
        setLastUpdated(updatedAt);
    }

    public int getId() {
        return get(GIVEAWAY_WINNERS.ID);
    }

    public String getGiveawayId() {
        return get(GIVEAWAY_WINNERS.GIVEAWAY_ID);
    }

    public long getUserId() {
        return get(GIVEAWAY_WINNERS.USER_ID);
    }

    public GiveawayWinnerStatus getStatus() {
        return GiveawayWinnerStatus.valueOf(get(GIVEAWAY_WINNERS.STATUS));
    }

    public CurrencyType getCurrency() {
        String value = get(GIVEAWAY_WINNERS.CURRENCY);
        return value == null ? null : CurrencyType.valueOf(value);
    }

    public Long getColorRoleId() {
        return get(GIVEAWAY_WINNERS.COLOR_ROLE_ID);
    }

    public Long getClaimedAt() {
        return get(GIVEAWAY_WINNERS.CLAIMED_AT);
    }

    public long getTimeCreated() {
        return get(GIVEAWAY_WINNERS.CREATED_AT);
    }

    @Override
    public long getLastUpdated() {
        return get(GIVEAWAY_WINNERS.UPDATED_AT);
    }

    public GiveawayWinner setGiveawayId(@NotNull String giveawayId) {
        set(GIVEAWAY_WINNERS.GIVEAWAY_ID, giveawayId);
        return this;
    }

    public GiveawayWinner setUserId(long userId) {
        set(GIVEAWAY_WINNERS.USER_ID, userId);
        return this;
    }

    public GiveawayWinner setStatus(@NotNull GiveawayWinnerStatus status) {
        set(GIVEAWAY_WINNERS.STATUS, status.name());
        return this;
    }

    public GiveawayWinner setCurrency(@Nullable CurrencyType currency) {
        set(GIVEAWAY_WINNERS.CURRENCY, currency == null ? null : currency.name());
        return this;
    }

    public GiveawayWinner setColorRoleId(@Nullable Long colorRoleId) {
        set(GIVEAWAY_WINNERS.COLOR_ROLE_ID, colorRoleId);
        return this;
    }

    public GiveawayWinner setClaimedAt(@Nullable Long claimedAt) {
        set(GIVEAWAY_WINNERS.CLAIMED_AT, claimedAt);
        return this;
    }

    public GiveawayWinner setTimeCreated(long createdAt) {
        set(GIVEAWAY_WINNERS.CREATED_AT, createdAt);
        return this;
    }

    @NotNull
    @Override
    public GiveawayWinner setLastUpdated(long updatedAt) {
        set(GIVEAWAY_WINNERS.UPDATED_AT, updatedAt);
        return this;
    }
}
