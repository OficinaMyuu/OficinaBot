package ofc.bot.domain.entity;

import ofc.bot.domain.tables.BlockedWordsTable;

public class BlockedWord extends OficinaRecord<BlockedWord> {
    private static final BlockedWordsTable BLOCKED_WORDS = BlockedWordsTable.BLOCKED_WORDS;

    public BlockedWord() {
        super(BLOCKED_WORDS);
    }

    public int getId() {
        return get(BLOCKED_WORDS.ID);
    }

    public long getGuildId() {
        return get(BLOCKED_WORDS.GUILD_ID);
    }

    public String getWord() {
        return get(BLOCKED_WORDS.WORD);
    }

    public boolean isSevere() {
        return get(BLOCKED_WORDS.SEVERE);
    }

    public boolean isMatchExact() {
        return get(BLOCKED_WORDS.MATCH_EXACT);
    }

    public long getTimeCreated() {
        return get(BLOCKED_WORDS.CREATED_AT);
    }

    @Override
    public long getLastUpdated() {
        return get(BLOCKED_WORDS.UPDATED_AT);
    }
}