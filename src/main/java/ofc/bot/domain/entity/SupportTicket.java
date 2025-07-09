package ofc.bot.domain.entity;

import ofc.bot.domain.tables.SupportTicketsTable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SupportTicket extends OficinaRecord<SupportTicket> {
    public static final int MAX_SUBJECT_LENGTH = 50;
    public static final int MAX_BODY_LENGTH = 2000;
    private static final SupportTicketsTable SUPPORT_TICKETS = SupportTicketsTable.SUPPORT_TICKETS;

    public SupportTicket() {
        super(SUPPORT_TICKETS);
    }

    public SupportTicket(@NotNull String title, @NotNull String description, long guildId, long channelId,
                         long initiatorId, @Nullable String closeReason, @Nullable Long closeAuthorId,
                         long createdAt, long updatedAt) {
        this();
        set(SUPPORT_TICKETS.TITLE, title);
        set(SUPPORT_TICKETS.DESCRIPTION, description);
        set(SUPPORT_TICKETS.GUILD_ID, guildId);
        set(SUPPORT_TICKETS.CHANNEL_ID, channelId);
        set(SUPPORT_TICKETS.INITIATOR_ID, initiatorId);
        set(SUPPORT_TICKETS.CLOSE_REASON, closeReason);
        set(SUPPORT_TICKETS.CLOSED_BY_ID, closeAuthorId);
        set(SUPPORT_TICKETS.CREATED_AT, createdAt);
        set(SUPPORT_TICKETS.UPDATED_AT, updatedAt);
    }

    public int getId() {
        return get(SUPPORT_TICKETS.ID);
    }

    public String getTitle() {
        return get(SUPPORT_TICKETS.TITLE);
    }

    public String getDescription() {
        return get(SUPPORT_TICKETS.DESCRIPTION);
    }

    public long getGuildId() {
        return get(SUPPORT_TICKETS.GUILD_ID);
    }

    public long getChannelId() {
        return get(SUPPORT_TICKETS.CHANNEL_ID);
    }

    public long getInitiatorId() {
        return get(SUPPORT_TICKETS.INITIATOR_ID);
    }

    /**
     * Gets the reason of why this ticket was closed.
     * <p>
     * This method will return {@code null} if the ticket hasn't been closed yet.
     *
     * @return The reason for which this ticket was closed,
     *         or {@code null} if it is still open.
     */
    public String getCloseReason() {
        return get(SUPPORT_TICKETS.CLOSE_REASON);
    }

    /**
     * Gets the ID of the person who closed this ticket.
     * <p>
     * This method will return {@code 0} if the ticket is not closed yet.
     *
     * @return The ID of the person who closed this ticket, {@code 0} otherwise.
     */
    public long getClosedAuthorId() {
        Long id = get(SUPPORT_TICKETS.CLOSED_BY_ID);
        return id == null ? 0 : id;
    }

    public long getTimeCreated() {
        return get(SUPPORT_TICKETS.CREATED_AT);
    }

    @Override
    public long getLastUpdated() {
        return get(SUPPORT_TICKETS.UPDATED_AT);
    }

    public SupportTicket setTitle(@NotNull String title) {
        set(SUPPORT_TICKETS.TITLE, title);
        return this;
    }

    public SupportTicket setDescription(@NotNull String description) {
        set(SUPPORT_TICKETS.DESCRIPTION, description);
        return this;
    }

    public SupportTicket setGuildId(long guildId) {
        set(SUPPORT_TICKETS.GUILD_ID, guildId);
        return this;
    }

    public SupportTicket setChannelId(long channelId) {
        set(SUPPORT_TICKETS.CHANNEL_ID, channelId);
        return this;
    }

    public SupportTicket setInitiatorId(long initiatorId) {
        set(SUPPORT_TICKETS.INITIATOR_ID, initiatorId);
        return this;
    }

    public SupportTicket setCloseReason(String closeReason) {
        set(SUPPORT_TICKETS.CLOSE_REASON, closeReason);
        return this;
    }

    public SupportTicket setClosedAuthorId(long userId) {
        set(SUPPORT_TICKETS.CLOSED_BY_ID, userId);
        return this;
    }

    @NotNull
    @Override
    public SupportTicket setLastUpdated(long timestamp) {
        set(SUPPORT_TICKETS.UPDATED_AT, timestamp);
        return this;
    }
}