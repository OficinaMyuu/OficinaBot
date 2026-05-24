package ofc.bot.domain.sqlite.repository;

import ofc.bot.domain.abstractions.InitializableTable;
import ofc.bot.domain.entity.MemberEmoji;
import ofc.bot.domain.tables.MembersEmojisTable;
import org.jetbrains.annotations.NotNull;
import org.jooq.DSLContext;

/**
 * Repository for {@link MemberEmoji} entity.
 */
public class MemberEmojiRepository extends Repository<MemberEmoji> {
    private static final MembersEmojisTable MEMBERS_EMOJIS = MembersEmojisTable.MEMBERS_EMOJIS;

    public MemberEmojiRepository(DSLContext ctx) {
        super(ctx);
    }

    @NotNull
    @Override
    public InitializableTable<MemberEmoji> getTable() {
        return MEMBERS_EMOJIS;
    }

    public MemberEmoji findByUserId(long userId) {
        return ctx.selectFrom(MEMBERS_EMOJIS)
                .where(MEMBERS_EMOJIS.USER_ID.eq(userId))
                .fetchOne();
    }
}