package ofc.bot.domain.tables;

import ofc.bot.domain.abstractions.InitializableTable;
import ofc.bot.domain.entity.MessageVersion;
import org.jetbrains.annotations.NotNull;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Query;

import static ofc.bot.domain.tables.UsersTable.USERS;

/**
 * Represents the append-only <code>message_versions</code> table used for tracking
 * all historical versions of Discord messages handled by the bot.
 * <p>
 * This table implements an event-sourcing style model: messages are never updated
 * in place. Instead, every message creation, edit, or deletion is recorded as a
 * new row ("version") with its own timestamp. The latest version of a message
 * is always the row with the highest <code>created_at</code> value for a given
 * <code>message_id</code>.
 *
 * <h2>Versioning Model</h2>
 *
 * <ul>
 *   <li><b>Creation</b>: A new message is inserted with <code>is_original = true</code>
 *       and <code>is_deleted = false</code>.</li>
 *
 *   <li><b>Update ("Edit")</b>: Edits do not modify prior rows. Instead, a new row is
 *       inserted with updated content and <code>is_original = false</code>.
 *       The previous versions remain permanently stored.</li>
 *
 *   <li><b>Deletion</b>: Deleting a message also inserts a new version, with
 *       <code>is_deleted = true</code> and <code>deleted_by_id</code> set to the
 *       responsible user or system. Previous versions remain intact.</li>
 * </ul>
 *
 * <p>
 * This design guarantees perfect historical traceability and eliminates any
 * inconsistencies that can arise from maintaining separate "message" and
 * "message_edits" tables.
 *
 * <h2>Columns</h2>
 *
 * <ul>
 *   <li><b>id</b>: Auto-incrementing primary key for the version row.</li>
 *   <li><b>message_id</b>: Discord's message ID.</li>
 *   <li><b>author_id</b>: Author of the message for this version.</li>
 *   <li><b>channel_id</b>: The Discord channel this message belongs to.</li>
 *   <li><b>message_ref_id</b>: Optional message reference (e.g., reply target).</li>
 *   <li><b>content</b>: The message content for this specific version.</li>
 *   <li><b>sticker_id</b>: Optional sticker attached to this message version.</li>
 *   <li><b>is_deleted</b>: Whether the message is considered deleted at this version.</li>
 *   <li><b>is_original</b>: <code>true</code> only for the first version of a message.</li>
 *   <li><b>deleted_by_id</b>: ID of the user/bot responsible for the deletion, if any.</li>
 *   <li><b>created_at</b>: Timestamp representing when this version was recorded.</li>
 * </ul>
 *
 * <h2>Query Examples</h2>
 *
 * <h3>1. Creating a Message</h3>
 *
 * <pre>{@code
 * DSL.using(configuration).insertInto(MESSAGE_VERSIONS)
 *    .set(MESSAGE_VERSIONS.MESSAGE_ID, discordMessageId)
 *    .set(MESSAGE_VERSIONS.AUTHOR_ID, authorId)
 *    .set(MESSAGE_VERSIONS.CHANNEL_ID, channelId)
 *    .set(MESSAGE_VERSIONS.MESSAGE_REF_ID, replyTarget)
 *    .set(MESSAGE_VERSIONS.CONTENT, initialContent)
 *    .set(MESSAGE_VERSIONS.STICKER_ID, stickerId)
 *    .set(MESSAGE_VERSIONS.IS_DELETED, false)
 *    .set(MESSAGE_VERSIONS.IS_ORIGINAL, true)
 *    .set(MESSAGE_VERSIONS.CREATED_AT, Bot.unixNow())
 *    .execute();
 * }</pre>
 *
 * <h3>2. Updating (Editing) a Message</h3>
 * To "update" a message, insert a new version with <code>is_original = false</code>.
 *
 * <pre>{@code
 * DSL.using(configuration).insertInto(MESSAGE_VERSIONS)
 *    .set(MESSAGE_VERSIONS.MESSAGE_ID, discordMessageId)
 *    .set(MESSAGE_VERSIONS.AUTHOR_ID, originalAuthorId)
 *    .set(MESSAGE_VERSIONS.CHANNEL_ID, channelId)
 *    .set(MESSAGE_VERSIONS.MESSAGE_REF_ID, oldReference)
 *    .set(MESSAGE_VERSIONS.CONTENT, editedContent)
 *    .set(MESSAGE_VERSIONS.STICKER_ID, updatedSticker)
 *    .set(MESSAGE_VERSIONS.IS_DELETED, false)
 *    .set(MESSAGE_VERSIONS.IS_ORIGINAL, false)
 *    .set(MESSAGE_VERSIONS.CREATED_AT, Bot.unixNow())
 *    .execute();
 * }</pre>
 *
 * <h3>3. Deleting a Message</h3>
 * Logically deleting a message is also just inserting a new version:
 *
 * <pre>{@code
 * DSL.using(configuration).insertInto(MESSAGE_VERSIONS)
 *    .set(MESSAGE_VERSIONS.MESSAGE_ID, discordMessageId)
 *    .set(MESSAGE_VERSIONS.AUTHOR_ID, originalAuthorId)
 *    .set(MESSAGE_VERSIONS.CHANNEL_ID, channelId)
 *    .set(MESSAGE_VERSIONS.IS_DELETED, true)
 *    .set(MESSAGE_VERSIONS.IS_ORIGINAL, false)
 *    .set(MESSAGE_VERSIONS.DELETED_BY_ID, moderatorId)
 *    .set(MESSAGE_VERSIONS.CREATED_AT, Bot.unixNow())
 *    .execute();
 * }</pre>
 *
 * <h2>Retrieving the Latest Version</h2>
 *
 * <pre>{@code
 * MessageVersion latest = DSL.using(configuration)
 *    .selectFrom(MESSAGE_VERSIONS)
 *    .where(MESSAGE_VERSIONS.MESSAGE_ID.eq(discordMessageId))
 *    .orderBy(MESSAGE_VERSIONS.CREATED_AT.desc())
 *    .limit(1)
 *    .fetchOneInto(MessageVersion.class);
 * }</pre>
 *
 * This yields the current state of the message as seen in Discord.
 *
 */
public class MessagesVersionsTable extends InitializableTable<MessageVersion> {
    public static final MessagesVersionsTable MESSAGE_VERSIONS = new MessagesVersionsTable();

    public final Field<Integer> ID          = newField("id",             INT.identity(true));
    public final Field<Long> MESSAGE_ID     = newField("message_id",     BIGINT.notNull());
    public final Field<Long> AUTHOR_ID      = newField("author_id",      BIGINT.notNull());
    public final Field<Long> CHANNEL_ID     = newField("channel_id",     BIGINT.notNull());
    public final Field<Long> MESSAGE_REF_ID = newField("message_ref_id", BIGINT);
    public final Field<String> CONTENT      = newField("content",        CHAR);
    public final Field<Long> STICKER_ID     = newField("sticker_id",     BIGINT);
    public final Field<Boolean> IS_DELETED  = newField("is_deleted",     BOOL.notNull());
    public final Field<Boolean> IS_ORIGINAL = newField("is_original",    BOOL.notNull());
    public final Field<Long> DELETED_BY_ID  = newField("deleted_by_id",  BIGINT);
    public final Field<Long> CREATED_AT     = newField("created_at",     BIGINT.notNull());

    public MessagesVersionsTable() {
        super("messages_versions");
    }

    @NotNull
    @Override
    public Class<MessageVersion> getRecordType() {
        return MessageVersion.class;
    }

    @Override
    public Query getSchema(@NotNull DSLContext ctx) {
        return ctx.createTableIfNotExists(this)
                .primaryKey(ID)
                .columns(fields())
                .constraints(
                        foreignKey(AUTHOR_ID).references(USERS, USERS.ID),
                        foreignKey(DELETED_BY_ID).references(USERS, USERS.ID)
                );
    }
}
