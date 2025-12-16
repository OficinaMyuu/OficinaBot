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

    public SupportTicket findById(int id) {
        return ctx.fetchOne(SUPPORT_TICKETS, SUPPORT_TICKETS.ID.eq(id));
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
     * Searches tickets given the provided filters.
     *
     * @param user The user to filter the results.
     * @param offset The offset of the query.
     * @param limit The amount of results this method should limit to.
     * @return A {@link List List&lt;SupportTicket&gt;} containing the found results.
     */
    public List<SupportTicket> searchByUser(@Nullable User user, int offset, int limit) {
        Long userId = user == null ? null : user.getIdLong();
        return searchByUser(userId, offset, limit);
    }

    /**
     * Searches tickets given the provided filters.
     *
     * @param userId The ID of the user.
     * @param offset The offset of the query.
     * @param limit The amount of results this method should limit to.
     * @return A {@link List List&lt;SupportTicket&gt;} containing the found results.
     */
    public List<SupportTicket> searchByUser(@Nullable Long userId, int offset, int limit) {
        Condition userCondition = userId == null ? noCondition() : SUPPORT_TICKETS.INITIATOR_ID.eq(userId);

        return ctx.selectFrom(SUPPORT_TICKETS)
                .where(userCondition)
                .orderBy(SUPPORT_TICKETS.CREATED_AT.desc())
                .offset(offset)
                .limit(limit)
                .fetch();
    }

    public int countByUser(@Nullable User user) {
        return countByUser(user == null ? null : user.getIdLong());
    }

    public int countByUser(@Nullable Long userId) {
        Condition userCond = userId == null ? noCondition() : SUPPORT_TICKETS.INITIATOR_ID.eq(userId);
        return ctx.fetchCount(SUPPORT_TICKETS, userCond);
    }
}