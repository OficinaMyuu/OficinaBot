package ofc.bot.domain.entity;

import ofc.bot.domain.tables.MentionsLogTable;

public class MentionLog extends OficinaRecord<MentionLog> {
    private static final MentionsLogTable MENTIONS_LOG = MentionsLogTable.MENTIONS_LOG;

    public MentionLog() {
        super(MENTIONS_LOG);
    }

    public MentionLog(long msgId, long authorId, long mentionedId) {
        this();
        set(MENTIONS_LOG.MSG_ID, msgId);
        set(MENTIONS_LOG.AUTHOR_ID, authorId);
        set(MENTIONS_LOG.MENTIONED_ID, mentionedId);
    }

    public int getId() {
        return get(MENTIONS_LOG.ID);
    }

    public long getMessageId() {
        return get(MENTIONS_LOG.MSG_ID);
    }

    public long getAuthorId() {
        return get(MENTIONS_LOG.AUTHOR_ID);
    }

    public long getMentionedId() {
        return get(MENTIONS_LOG.MENTIONED_ID);
    }

    public long getTimeCreated() {
        return get(MENTIONS_LOG.CREATED_AT);
    }
}