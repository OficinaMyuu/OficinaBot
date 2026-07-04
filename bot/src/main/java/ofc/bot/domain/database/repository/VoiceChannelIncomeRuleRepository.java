package ofc.bot.domain.database.repository;

import ofc.bot.domain.abstractions.InitializableTable;
import ofc.bot.domain.entity.VoiceChannelIncomeRule;
import ofc.bot.domain.entity.enums.VoiceChannelIncomePayoutType;
import ofc.bot.domain.tables.VoiceChannelIncomeRulesTable;
import org.jetbrains.annotations.NotNull;
import org.jooq.DSLContext;

import java.util.List;

/**
 * Repository for {@link VoiceChannelIncomeRule} records.
 */
public class VoiceChannelIncomeRuleRepository extends Repository<VoiceChannelIncomeRule> {
    private static final VoiceChannelIncomeRulesTable VOICE_CHANNEL_INCOME_RULES =
            VoiceChannelIncomeRulesTable.VOICE_CHANNEL_INCOME_RULES;

    /**
     * Creates a repository backed by the provided jOOQ context.
     */
    public VoiceChannelIncomeRuleRepository(@NotNull DSLContext ctx) {
        super(ctx);
    }

    /**
     * Returns the table owned by this repository.
     */
    @NotNull
    @Override
    public InitializableTable<VoiceChannelIncomeRule> getTable() {
        return VOICE_CHANNEL_INCOME_RULES;
    }

    /**
     * Finds all rules for a single payout type.
     */
    public List<VoiceChannelIncomeRule> findByPayoutType(@NotNull VoiceChannelIncomePayoutType payoutType) {
        return ctx.selectFrom(VOICE_CHANNEL_INCOME_RULES)
                .where(VOICE_CHANNEL_INCOME_RULES.PAYOUT_TYPE.eq(payoutType.name()))
                .fetch();
    }

    /**
     * Finds one channel rule for a payout type.
     */
    public VoiceChannelIncomeRule findByChannelAndType(long channelId, @NotNull VoiceChannelIncomePayoutType payoutType) {
        return ctx.selectFrom(VOICE_CHANNEL_INCOME_RULES)
                .where(VOICE_CHANNEL_INCOME_RULES.CHANNEL_ID.eq(channelId))
                .and(VOICE_CHANNEL_INCOME_RULES.PAYOUT_TYPE.eq(payoutType.name()))
                .fetchOne();
    }
}
