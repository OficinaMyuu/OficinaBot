package ofc.bot.domain.sqlite.repository;

import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import ofc.bot.domain.abstractions.InitializableTable;
import ofc.bot.domain.entity.SupportTicket;
import ofc.bot.domain.tables.SupportTicketsTable;
import org.jetbrains.annotations.NotNull;
import org.jooq.DSLContext;

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
}