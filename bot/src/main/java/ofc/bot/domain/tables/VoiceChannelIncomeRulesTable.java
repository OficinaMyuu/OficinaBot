package ofc.bot.domain.tables;

import ofc.bot.domain.abstractions.InitializableTable;
import ofc.bot.domain.entity.VoiceChannelIncomeRule;
import org.jetbrains.annotations.NotNull;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Query;
import org.jooq.impl.SQLDataType;

/**
 * Defines per-channel voice income rules for money and XP jobs.
 */
public class VoiceChannelIncomeRulesTable extends InitializableTable<VoiceChannelIncomeRule> {
    public static final VoiceChannelIncomeRulesTable VOICE_CHANNEL_INCOME_RULES =
            new VoiceChannelIncomeRulesTable();

    public final Field<Integer> ID           = newField("id",          SQLDataType.INTEGER.identity(true));
    public final Field<Long> GUILD_ID        = newField("guild_id",    SQLDataType.BIGINT.notNull());
    public final Field<Long> CHANNEL_ID      = newField("channel_id",  SQLDataType.BIGINT.notNull());
    public final Field<String> PAYOUT_TYPE   = newField("payout_type", SQLDataType.VARCHAR(255).notNull());
    public final Field<Double> MULTIPLIER    = newField("multiplier",  SQLDataType.DOUBLE.notNull());
    public final Field<Boolean> ALLOW_MUTED  = newField("allow_muted", SQLDataType.BOOLEAN.notNull().defaultValue(false));
    public final Field<Boolean> ALLOW_SOLO   = newField("allow_solo",  SQLDataType.BOOLEAN.notNull().defaultValue(false));
    public final Field<Long> CREATED_BY      = newField("created_by",  SQLDataType.BIGINT.notNull());
    public final Field<Long> CREATED_AT      = newField("created_at",  SQLDataType.BIGINT.notNull());
    public final Field<Long> UPDATED_AT      = newField("updated_at",  SQLDataType.BIGINT.notNull());

    /**
     * Creates the table descriptor.
     */
    public VoiceChannelIncomeRulesTable() {
        super("voice_channel_income_rules");
    }

    /**
     * Builds a test schema for repository integration tests.
     */
    @Override
    public Query getSchema(@NotNull DSLContext ctx) {
        return ctx.createTableIfNotExists(this)
                .primaryKey(ID)
                .columns(fields())
                .unique(CHANNEL_ID, PAYOUT_TYPE)
                .check(MULTIPLIER.gt(0.0D));
    }

    /**
     * Returns the jOOQ record type mapped by this table.
     */
    @NotNull
    @Override
    public Class<VoiceChannelIncomeRule> getRecordType() {
        return VoiceChannelIncomeRule.class;
    }
}
