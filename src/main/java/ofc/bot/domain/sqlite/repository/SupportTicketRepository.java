package ofc.bot.domain.sqlite.repository;

import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import ofc.bot.domain.abstractions.InitializableTable;
import ofc.bot.domain.entity.SupportTicket;
import ofc.bot.domain.tables.SupportTicketsTable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;

import java.util.List;

import static org.jooq.impl.DSL.noCondition;

/**
 * Repository for {@link SupportTicket} entity.
 */
public class SupportTicketRepository extends Repository<SupportTicket> {
    private static final SupportTicketsTable SUPPORT_TICKETS = SupportTicketsTable.SUPPORT_TICKETS;

    public SupportTicketRepository(DSLContext ctx) {
        super(ctx);
    }

    @NotNull
    @Override
    public InitializableTable<SupportTicket> getTable() {
        return SUPPORT_TICKETS;
    }

    public SupportTicket findByChannelId(long channelId) {
        return ctx.selectFrom(SUPPORT_TICKETS)
                .where(SUPPORT_TICKETS.CHANNEL_ID.eq(channelId))
                .fetchOne();
    }

    public int getLastId() {
        return ctx.select(SUPPORT_TICKETS.ID)
                .from(SUPPORT_TICKETS)
                .orderBy(SUPPORT_TICKETS.ID.desc())
                .limit(1)
                .fetchOptionalInto(int.class)
                .orElse(1);
    }

    /**
     * Checks if the given channel is (or was) a ticket channel.
     * <p>
     * This method does not check if the provided channel still exists
     * or if the ticket is still open.
     *
     * @param channel The {@link MessageChannel} to be checked.
     * @return {@code true} if the channel is (or was) a channel created by a ticket.
     */
    public boolean isTicketChannel(@NotNull MessageChannel channel) {
        return isTicketChannel(channel.getIdLong());
    }

    /**
     * Checks if the given channel is (or was) a ticket channel.
     * <p>
     * This method does not check if the provided channel still exists
     * or if the ticket is still open.
     *
     * @param channelId The ID of the channel to be checked.
     * @return {@code true} if the channel is (or was) a channel created by a ticket.
     */
    public boolean isTicketChannel(long channelId) {
        return ctx.fetchExists(SUPPORT_TICKETS, SUPPORT_TICKETS.CHANNEL_ID.eq(channelId));
    }

    /**
     * Searches tickets given the provided filters with the {@code AND}
     * clause.
     *
     * @param user The user to filter the results.
     * @param status The current status of the ticket.
     * @param offset The offset of the query.
     * @param limit The amount of results this method should limit to.
     * @return A {@link List List&lt;SupportTicket&gt;} containing the found results.
     */
    public List<SupportTicket> searchByUserAndStatus(@Nullable User user, @Nullable TicketStatus status,
                                                     int offset, int limit) {
        Long userId = user == null ? null : user.getIdLong();
        return searchByUserAndStatus(userId, status, offset, limit);
    }

    /**
     * Searches tickets given the provided filters with the {@code AND}
     * clause.
     *
     * @param userId The ID of the user.
     * @param status The current status of the ticket.
     * @param offset The offset of the query.
     * @param limit The amount of results this method should limit to.
     * @return A {@link List List&lt;SupportTicket&gt;} containing the found results.
     */
    public List<SupportTicket> searchByUserAndStatus(@Nullable Long userId, @Nullable TicketStatus status,
                                                     int offset, int limit) {
        Condition userCondition = userId == null ? noCondition() : SUPPORT_TICKETS.INITIATOR_ID.eq(userId);
        Condition statusCondition = getSearchStatusCondition(status);

        return ctx.selectFrom(SUPPORT_TICKETS)
                .where(userCondition)
                .and(statusCondition)
                .orderBy(SUPPORT_TICKETS.CREATED_AT.desc())
                .offset(offset)
                .limit(limit)
                .fetch();
    }

    public int countByUserAndStatus(@Nullable User user, @Nullable TicketStatus status) {
        Long userId = user == null ? null : user.getIdLong();
        return countByUserAndStatus(userId, status);
    }

    public int countByUserAndStatus(@Nullable Long userId, @Nullable TicketStatus status) {
        Condition userCondition = userId == null ? noCondition() : SUPPORT_TICKETS.INITIATOR_ID.eq(userId);
        Condition statusCondition = getSearchStatusCondition(status);

        return ctx.fetchCount(SUPPORT_TICKETS, userCondition.and(statusCondition));
    }

    private Condition getSearchStatusCondition(TicketStatus status) {
        if (status == null) return noCondition();

        Field<String> closeReason = SUPPORT_TICKETS.CLOSE_REASON;
        Field<Long> closedById = SUPPORT_TICKETS.CLOSED_BY_ID;

        return status == TicketStatus.OPEN
                ? closeReason.isNull().and(closedById.isNull())
                : closeReason.isNotNull().and(closedById.isNotNull());
    }

    public enum TicketStatus {
        OPEN("Only Open"),
        CLOSED("Only Closed");

        private final String display;

        TicketStatus(String display) {
            this.display = display;
        }

        public String getDisplay() {
            return this.display;
        }
    }
}