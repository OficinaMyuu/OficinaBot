package ofc.bot.domain.entity;

import ofc.bot.domain.tables.MessagesVersionsTable;
import org.jetbrains.annotations.Nullable;

public class MessageVersion extends OficinaRecord<MessageVersion> {
    private static final MessagesVersionsTable MESSAGES_VERSIONS = MessagesVersionsTable.MESSAGE_VERSIONS;

    public MessageVersion() {
        super(MESSAGES_VERSIONS);
    }

    /** @return Auto-incrementing version row ID. */
    public int getId() {
        return get(MESSAGES_VERSIONS.ID);
    }

    /** @return The Discord message ID that this version belongs to. */
    public long getMessageId() {
        return get(MESSAGES_VERSIONS.MESSAGE_ID);
    }

    /** @return The author of this message version. */
    public long getAuthorId() {
        return get(MESSAGES_VERSIONS.AUTHOR_ID);
    }

    /** @return The channel where the message exists. */
    public long getChannelId() {
        return get(MESSAGES_VERSIONS.CHANNEL_ID);
    }

    /**
     * Returns the referenced message ID (for replies), or {@code 0}
     * if this version does not reference another message.
     *
     * @return The reply target, or {@code 0} if none.
     */
    public long getMessageReferenceId() {
        Long ref = get(MESSAGES_VERSIONS.MESSAGE_REF_ID);
        return ref == null ? 0 : ref;
    }

    /**
     * @return {@code true} if this version has a reply target.
     */
    public boolean isReply() {
        return getMessageReferenceId() != 0;
    }

    /**
     * Returns the content of this message version.
     *
     * @return Text content, or {@code null} if the message had no textual content.
     */
    @Nullable
    public String getContent() {
        return get(MESSAGES_VERSIONS.CONTENT);
    }

    /**
     * Returns the sticker associated with this version.
     *
     * @return Sticker ID, or {@code 0} if no sticker was attached.
     */
    public long getStickerId() {
        Long id = get(MESSAGES_VERSIONS.STICKER_ID);
        return id == null ? 0 : id;
    }

    /**
     * Indicates whether this version represents a logical deletion
     * of the underlying message.
     *
     * @return {@code true} if the message is deleted at this version.
     */
    public boolean isDeleted() {
        return get(MESSAGES_VERSIONS.IS_DELETED);
    }

    /**
     * Indicates whether this version is the very first (original) message
     * as it appeared when created.
     *
     * @return {@code true} if this is the original message version.
     */
    public boolean isOriginal() {
        return get(MESSAGES_VERSIONS.IS_ORIGINAL);
    }

    /**
     * Returns the ID of the user or bot that deleted the message for this
     * specific version entry.
     *
     * <p><b>Important:</b> The expression {@code getDeletedById() == 0} is
     * <em>not</em> equivalent to the negative value of {@link #isDeleted()}.
     *
     * <p>A return value of {@code 0} means only that the deletion actor could not
     * be determined (if the message was deleted at all). This commonly occurs when:
     *
     * <ul>
     *   <li>The user deleted their own message (Discord does not provide the
     *       actor for self-deletions).</li>
     *   <li>Discord haven't provided the deletion information in time.</li>
     *   <li>The message is <em>not</em> a deletion version (see {@link #isDeleted()}).</li>
     * </ul>
     *
     * In other words, {@code 0} signals <i>"unknown deleter"</i>, not
     * <i>"message is not deleted"</i>.
     *
     * @return The deleter ID, or {@code 0} if unknown or not applicable.
     */
    public long getDeletedById() {
        Long id = get(MESSAGES_VERSIONS.DELETED_BY_ID);
        return id == null ? 0 : id;
    }

    /**
     * Returns the timestamp at which this version was recorded.
     *
     * @return UNIX epoch milliseconds.
     */
    public long getTimeCreated() {
        return get(MESSAGES_VERSIONS.CREATED_AT);
    }

    // -----------------------------------------------------------
    // Setters
    // -----------------------------------------------------------

    /**
     * Sets the Discord message ID this version belongs to.
     *
     * @param id The message ID.
     * @return This record instance.
     */
    public MessageVersion setMessageId(long id) {
        set(MESSAGES_VERSIONS.MESSAGE_ID, id);
        return this;
    }

    /**
     * Sets the referenced (replied-to) message ID.
     *
     * @param id Target message, or {@code null} to indicate no reference.
     * @return This record instance.
     */
    public MessageVersion setMessageReferenceId(@Nullable Long id) {
        set(MESSAGES_VERSIONS.MESSAGE_REF_ID, id);
        return this;
    }

    /**
     * Sets the textual content for this version.
     *
     * @param content Text content, or {@code null}.
     * @return This record instance.
     */
    public MessageVersion setContent(@Nullable String content) {
        set(MESSAGES_VERSIONS.CONTENT, content);
        return this;
    }

    /**
     * Sets the sticker ID for this version.
     *
     * @param id Sticker ID, or {@code null}.
     * @return This record instance.
     */
    public MessageVersion setStickerId(@Nullable Long id) {
        set(MESSAGES_VERSIONS.STICKER_ID, id);
        return this;
    }

    /**
     * Marks this version as representing a deletion (or not).
     *
     * @param deleted Whether the message is deleted at this version.
     * @return This record instance.
     */
    public MessageVersion setDeleted(boolean deleted) {
        set(MESSAGES_VERSIONS.IS_DELETED, deleted);
        return this;
    }

    /**
     * Marks whether this version is the original version.
     *
     * @param original {@code true} if this is the first version.
     * @return This record instance.
     */
    public MessageVersion setOriginal(boolean original) {
        set(MESSAGES_VERSIONS.IS_ORIGINAL, original);
        return this;
    }

    /**
     * Sets the user/bot who deleted the message for this version.
     *
     * @param id User ID or {@code null}.
     * @return This record instance.
     */
    public MessageVersion setDeletedById(@Nullable Long id) {
        set(MESSAGES_VERSIONS.DELETED_BY_ID, id);
        return this;
    }

    /**
     * Sets the timestamp at which this version was recorded.
     *
     * @param timestamp UNIX epoch milliseconds.
     * @return This record instance.
     */
    public MessageVersion setTimeCreated(long timestamp) {
        set(MESSAGES_VERSIONS.CREATED_AT, timestamp);
        return this;
    }
}