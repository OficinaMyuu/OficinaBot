package ofc.bot.domain.database.repository;

import ofc.bot.domain.abstractions.InitializableTable;
import ofc.bot.domain.entity.GiveawayWinner;
import ofc.bot.domain.entity.enums.GiveawayWinnerStatus;
import ofc.bot.domain.tables.GiveawayWinnersTable;
import ofc.bot.handlers.economy.CurrencyType;
import org.jetbrains.annotations.NotNull;
import org.jooq.DSLContext;

import java.util.Collection;
import java.util.List;

public class GiveawayWinnerRepository extends Repository<GiveawayWinner> {
    private static final GiveawayWinnersTable GIVEAWAY_WINNERS = GiveawayWinnersTable.GIVEAWAY_WINNERS;

    public GiveawayWinnerRepository(@NotNull DSLContext ctx) {
        super(ctx);
    }

    @NotNull
    @Override
    public InitializableTable<GiveawayWinner> getTable() {
        return GIVEAWAY_WINNERS;
    }

    public GiveawayWinner findActiveWinner(@NotNull String giveawayId, long userId) {
        return ctx.selectFrom(GIVEAWAY_WINNERS)
                .where(GIVEAWAY_WINNERS.GIVEAWAY_ID.eq(giveawayId))
                .and(GIVEAWAY_WINNERS.USER_ID.eq(userId))
                .and(GIVEAWAY_WINNERS.STATUS.ne(GiveawayWinnerStatus.REROLLED.name()))
                .fetchOne();
    }

    public List<GiveawayWinner> findActiveByGiveaway(@NotNull String giveawayId) {
        return ctx.selectFrom(GIVEAWAY_WINNERS)
                .where(GIVEAWAY_WINNERS.GIVEAWAY_ID.eq(giveawayId))
                .and(GIVEAWAY_WINNERS.STATUS.ne(GiveawayWinnerStatus.REROLLED.name()))
                .fetch();
    }

    public List<Long> findAllWinnerIds(@NotNull String giveawayId) {
        return ctx.select(GIVEAWAY_WINNERS.USER_ID)
                .from(GIVEAWAY_WINNERS)
                .where(GIVEAWAY_WINNERS.GIVEAWAY_ID.eq(giveawayId))
                .fetch(GIVEAWAY_WINNERS.USER_ID);
    }

    public void markActiveAsRerolled(@NotNull String giveawayId, long updatedAt) {
        ctx.update(GIVEAWAY_WINNERS)
                .set(GIVEAWAY_WINNERS.STATUS, GiveawayWinnerStatus.REROLLED.name())
                .set(GIVEAWAY_WINNERS.UPDATED_AT, updatedAt)
                .where(GIVEAWAY_WINNERS.GIVEAWAY_ID.eq(giveawayId))
                .and(GIVEAWAY_WINNERS.STATUS.ne(GiveawayWinnerStatus.REROLLED.name()))
                .execute();
    }

    public void saveWinners(
            @NotNull String giveawayId,
            @NotNull Collection<Long> userIds,
            @NotNull GiveawayWinnerStatus status,
            long createdAt
    ) {
        List<GiveawayWinner> winners = userIds.stream()
                .map(userId -> new GiveawayWinner(giveawayId, userId, status, null, null, null, createdAt, createdAt))
                .toList();

        bulkSave(winners);
    }

    public boolean startClaim(@NotNull String giveawayId, long userId, long updatedAt) {
        return ctx.update(GIVEAWAY_WINNERS)
                .set(GIVEAWAY_WINNERS.STATUS, GiveawayWinnerStatus.PROCESSING.name())
                .set(GIVEAWAY_WINNERS.UPDATED_AT, updatedAt)
                .where(GIVEAWAY_WINNERS.GIVEAWAY_ID.eq(giveawayId))
                .and(GIVEAWAY_WINNERS.USER_ID.eq(userId))
                .and(GIVEAWAY_WINNERS.STATUS.eq(GiveawayWinnerStatus.PENDING_CLAIM.name()))
                .execute() == 1;
    }

    public boolean markClaimed(
            @NotNull String giveawayId,
            long userId,
            CurrencyType currency,
            Long colorRoleId,
            long claimedAt
    ) {
        return ctx.update(GIVEAWAY_WINNERS)
                .set(GIVEAWAY_WINNERS.STATUS, GiveawayWinnerStatus.CLAIMED.name())
                .set(GIVEAWAY_WINNERS.CURRENCY, currency == null ? null : currency.name())
                .set(GIVEAWAY_WINNERS.COLOR_ROLE_ID, colorRoleId)
                .set(GIVEAWAY_WINNERS.CLAIMED_AT, claimedAt)
                .set(GIVEAWAY_WINNERS.UPDATED_AT, claimedAt)
                .where(GIVEAWAY_WINNERS.GIVEAWAY_ID.eq(giveawayId))
                .and(GIVEAWAY_WINNERS.USER_ID.eq(userId))
                .and(GIVEAWAY_WINNERS.STATUS.eq(GiveawayWinnerStatus.PROCESSING.name()))
                .execute() == 1;
    }

    public void markFailed(@NotNull String giveawayId, long userId, long updatedAt) {
        ctx.update(GIVEAWAY_WINNERS)
                .set(GIVEAWAY_WINNERS.STATUS, GiveawayWinnerStatus.FAILED.name())
                .set(GIVEAWAY_WINNERS.UPDATED_AT, updatedAt)
                .where(GIVEAWAY_WINNERS.GIVEAWAY_ID.eq(giveawayId))
                .and(GIVEAWAY_WINNERS.USER_ID.eq(userId))
                .and(GIVEAWAY_WINNERS.STATUS.in(
                        GiveawayWinnerStatus.PENDING_CLAIM.name(),
                        GiveawayWinnerStatus.PROCESSING.name()
                ))
                .execute();
    }
}
